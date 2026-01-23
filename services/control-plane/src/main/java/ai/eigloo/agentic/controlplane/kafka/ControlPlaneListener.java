package ai.eigloo.agentic.controlplane.kafka;

import ai.eigloo.agentic.common.ProtobufUtils;
import ai.eigloo.agentic.common.TopicNames;
import ai.eigloo.agentic.controlplane.service.ExecutionRouter;
import ai.eigloo.proto.model.Common.TaskExecution;
import ai.eigloo.proto.model.Common.PlanExecution;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Kafka listener for the Control Plane service.
 * 
 * This consumer:
 * - Listens to data plane topics for execution messages (persisted-task-executions-{tenantId}, persisted-plan-executions-{tenantId})
 * - Processes execution messages for guardrail evaluation
 * - Routes messages to appropriate handlers via ExecutionRouter
 */
@Component
public class ControlPlaneListener {
    
    private static final Logger logger = LoggerFactory.getLogger(ControlPlaneListener.class);
    
    private final ExecutionRouter executionRouter;
    
    @Autowired
    public ControlPlaneListener(ExecutionRouter executionRouter) {
        this.executionRouter = executionRouter;
    }
    
    /**
     * Consume task execution protobuf messages from data plane.
     * 
     * @param record the Kafka consumer record
     * @param topic the topic name
     * @param acknowledgment manual acknowledgment
     */
    @KafkaListener(
        topics = "#{@kafkaTopicPatterns.persistedTaskExecutionsPattern}",
        groupId = "control-plane-persisted-task-executions",
        containerFactory = "tenantAwareKafkaListenerContainerFactory"
    )
    public void handleTaskExecution(
            ConsumerRecord<String, byte[]> record,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {
        
        try {
            logger.debug("Received task execution protobuf message from topic: {}", topic);
            
            // Deserialize protobuf message
            TaskExecution taskExecution = ProtobufUtils.deserializeTaskExecution(record.value());
            if (taskExecution == null) {
                logger.error("Failed to deserialize TaskExecution message from topic: {}", topic);
                acknowledgment.acknowledge();
                return;
            }
            
            // Extract tenant ID from topic name
            String tenantId = TopicNames.extractTenantId(topic);
            if (tenantId == null) {
                logger.error("Could not extract tenant ID from topic: {}", topic);
                acknowledgment.acknowledge();
                return;
            }
            
            // Process task execution for guardrail evaluation and routing
            executionRouter.routeTaskExecution(taskExecution, tenantId);
            
            logger.info("Processed task execution protobuf message for tenant: {}", tenantId);
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            logger.error("Error processing task execution protobuf message from topic {}: {}", topic, e.getMessage(), e);
            // Don't acknowledge on error to allow retry
        }
    }
    
    /**
     * Consume plan execution protobuf messages from data plane.
     * 
     * @param record the Kafka consumer record
     * @param topic the topic name
     * @param acknowledgment manual acknowledgment
     */
    @KafkaListener(
        topics = "#{@kafkaTopicPatterns.persistedPlanExecutionsPattern}",
        groupId = "control-plane-persisted-plan-executions",
        containerFactory = "tenantAwareKafkaListenerContainerFactory"
    )
    public void handlePlanExecution(
            ConsumerRecord<String, byte[]> record,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {
        
        try {
            logger.debug("Received plan execution protobuf message from topic: {}", topic);
            
            // Deserialize protobuf message
            PlanExecution planExecution = ProtobufUtils.deserializePlanExecution(record.value());
            if (planExecution == null) {
                logger.error("Failed to deserialize PlanExecution message from topic: {}", topic);
                acknowledgment.acknowledge();
                return;
            }
            
            // Extract tenant ID from topic name
            String tenantId = TopicNames.extractTenantId(topic);
            if (tenantId == null) {
                logger.error("Could not extract tenant ID from topic: {}", topic);
                acknowledgment.acknowledge();
                return;
            }
            
            // Process plan execution for guardrail evaluation and routing
            executionRouter.routePlanExecution(planExecution, tenantId);
            
            logger.info("Processed plan execution protobuf message for tenant: {}", tenantId);
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            logger.error("Error processing plan execution protobuf message from topic {}: {}", topic, e.getMessage(), e);
            // Don't acknowledge on error to allow retry
        }
    }
} 