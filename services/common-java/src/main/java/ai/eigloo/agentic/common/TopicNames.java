package ai.eigloo.agentic.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

/**
 * Utility class for Kafka topic naming conventions used across all microservices.
 * Provides consistent topic naming and tenant ID extraction methods.
 * 
 * The system uses only tenant-specific topics with the pattern {prefix}-{tenantId}
 * where prefix is one of: task-executions, plan-executions, persisted-task-executions, 
 * persisted-plan-executions, plan-inputs, task-inputs.
 */
public class TopicNames {
    
    private static final Logger logger = LoggerFactory.getLogger(TopicNames.class);
    
    private TopicNames() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Generate task executions topic name for a tenant.
     * 
     * @param tenantId the tenant identifier
     * @return topic name in format: task-executions-{tenantId}
     */
    public static String taskExecutions(String tenantId) {
        return "task-executions-" + tenantId;
    }
    
    /**
     * Generate plan executions topic name for a tenant.
     * 
     * @param tenantId the tenant identifier
     * @return topic name in format: plan-executions-{tenantId}
     */
    public static String planExecutions(String tenantId) {
        return "plan-executions-" + tenantId;
    }
    
    /**
     * Generate persisted task executions topic name for a tenant.
     * 
     * @param tenantId the tenant identifier
     * @return topic name in format: persisted-task-executions-{tenantId}
     */
    public static String persistedTaskExecutions(String tenantId) {
        return "persisted-task-executions-" + tenantId;
    }
    
    /**
     * Generate persisted plan executions topic name for a tenant.
     * 
     * @param tenantId the tenant identifier
     * @return topic name in format: persisted-plan-executions-{tenantId}
     */
    public static String persistedPlanExecutions(String tenantId) {
        return "persisted-plan-executions-" + tenantId;
    }
    
    /**
     * Generate plan inputs topic name for a tenant.
     * 
     * @param tenantId the tenant identifier
     * @return topic name in format: plan-inputs-{tenantId}
     */
    public static String planInputs(String tenantId) {
        return "plan-inputs-" + tenantId;
    }
    
    /**
     * Generate task inputs topic name for a tenant.
     * 
     * @param tenantId the tenant identifier
     * @return topic name in format: task-inputs-{tenantId}
     */
    public static String taskInputs(String tenantId) {
        return "task-inputs-" + tenantId;
    }

    /**
     * Build a Kafka key that scopes node traffic to a graph.
     *
     * @param graphId graph identifier
     * @param nodeName plan/task node name
     * @return key in format: {graphId}:{nodeName}
     */
    public static String graphNodeKey(String graphId, String nodeName) {
        if (graphId == null || graphId.isBlank()) {
            throw new IllegalArgumentException("graphId is required for Kafka key construction");
        }
        if (nodeName == null || nodeName.isBlank()) {
            throw new IllegalArgumentException("nodeName is required for Kafka key construction");
        }
        return graphId + ":" + nodeName;
    }
    

    
    /**
     * Extract tenant ID from a topic name by splitting on the last hyphen.
     * Handles patterns like task-executions-{tenantId}, etc.
     * 
     * @param topicName the full topic name
     * @return the tenant ID, or null if not found
     */
    public static String extractTenantId(String topicName) {
        if (topicName == null || topicName.isEmpty()) {
            logger.warn("Cannot extract tenant ID from null or empty topic name");
            return null;
        }
        
        // Split on hyphen and take the last part as tenant ID
        String[] parts = topicName.split("-");
        if (parts.length >= 3) {
            return parts[parts.length - 1];
        }
        
        logger.warn("Topic name '{}' does not contain valid tenant ID separator", topicName);
        return null;
    }
    
    /**
     * Extract tenant ID from message headers.
     * Looks for tenant_id, tenantId, or x-tenant-id headers.
     * 
     * @param headers the message headers map
     * @return the tenant ID, or null if not found
     */
    public static String extractTenantIdFromHeaders(Map<String, Object> headers) {
        if (headers == null || headers.isEmpty()) {
            logger.debug("Cannot extract tenant ID from null or empty headers");
            return null;
        }
        
        // Try different header names for tenant ID
        String[] headerNames = {"tenant_id", "tenantId", "x-tenant-id", "X-Tenant-ID"};
        
        for (String headerName : headerNames) {
            Object value = headers.get(headerName);
            if (value != null && !value.toString().isEmpty()) {
                logger.debug("Found tenant ID in header '{}': {}", headerName, value);
                return value.toString();
            }
        }
        
        logger.debug("No tenant ID found in message headers");
        return null;
    }
    
    /**
     * Extract tenant ID from message payload.
     * Looks for tenant_id or tenantId fields in the message data.
     * 
     * @param payload the message payload map
     * @return the tenant ID, or null if not found
     */
    public static String extractTenantIdFromPayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            logger.debug("Cannot extract tenant ID from null or empty payload");
            return null;
        }
        
        // Try different field names for tenant ID
        String[] fieldNames = {"tenant_id", "tenantId", "tenant-id"};
        
        for (String fieldName : fieldNames) {
            Object value = payload.get(fieldName);
            if (value != null && !value.toString().isEmpty()) {
                logger.debug("Found tenant ID in payload field '{}': {}", fieldName, value);
                return value.toString();
            }
        }
        
        logger.debug("No tenant ID found in message payload");
        return null;
    }
    
    /**
     * Extract tenant ID from multiple sources with fallback logic.
     * Priority: 1. Message headers, 2. Message payload, 3. Topic name
     * 
     * @param topicName the topic name
     * @param headers the message headers (can be null)
     * @param payload the message payload (can be null)
     * @return the tenant ID, or null if not found in any source
     */
    public static String extractTenantIdFromMultipleSources(String topicName, Map<String, Object> headers, Map<String, Object> payload) {
        // Try headers first
        String tenantId = extractTenantIdFromHeaders(headers);
        if (tenantId != null) {
            return tenantId;
        }
        
        // Try payload second
        tenantId = extractTenantIdFromPayload(payload);
        if (tenantId != null) {
            return tenantId;
        }
        
        // Fall back to topic name
        return extractTenantId(topicName);
    }
    
    /**
     * Validate if a topic name follows the expected pattern.
     * 
     * @param topicName the topic name to validate
     * @return true if the topic name is valid, false otherwise
     */
    public static boolean isValidTopicName(String topicName) {
        if (topicName == null || topicName.isEmpty()) {
            return false;
        }
        
        // Check for all topic patterns with hyphen separator
        if (topicName.contains("-")) {
            String[] parts = topicName.split("-");
            if (parts.length >= 3) {
                String prefix = parts[0] + "-" + parts[1];
                return prefix.equals("task-executions") || 
                       prefix.equals("plan-executions") || 
                       prefix.equals("persisted-task-executions") || 
                       prefix.equals("persisted-plan-executions") ||
                       prefix.equals("plan-inputs") ||
                       prefix.equals("task-inputs");
            }
        }
        
        return false;
    }
} 
