package ai.eigloo.agentic.admin.service;

import ai.eigloo.agentic.common.TopicNames;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsOptions;
import org.apache.kafka.clients.admin.DeleteTopicsOptions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.common.KafkaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Service for managing Kafka topics programmatically.
 * Handles creation and deletion of tenant-specific topics.
 */
@Service
public class KafkaTopicManager {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaTopicManager.class);
    
    private final AdminClient adminClient;
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    // Topic configuration
    private static final int DEFAULT_PARTITIONS = 3;
    private static final short DEFAULT_REPLICATION_FACTOR = 1;
    private static final int TOPIC_OPERATION_TIMEOUT_MS = 30000;
    
    @Autowired
    public KafkaTopicManager(AdminClient adminClient, KafkaTemplate<String, String> kafkaTemplate) {
        this.adminClient = adminClient;
        this.kafkaTemplate = kafkaTemplate;
    }
    
    /**
     * Create all tenant-specific topics for a given tenant ID.
     * 
     * @param tenantId the tenant identifier
     * @throws KafkaException if topic creation fails
     */
    public void createTenantTopics(String tenantId) {
        logger.info("Creating Kafka topics for tenant: {}", tenantId);
        
        List<String> topicNames = getTenantTopicNames(tenantId);
        List<NewTopic> newTopics = new ArrayList<>();
        
        for (String topicName : topicNames) {
            NewTopic newTopic = new NewTopic(topicName, DEFAULT_PARTITIONS, DEFAULT_REPLICATION_FACTOR);
            newTopics.add(newTopic);
        }
        
        try {
            CreateTopicsOptions options = new CreateTopicsOptions()
                .timeoutMs(TOPIC_OPERATION_TIMEOUT_MS);
            
            adminClient.createTopics(newTopics, options).all().get(TOPIC_OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            logger.info("Successfully created {} topics for tenant: {}", topicNames.size(), tenantId);
            
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("Failed to create topics for tenant {}: {}", tenantId, e.getMessage(), e);
            throw new KafkaException("Failed to create topics for tenant: " + tenantId, e);
        }
    }
    
    /**
     * Delete all tenant-specific topics for a given tenant ID.
     * 
     * @param tenantId the tenant identifier
     * @throws KafkaException if topic deletion fails
     */
    public void deleteTenantTopics(String tenantId) {
        logger.info("Deleting Kafka topics for tenant: {}", tenantId);
        
        List<String> topicNames = getTenantTopicNames(tenantId);
        
        try {
            DeleteTopicsOptions options = new DeleteTopicsOptions()
                .timeoutMs(TOPIC_OPERATION_TIMEOUT_MS);
            
            adminClient.deleteTopics(topicNames, options).all().get(TOPIC_OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            logger.info("Successfully deleted {} topics for tenant: {}", topicNames.size(), tenantId);
            
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("Failed to delete topics for tenant {}: {}", tenantId, e.getMessage(), e);
            throw new KafkaException("Failed to delete topics for tenant: " + tenantId, e);
        }
    }
    
    /**
     * Check if a topic exists.
     * 
     * @param topicName the topic name to check
     * @return true if the topic exists, false otherwise
     */
    public boolean topicExists(String topicName) {
        try {
            Collection<TopicListing> topics = adminClient.listTopics().listings().get(TOPIC_OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            return topics.stream().anyMatch(topic -> topic.name().equals(topicName));
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("Failed to check if topic {} exists: {}", topicName, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Get all topic names for a tenant.
     * 
     * @param tenantId the tenant identifier
     * @return list of topic names for the tenant
     */
    public List<String> getTenantTopicNames(String tenantId) {
        return Arrays.asList(
            TopicNames.taskExecutions(tenantId),
            TopicNames.planExecutions(tenantId),
            TopicNames.persistedTaskExecutions(tenantId),
            TopicNames.persistedPlanExecutions(tenantId),
            TopicNames.planInputs(tenantId),
            TopicNames.taskInputs(tenantId)
        );
    }
    
    /**
     * Check if all tenant topics exist.
     * 
     * @param tenantId the tenant identifier
     * @return true if all topics exist, false otherwise
     */
    public boolean allTenantTopicsExist(String tenantId) {
        List<String> topicNames = getTenantTopicNames(tenantId);
        return topicNames.stream().allMatch(this::topicExists);
    }
    
    /**
     * Get list of existing tenant topics.
     * 
     * @param tenantId the tenant identifier
     * @return list of existing topic names for the tenant
     */
    public List<String> getExistingTenantTopics(String tenantId) {
        List<String> topicNames = getTenantTopicNames(tenantId);
        return topicNames.stream()
            .filter(this::topicExists)
            .toList();
    }
} 