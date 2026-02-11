# Tenant-Aware Kafka Architecture

This document describes the implementation of a tenant-aware Kafka architecture that supports dynamic topic subscription and efficient multi-tenant message processing.

## Overview

The tenant-aware Kafka architecture provides:

- **Dynamic Topic Subscription**: Services subscribe to topic patterns like `task-executions-*` instead of static topic names
- **Tenant ID Extraction**: Robust tenant ID extraction from topic names, message headers, and payload
- **Tenant-Aware Configuration**: Configurable concurrency, error handling, and retry policies per tenant
- **Efficient Multi-Tenancy**: Support for multiple tenant-specific topics with optimized resource usage

## Architecture Components

### 1. Topic Naming Convention

All topics follow the pattern: `{message-type}-{tenant-id}`

**Supported Topic Types:**
- `task-executions-{tenantId}` - Task execution messages
- `plan-executions-{tenantId}` - Plan execution messages  
- `persisted-task-executions-{tenantId}` - Persisted task execution messages
- `persisted-plan-executions-{tenantId}` - Persisted plan execution messages
- `plan-inputs-{tenantId}` - Plan input messages
- `task-inputs-{tenantId}` - Task input messages
- `task-executions-dlq-{tenantId}` - Task execution dead letter queue
- `plan-executions-dlq-{tenantId}` - Plan execution dead letter queue

### 2. Topic Pattern Configuration

Topic patterns are configured in `application.yml` files:

```yaml
kafka:
  topic-patterns:
    task-executions: "task-executions-.*"
    plan-executions: "plan-executions-.*"
    persisted-task-executions: "persisted-task-executions-.*"
    persisted-plan-executions: "persisted-plan-executions-.*"
    plan-inputs: "plan-inputs-.*"
    task-inputs: "task-inputs-.*"
    task-executions-dlq: "task-executions-dlq-.*"
    plan-executions-dlq: "plan-executions-dlq-.*"
```

### 3. KafkaTopicPatterns Configuration Class

The `KafkaTopicPatterns` class reads topic patterns from configuration and provides them as Spring beans:

```java
@Configuration
@ConfigurationProperties(prefix = "kafka.topic-patterns")
public class KafkaTopicPatterns {
    private String taskExecutions = "task-executions-.*";
    private String planExecutions = "plan-executions-.*";
    private String planInputs = "plan-inputs-.*";
    // ... other patterns
    
    public String getTaskExecutionsPattern() {
        return taskExecutions;
    }
    
    public String getPlanInputsPattern() {
        return planInputs;
    }
    // ... other getters
}
```

### 4. Tenant ID Extraction

The `TopicNames` utility class provides multiple methods for tenant ID extraction:

#### From Topic Name
```java
String tenantId = TopicNames.extractTenantId("task-executions-tenant123");
// Returns: "tenant123"
```

#### From Message Headers
```java
Map<String, Object> headers = Map.of("tenant_id", "tenant123");
String tenantId = TopicNames.extractTenantIdFromHeaders(headers);
// Returns: "tenant123"
```

#### From Message Payload
```java
Map<String, Object> payload = Map.of("tenantId", "tenant123");
String tenantId = TopicNames.extractTenantIdFromPayload(payload);
// Returns: "tenant123"
```

#### Multi-Source Extraction with Fallback
```java
String tenantId = TopicNames.extractTenantIdFromMultipleSources(
    topic, headers, payload
);
// Priority: 1. Headers, 2. Payload, 3. Topic name
```

### 5. Tenant-Aware Kafka Configuration

The `TenantAwareKafkaConfig` class provides tenant-aware Kafka configuration:

#### Features:
- **Dynamic Topic Subscription**: Support for topic patterns with wildcards
- **Tenant-Specific Consumer Groups**: Automatic tenant-aware group naming
- **Configurable Concurrency**: Per-tenant concurrency settings
- **Tenant-Aware Error Handling**: Custom error handlers with tenant context
- **Active Tenant Tracking**: Registry of active tenant subscriptions

#### Configuration Properties:
```yaml
kafka:
  tenant:
    concurrency: 3
    max-poll-records: 500
    session-timeout-ms: 30000
```

#### Usage in Listeners:
```java
@KafkaListener(
    topics = "#{@kafkaTopicPatterns.persistedTaskExecutionsPattern}",
    groupId = "control-plane-persisted-task-executions",
    containerFactory = "tenantAwareKafkaListenerContainerFactory"
)
public void handleTaskExecution(String message, 
                              @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                              Acknowledgment acknowledgment) {
    // Extract tenant ID from topic
    String tenantId = TopicNames.extractTenantId(topic);
    
    // Process message with tenant context
    processMessage(message, tenantId);
}
```

## Implementation Details

### 1. Service Updates

All microservices have been updated to use the tenant-aware configuration:

#### Control Plane
- Updated `ControlPlaneListener` to use `tenantAwareKafkaListenerContainerFactory`
- Enhanced tenant ID extraction logic
- Simplified Kafka configuration by importing `TenantAwareKafkaConfig`

#### Executor Java
- Updated `TaskInputListener` and `PlanInputListener`
- Uses tenant-aware container factory
- Simplified configuration

#### Data Plane
- Updated `TaskExecutionListener` and `PlanExecutionListener`
- Uses tenant-aware container factory
- Enhanced error handling with tenant context

### 2. Error Handling

