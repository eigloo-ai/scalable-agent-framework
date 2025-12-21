# Tenant-Aware Kafka Architecture - Practical Example

This document provides a practical example of how to use the tenant-aware Kafka architecture in a real-world scenario.

## Scenario: Multi-Tenant Task Processing System

Imagine you have a system that processes tasks for multiple tenants (companies). Each tenant has their own isolated data and processing requirements.

## Step 1: Define Topic Patterns

First, configure the topic patterns in your `application.yml`:

```yaml
kafka:
  topic-patterns:
    task-executions: "task-executions-.*"
    task-results: "task-results-.*"
    persisted-task-executions: "persisted-task-executions-.*"
```

## Step 2: Create Tenant-Specific Topics

Create Kafka topics for each tenant:

```bash
# Create topics for Tenant A
kafka-topics.sh --create --topic task-executions-tenant-a --partitions 3 --replication-factor 2
kafka-topics.sh --create --topic task-results-tenant-a --partitions 3 --replication-factor 2
kafka-topics.sh --create --topic persisted-task-executions-tenant-a --partitions 3 --replication-factor 2

# Create topics for Tenant B
kafka-topics.sh --create --topic task-executions-tenant-b --partitions 3 --replication-factor 2
kafka-topics.sh --create --topic task-results-tenant-b --partitions 3 --replication-factor 2
kafka-topics.sh --create --topic persisted-task-executions-tenant-b --partitions 3 --replication-factor 2
```

## Step 3: Implement Tenant-Aware Kafka Listener

Create a service that listens to task execution messages for all tenants:

```java
@Component
public class TaskExecutionProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskExecutionProcessor.class);
    
    private final ObjectMapper objectMapper;
    private final TaskService taskService;
    
    @Autowired
    public TaskExecutionProcessor(ObjectMapper objectMapper, TaskService taskService) {
        this.objectMapper = objectMapper;
        this.taskService = taskService;
    }
    
    /**
     * Process task execution messages from all tenants.
     * This listener automatically subscribes to all task-executions-* topics.
     */
    @KafkaListener(
        topics = "#{@kafkaTopicPatterns.taskExecutionsPattern}",
        groupId = "task-processor-group",
        containerFactory = "tenantAwareKafkaListenerContainerFactory"
    )
    public void handleTaskExecution(
            String message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {
        
        try {
            logger.debug("Received task execution message from topic: {}", topic);
            
            // Parse the message
            Map<String, Object> taskExecution = objectMapper.readValue(message, Map.class);
            
            // Extract tenant ID from topic name
            String tenantId = TopicNames.extractTenantId(topic);
            if (tenantId == null) {
                logger.error("Could not extract tenant ID from topic: {}", topic);
                acknowledgment.acknowledge();
                return;
            }
            
            // Validate tenant ID
            if (!TenantAwareKafkaConfig.validateTenantId(tenantId)) {
                logger.error("Invalid tenant ID: {}", tenantId);
                acknowledgment.acknowledge();
                return;
            }
            
            // Process the task with tenant context
            processTask(taskExecution, tenantId);
            
            logger.info("Processed task execution for tenant: {}", tenantId);
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            logger.error("Error processing task execution from topic {}: {}", 
                       topic, e.getMessage(), e);
            // Don't acknowledge on error to allow retry
        }
    }
    
    private void processTask(Map<String, Object> taskExecution, String tenantId) {
        // Add tenant context to the task
        taskExecution.put("tenantId", tenantId);
        
        // Process the task with tenant-specific logic
        taskService.processTask(taskExecution, tenantId);
    }
}
```

## Step 4: Send Messages to Tenant-Specific Topics

Create a producer service that sends messages to the appropriate tenant topics:

```java
@Service
public class TaskExecutionProducer {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskExecutionProducer.class);
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Autowired
    public TaskExecutionProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    /**
     * Send a task execution message to a specific tenant's topic.
     */
    public void sendTaskExecution(String tenantId, Map<String, Object> taskExecution) {
        // Validate tenant ID
        if (!TenantAwareKafkaConfig.validateTenantId(tenantId)) {
            throw new IllegalArgumentException("Invalid tenant ID: " + tenantId);
        }
        
        // Create tenant-specific topic name
        String topicName = TopicNames.taskExecutions(tenantId);
        
        // Add tenant context to the message
        taskExecution.put("tenantId", tenantId);
        taskExecution.put("timestamp", System.currentTimeMillis());
        
        try {
            // Send message to tenant-specific topic
            kafkaTemplate.send(topicName, taskExecution);
            
            logger.info("Sent task execution message to topic: {} for tenant: {}", 
                      topicName, tenantId);
            
        } catch (Exception e) {
            logger.error("Failed to send task execution message to topic {}: {}", 
                       topicName, e.getMessage(), e);
            throw new RuntimeException("Failed to send task execution message", e);
        }
    }
    
    /**
     * Send task execution messages to multiple tenants.
     */
    public void sendTaskExecutionToMultipleTenants(
            List<String> tenantIds, 
            Map<String, Object> taskExecution) {
        
        for (String tenantId : tenantIds) {
            sendTaskExecution(tenantId, taskExecution);
        }
    }
}
```

## Step 5: Monitor Tenant Activity

Create a monitoring service to track tenant activity:

```java
@Service
public class TenantMonitoringService {
    
    private static final Logger logger = LoggerFactory.getLogger(TenantMonitoringService.class);
    
    @Autowired
    private TenantAwareKafkaConfig tenantConfig;
    
    /**
     * Get all active tenants.
     */
    public Set<String> getActiveTenants() {
        return tenantConfig.getActiveTenants();
    }
    
    /**
     * Check if a tenant is active.
     */
    public boolean isTenantActive(String tenantId) {
        return tenantConfig.isTenantActive(tenantId);
    }
    
    /**
     * Register a tenant subscription.
     */
    public void registerTenant(String tenantId) {
        tenantConfig.registerTenantSubscription(tenantId);
        logger.info("Registered tenant subscription: {}", tenantId);
    }
    
    /**
     * Unregister a tenant subscription.
     */
    public void unregisterTenant(String tenantId) {
        tenantConfig.unregisterTenantSubscription(tenantId);
        logger.info("Unregistered tenant subscription: {}", tenantId);
    }
    
    /**
     * Get tenant-specific metrics.
     */
    public Map<String, Object> getTenantMetrics(String tenantId) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("active", isTenantActive(tenantId));
        metrics.put("registeredAt", System.currentTimeMillis());
        
        return metrics;
    }
}
```

## Step 6: Configuration for Different Environments

### Development Environment

```yaml
kafka:
  topic-patterns:
    task-executions: "task-executions-.*"
    task-results: "task-results-.*"
  
  tenant:
    concurrency: 2
    max-poll-records: 100
    session-timeout-ms: 30000

logging:
  level:
    ai.eigloo.agentic: DEBUG
```

### Production Environment

```yaml
kafka:
  topic-patterns:
    task-executions: "task-executions-.*"
    task-results: "task-results-.*"
  
  tenant:
    concurrency: 5
    max-poll-records: 500
    session-timeout-ms: 60000

logging:
  level:
    ai.eigloo.agentic: INFO
```

## Step 7: Testing the Implementation

### Unit Test Example

```java
@SpringBootTest
class TaskExecutionProcessorTest {
    
    @Autowired
    private TaskExecutionProducer producer;
    
    @Autowired
    private TaskExecutionProcessor processor;
    
    @Test
    void testTenantSpecificProcessing() {
        // Create test data
        Map<String, Object> taskExecution = Map.of(
            "taskId", "task-123",
            "type", "data-processing",
            "priority", "high"
        );
        
        // Send message to tenant A
        producer.sendTaskExecution("tenant-a", taskExecution);
        
        // Verify processing (this would depend on your test setup)
        // The processor should receive the message and process it with tenant context
    }
    
    @Test
    void testInvalidTenantId() {
        Map<String, Object> taskExecution = Map.of("taskId", "task-123");
        
        // This should throw an exception
        assertThrows(IllegalArgumentException.class, () -> {
            producer.sendTaskExecution("invalid-tenant!", taskExecution);
        });
    }
}
```

### Integration Test Example

```java
@SpringBootTest
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=localhost:9092"
})
class TenantAwareKafkaIntegrationTest {
    
    @Autowired
    private TaskExecutionProducer producer;
    
    @Autowired
    private TenantMonitoringService monitoringService;
    
    @Test
    void testMultiTenantMessageProcessing() throws InterruptedException {
        // Send messages to multiple tenants
        Map<String, Object> task1 = Map.of("taskId", "task-1", "type", "processing");
        Map<String, Object> task2 = Map.of("taskId", "task-2", "type", "analysis");
        
        producer.sendTaskExecution("tenant-a", task1);
        producer.sendTaskExecution("tenant-b", task2);
        
        // Wait for processing
        Thread.sleep(2000);
        
        // Verify tenant activity
        Set<String> activeTenants = monitoringService.getActiveTenants();
        assertTrue(activeTenants.contains("tenant-a"));
        assertTrue(activeTenants.contains("tenant-b"));
    }
}
```

## Step 8: Deployment and Monitoring

### Docker Compose Example

```yaml
version: '3.8'
services:
  kafka:
    image: confluentinc/cp-kafka:latest
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    ports:
      - "9092:9092"
    depends_on:
      - zookeeper
  
  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
  
  task-processor:
    build: .
    environment:
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      KAFKA_TOPIC_PATTERNS_TASK_EXECUTIONS: "task-executions-.*"
    depends_on:
      - kafka
```

### Monitoring Dashboard

Create a simple monitoring dashboard to track tenant activity:

```java
@RestController
@RequestMapping("/api/tenants")
public class TenantMonitoringController {
    
    @Autowired
    private TenantMonitoringService monitoringService;
    
    @GetMapping("/active")
    public Set<String> getActiveTenants() {
        return monitoringService.getActiveTenants();
    }
    
    @GetMapping("/{tenantId}/metrics")
    public Map<String, Object> getTenantMetrics(@PathVariable String tenantId) {
        return monitoringService.getTenantMetrics(tenantId);
    }
    
    @PostMapping("/{tenantId}/register")
    public void registerTenant(@PathVariable String tenantId) {
        monitoringService.registerTenant(tenantId);
    }
    
    @DeleteMapping("/{tenantId}/unregister")
    public void unregisterTenant(@PathVariable String tenantId) {
        monitoringService.unregisterTenant(tenantId);
    }
}
```

## Benefits Demonstrated

1. **Scalability**: Easy to add new tenants without code changes
2. **Isolation**: Each tenant's messages are processed separately
3. **Monitoring**: Track tenant activity and performance
4. **Flexibility**: Support for different tenant configurations
5. **Reliability**: Robust error handling per tenant

This example shows how the tenant-aware Kafka architecture provides a scalable, maintainable solution for multi-tenant message processing. 