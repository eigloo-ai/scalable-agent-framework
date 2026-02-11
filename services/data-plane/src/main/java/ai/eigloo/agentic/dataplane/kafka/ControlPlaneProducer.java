package ai.eigloo.agentic.dataplane.kafka;

import ai.eigloo.agentic.common.TopicNames;
import ai.eigloo.agentic.common.ProtobufUtils;
import ai.eigloo.proto.model.Common.ExecutionHeader;
import ai.eigloo.proto.model.Common.TaskExecution;
import ai.eigloo.proto.model.Common.PlanExecution;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer service for publishing protobuf messages to control plane topics.
 * Sends TaskExecution and PlanExecution protobuf messages to persisted-task-executions-{tenantId} and persisted-plan-executions-{tenantId} topics.
 */
@Service
public class ControlPlaneProducer {
    
    private static final Logger logger = LoggerFactory.getLogger(ControlPlaneProducer.class);
    
    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    
    @Autowired
    public ControlPlaneProducer(KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    /**
     * Publish a TaskExecution protobuf message to the persisted-task-executions topic.
     * 
     * @param tenantId the tenant identifier
     * @param taskExecution the TaskExecution protobuf message to publish
     * @return CompletableFuture for the send result
     */
    public CompletableFuture<SendResult<String, byte[]>> publishTaskExecution(
            String tenantId, TaskExecution taskExecution) {
        
        try {
            String topic = TopicNames.persistedTaskExecutions(tenantId);
            ExecutionHeader header = requiredHeader(taskExecution.hasHeader() ? taskExecution.getHeader() : null, "TaskExecution");
            String messageKey = TopicNames.graphNodeKey(header.getGraphId(), header.getName());
            
            byte[] message = ProtobufUtils.serializeTaskExecution(taskExecution);
            if (message == null) {
                throw new RuntimeException("Failed to serialize TaskExecution");
            }
            
            logger.debug("Publishing TaskExecution protobuf to topic {}: {}", topic, messageKey);
            
            ProducerRecord<String, byte[]> record = new ProducerRecord<>(topic, messageKey, message);
            return kafkaTemplate.send(record);
            
        } catch (Exception e) {
            logger.error("Failed to publish TaskExecution for tenant {}: {}", 
                tenantId, e.getMessage(), e);
            CompletableFuture<SendResult<String, byte[]>> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
    
    /**
     * Publish a PlanExecution protobuf message to the persisted-plan-executions topic.
     * 
     * @param tenantId the tenant identifier
     * @param planExecution the PlanExecution protobuf message to publish
     * @return CompletableFuture for the send result
     */
    public CompletableFuture<SendResult<String, byte[]>> publishPlanExecution(
            String tenantId, PlanExecution planExecution) {
        
        try {
            String topic = TopicNames.persistedPlanExecutions(tenantId);
            ExecutionHeader header = requiredHeader(planExecution.hasHeader() ? planExecution.getHeader() : null, "PlanExecution");
            String messageKey = TopicNames.graphNodeKey(header.getGraphId(), header.getName());
            
            byte[] message = ProtobufUtils.serializePlanExecution(planExecution);
            if (message == null) {
                throw new RuntimeException("Failed to serialize PlanExecution");
            }
            
            logger.debug("Publishing PlanExecution protobuf to topic {}: {}", topic, messageKey);
            
            ProducerRecord<String, byte[]> record = new ProducerRecord<>(topic, messageKey, message);
            return kafkaTemplate.send(record);
            
        } catch (Exception e) {
            logger.error("Failed to publish PlanExecution for tenant {}: {}", 
                tenantId, e.getMessage(), e);
            CompletableFuture<SendResult<String, byte[]>> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
    
    /**
     * Handle send result and log success/failure.
     * 
     * @param future the CompletableFuture from the send operation
     * @param tenantId the tenant identifier
     * @param executionId the execution identifier
     * @param executionType the type of execution (task/plan)
     */
    public void handleSendResult(CompletableFuture<SendResult<String, byte[]>> future, 
                                String tenantId, String executionId, String executionType) {
        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                logger.error("Failed to publish {} protobuf for tenant {} execution {}: {}", 
                    executionType, tenantId, executionId, throwable.getMessage());
            } else {
                logger.debug("Successfully published {} protobuf for tenant {} execution {} to topic {}", 
                    executionType, tenantId, executionId, result.getRecordMetadata().topic());
            }
        });
    }

    private static ExecutionHeader requiredHeader(ExecutionHeader header, String messageType) {
        if (header == null) {
            throw new IllegalArgumentException(messageType + ".header is required");
        }
        if (header.getGraphId().isBlank()) {
            throw new IllegalArgumentException(messageType + ".header.graph_id is required");
        }
        if (header.getLifetimeId().isBlank()) {
            throw new IllegalArgumentException(messageType + ".header.lifetime_id is required");
        }
        if (header.getName().isBlank()) {
            throw new IllegalArgumentException(messageType + ".header.name is required");
        }
        return header;
    }
}