The tenant-aware configuration includes custom error handling:

```java
private static class TenantAwareErrorHandler implements CommonErrorHandler {
    @Override
    public boolean handleOne(Exception thrownException, 
                           ConsumerRecord<?, ?> record,
                           Consumer<?, ?> consumer,
                           MessageListenerContainer container) {
        
        String topic = record.topic();
        String tenantId = TopicNames.extractTenantId(topic);
        
        errorLogger.error("Error processing message for tenant {} from topic {}: {}", 
                       tenantId, topic, thrownException.getMessage(), thrownException);
        
        return false; // Let default error handling take over
    }
}
```

### 3. Tenant Validation

The system includes tenant ID validation:

```java
public boolean validateTenantId(String tenantId) {
    if (tenantId == null || tenantId.isEmpty()) {
        return false;
    }
    
    // Basic validation - alphanumeric with optional hyphens/underscores
    return tenantId.matches("^[a-zA-Z0-9_-]+$");
}
```

## Benefits

### 1. Scalability
- **Dynamic Topic Discovery**: Services automatically discover new tenant topics
- **Efficient Resource Usage**: Shared consumer groups with tenant-specific processing
- **Horizontal Scaling**: Easy to scale per tenant or globally

### 2. Maintainability
- **Centralized Configuration**: Topic patterns defined in one place
- **Consistent Naming**: Standardized topic naming across all services
- **Clear Separation**: Tenant-specific logic isolated from business logic

### 3. Reliability
- **Robust Error Handling**: Tenant-aware error handling and retry policies
- **Dead Letter Queues**: Tenant-specific DLQ topics for failed messages
- **Monitoring**: Tenant-specific metrics and monitoring

### 4. Flexibility
- **Multiple Tenant ID Sources**: Extract from topic, headers, or payload
- **Configurable Patterns**: Easy to add new topic types
- **Tenant-Specific Settings**: Per-tenant configuration options

## Usage Examples

### 1. Adding a New Tenant

1. **Create Tenant-Specific Topics**:
   ```bash
   kafka-topics.sh --create --topic task-executions-tenant123 --partitions 3 --replication-factor 2
   kafka-topics.sh --create --topic plan-executions-tenant123 --partitions 3 --replication-factor 2
   kafka-topics.sh --create --topic plan-inputs-tenant123 --partitions 3 --replication-factor 2
   ```

2. **Services Automatically Subscribe**: No code changes needed - services automatically discover new topics

3. **Send Messages**:
   ```java
   kafkaTemplate.send("task-executions-tenant123", message);
   ```

### 2. Monitoring Tenant Activity

```java
@Autowired
private TenantAwareKafkaConfig tenantConfig;

public void monitorTenants() {
    Set<String> activeTenants = tenantConfig.getActiveTenants();
    for (String tenantId : activeTenants) {
        logger.info("Active tenant: {}", tenantId);
    }
}
```

### 3. Tenant-Specific Configuration

```yaml
kafka:
  tenant:
    concurrency: 5  # Higher concurrency for busy tenants
    max-poll-records: 1000
    session-timeout-ms: 60000
```

## Migration Guide

### From Static Topics to Dynamic Patterns

1. **Update Topic Names**: Ensure all topics follow the `{type}-{tenantId}` pattern
2. **Update Listeners**: Change from static topics to pattern-based topics
3. **Update Configuration**: Add topic patterns to `application.yml`
4. **Test Tenant Isolation**: Verify messages are properly routed per tenant

### Example Migration

**Before**:
```java
@KafkaListener(topics = "task-executions", groupId = "executor-group")
```

**After**:
```java
@KafkaListener(
    topics = "#{@kafkaTopicPatterns.taskExecutionsPattern}",
    groupId = "executor-group",
    containerFactory = "tenantAwareKafkaListenerContainerFactory"
)
```

## Best Practices

### 1. Topic Naming
- Use consistent naming patterns across all services
- Include tenant ID as the last component of topic names
- Use descriptive prefixes for different message types

### 2. Tenant ID Extraction
- Always validate tenant IDs before processing
- Use fallback logic for tenant ID extraction
- Log tenant context for debugging

### 3. Error Handling
- Implement tenant-specific error handling
- Use tenant-aware dead letter queues
- Monitor per-tenant error rates

### 4. Performance
- Configure appropriate concurrency per tenant
- Monitor topic partition distribution
- Use tenant-specific consumer groups for isolation

## Monitoring and Observability

### 1. Metrics
- Per-tenant message processing rates
- Tenant-specific error rates
- Topic partition distribution

### 2. Logging
- Include tenant ID in all log messages
- Use structured logging for tenant context
- Monitor tenant-specific log patterns

### 3. Alerting
- Set up alerts for tenant-specific issues
- Monitor tenant message processing delays
- Alert on tenant-specific error thresholds

## TaskInput Message Flow

`executor-java` receives TaskInput messages (the upstream PlanExecution) on the task-input listener path.

## Future Enhancements

### 1. Dynamic Tenant Management
- Runtime tenant registration/deregistration
- Tenant-specific configuration hot-reloading
- Automatic topic creation for new tenants

### 2. Advanced Routing
- Tenant-specific message routing rules
- Priority-based tenant processing
- Tenant-specific message transformation

### 3. Enhanced Monitoring
- Real-time tenant activity dashboards
- Tenant-specific performance metrics
- Predictive scaling based on tenant usage patterns 
