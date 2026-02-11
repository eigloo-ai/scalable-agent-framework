package ai.eigloo.agentic.common;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tenant-aware Kafka configuration that supports dynamic topic subscription
 * and tenant-specific configurations.
 * 
 * This configuration provides:
 * - Dynamic topic subscription based on tenant patterns
 * - Tenant-aware consumer group naming
 * - Configurable concurrency per tenant
 * - Tenant-specific error handling and retry policies
 */
@Configuration
public class TenantAwareKafkaConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(TenantAwareKafkaConfig.class);
    
    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;
    
    @Value("${spring.kafka.consumer.auto-offset-reset:earliest}")
    private String autoOffsetReset;
    
    @Value("${kafka.tenant.concurrency:3}")
    private int tenantConcurrency;
    
    @Value("${kafka.tenant.max-poll-records:500}")
    private int maxPollRecords;
    
    @Value("${kafka.tenant.session-timeout-ms:30000}")
    private int sessionTimeoutMs;

    @Value("${kafka.tenant.metadata-max-age-ms:10000}")
    private int metadataMaxAgeMs;
    
    @Autowired
    private KafkaTopicPatterns topicPatterns;
    
    // Track active tenant subscriptions
    private final Set<String> activeTenants = ConcurrentHashMap.newKeySet();
    
    /**
     * Configure tenant-aware Kafka consumer factory.
     * 
     * @return the consumer factory
     */
    @Bean
    public ConsumerFactory<String, byte[]> tenantAwareConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, sessionTimeoutMs);
        // Discover newly created tenant topics quickly during active graph execution.
        props.put(ConsumerConfig.METADATA_MAX_AGE_CONFIG, metadataMaxAgeMs);
        
        return new DefaultKafkaConsumerFactory<>(props);
    }
    
    /**
     * Configure tenant-aware Kafka producer factory.
     * 
     * @return the producer factory
     */
    @Bean
    public ProducerFactory<String, byte[]> tenantAwareProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        
        return new DefaultKafkaProducerFactory<>(props);
    }
    
    /**
     * Configure tenant-aware Kafka template.
     * 
     * @return the Kafka template
     */
    @Bean
    public KafkaTemplate<String, byte[]> tenantAwareKafkaTemplate() {
        return new KafkaTemplate<>(tenantAwareProducerFactory());
    }
    
    /**
     * Configure tenant-aware Kafka listener container factory.
     * 
     * @return the listener container factory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, byte[]> tenantAwareKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, byte[]> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(tenantAwareConsumerFactory());
        factory.setConcurrency(tenantConcurrency);
        factory.getContainerProperties().setPollTimeout(3000);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        
        // Add tenant-aware error handling
        factory.setCommonErrorHandler(new TenantAwareErrorHandler());
        
        return factory;
    }
    
    /**
     * Get or create a tenant-specific consumer group name.
     * 
     * @param baseGroupId the base group ID
     * @param tenantId the tenant ID
     * @return the tenant-specific group ID
     */
    public static String getTenantGroupId(String baseGroupId, String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            return baseGroupId;
        }
        return baseGroupId + "-" + tenantId;
    }
    
    /**
     * Register an active tenant subscription.
     * 
     * @param tenantId the tenant ID
     */
    public void registerTenantSubscription(String tenantId) {
        if (tenantId != null && !tenantId.isEmpty()) {
            activeTenants.add(tenantId);
            logger.info("Registered tenant subscription: {}", tenantId);
        }
    }
    
    /**
     * Unregister a tenant subscription.
     * 
     * @param tenantId the tenant ID
     */
    public void unregisterTenantSubscription(String tenantId) {
        if (tenantId != null) {
            activeTenants.remove(tenantId);
            logger.info("Unregistered tenant subscription: {}", tenantId);
        }
    }
    
    /**
     * Get all active tenant subscriptions.
     * 
     * @return set of active tenant IDs
     */
    public Set<String> getActiveTenants() {
        return Set.copyOf(activeTenants);
    }
    
    /**
     * Check if a tenant is actively subscribed.
     * 
     * @param tenantId the tenant ID
     * @return true if the tenant is active, false otherwise
     */
    public boolean isTenantActive(String tenantId) {
        return tenantId != null && activeTenants.contains(tenantId);
    }
    
    /**
     * Get tenant-specific topic names for a given pattern.
     * 
     * @param pattern the topic pattern (e.g., "task-executions-.*")
     * @param tenantId the tenant ID
     * @return the tenant-specific topic name
     */
    public String getTenantTopic(String pattern, String tenantId) {
        if (pattern == null || pattern.isEmpty() || tenantId == null || tenantId.isEmpty()) {
            return pattern;
        }
        
        // Replace the wildcard with the tenant ID
        return pattern.replace(".*", tenantId);
    }
    
    /**
     * Validate tenant configuration.
     * 
     * @param tenantId the tenant ID to validate
     * @return true if the tenant ID is valid, false otherwise
     */
    public boolean validateTenantId(String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            logger.warn("Tenant ID is null or empty");
            return false;
        }
        
        // Basic validation - tenant ID should be alphanumeric with optional hyphens/underscores
        if (!tenantId.matches("^[a-zA-Z0-9_-]+$")) {
            logger.warn("Invalid tenant ID format: {}", tenantId);
            return false;
        }
        
        return true;
    }
    
    /**
     * Tenant-aware error handler that provides tenant-specific error handling.
     */
    private static class TenantAwareErrorHandler implements org.springframework.kafka.listener.CommonErrorHandler {
        
        private static final Logger errorLogger = LoggerFactory.getLogger(TenantAwareErrorHandler.class);
        
        @Override
        public boolean handleOne(Exception thrownException, 
                               org.apache.kafka.clients.consumer.ConsumerRecord<?, ?> record,
                               org.apache.kafka.clients.consumer.Consumer<?, ?> consumer,
                               org.springframework.kafka.listener.MessageListenerContainer container) {
            
            String topic = record.topic();
            String tenantId = TopicNames.extractTenantId(topic);
            
            errorLogger.error("Error processing message for tenant {} from topic {}: {}", 
                           tenantId, topic, thrownException.getMessage(), thrownException);
            
            // For now, return false to let the default error handling take over
            // In a production system, you might want to implement tenant-specific error handling
            return false;
        }
        
        @Override
        public void handleOtherException(Exception thrownException, 
                                      org.apache.kafka.clients.consumer.Consumer<?, ?> consumer,
                                      org.springframework.kafka.listener.MessageListenerContainer container,
                                      boolean batchListener) {
            
            errorLogger.error("Unexpected error in Kafka consumer: {}", 
                           thrownException.getMessage(), thrownException);
        }
    }
} 
