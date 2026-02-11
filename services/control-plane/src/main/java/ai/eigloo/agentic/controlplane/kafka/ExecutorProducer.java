package ai.eigloo.agentic.controlplane.kafka;

import ai.eigloo.agentic.common.TopicNames;
import ai.eigloo.agentic.common.ProtobufUtils;
import ai.eigloo.proto.model.Common.PlanExecution;
import ai.eigloo.proto.model.Common.PlanInput;
import ai.eigloo.proto.model.Common.TaskInput;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer for the Control Plane service.
 * 
 * This producer correctly routes protobuf messages:
 * - PlanInput messages to plan-inputs-{tenantId} topics (for PlanExecutor to consume)
 * - TaskInput messages to task-inputs-{tenantId} topics (for TaskExecutor to consume)
 * - Enhanced with proper parent relationship handling and logging
 */
@Component
public class ExecutorProducer {
    
    private static final Logger logger = LoggerFactory.getLogger(ExecutorProducer.class);
    
    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    
    @Autowired
    public ExecutorProducer(KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    /**
     * Publish PlanInput protobuf to plan-inputs topic for PlanExecutor to consume.
     * 
     * @param tenantId the tenant identifier
     * @param planInput the PlanInput protobuf message
     * @return CompletableFuture for the send result
     */
    public CompletableFuture<SendResult<String, byte[]>> publishPlanInput(String tenantId, PlanInput planInput) {
        try {
            String topic = TopicNames.planInputs(tenantId);
            validateInputContext(planInput.getGraphId(), planInput.getLifetimeId(), "PlanInput");
            
            byte[] message = ProtobufUtils.serializePlanInput(planInput);
            if (message == null) {
                throw new RuntimeException("Failed to serialize PlanInput");
            }
            
            String messageKey = TopicNames.graphNodeKey(planInput.getGraphId(), planInput.getPlanName());
            
            logger.info(
                    "Publishing PlanInput tenant={} graph={} lifetime={} plan={} inputId={} topic={} key={}",
                    tenantId,
                    planInput.getGraphId(),
                    planInput.getLifetimeId(),
                    planInput.getPlanName(),
                    planInput.getInputId(),
                    topic,
                    messageKey);
            
            ProducerRecord<String, byte[]> record = new ProducerRecord<>(topic, messageKey, message);
            return kafkaTemplate.send(record);
            
        } catch (Exception e) {
            logger.error("Failed to publish PlanInput protobuf for tenant {}: {}", tenantId, e.getMessage(), e);
            CompletableFuture<SendResult<String, byte[]>> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
    
    /**
     * Publish TaskInput protobuf to task-inputs topic for TaskExecutor to consume.
     * 
     * @param tenantId the tenant identifier
     * @param taskInput the TaskInput protobuf message
     * @return CompletableFuture for the send result
     */
    public CompletableFuture<SendResult<String, byte[]>> publishTaskInput(String tenantId, TaskInput taskInput) {
        try {
            String topic = TopicNames.taskInputs(tenantId);
            validateInputContext(taskInput.getGraphId(), taskInput.getLifetimeId(), "TaskInput");
            
            byte[] message = ProtobufUtils.serializeTaskInput(taskInput);
            if (message == null) {
                throw new RuntimeException("Failed to serialize TaskInput");
            }
            
            String messageKey = TopicNames.graphNodeKey(taskInput.getGraphId(), taskInput.getTaskName());
            
            logger.info(
                    "Publishing TaskInput tenant={} graph={} lifetime={} task={} inputId={} topic={} key={}",
                    tenantId,
                    taskInput.getGraphId(),
                    taskInput.getLifetimeId(),
                    taskInput.getTaskName(),
                    taskInput.getInputId(),
                    topic,
                    messageKey);
            
            ProducerRecord<String, byte[]> record = new ProducerRecord<>(topic, messageKey, message);
            return kafkaTemplate.send(record);
            
        } catch (Exception e) {
            logger.error("Failed to publish TaskInput protobuf for tenant {}: {}", tenantId, e.getMessage(), e);
            CompletableFuture<SendResult<String, byte[]>> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
    

    /**
     * Log enhanced parent relationship information for PlanExecution messages
     * 
     * @param messageType the type of message
     * @param planExecution the PlanExecution message
     * @param tenantId the tenant identifier
     */
    private void logParentRelationshipInfo(String messageType, PlanExecution planExecution, String tenantId) {
        if (planExecution != null) {
            var parentTaskExecIds = planExecution.getParentTaskExecIdsList();
            var parentTaskNames = planExecution.getParentTaskNamesList();
            
            logger.debug("{} parent relationships for tenant {}: parent_task_exec_ids={}, parent_task_names={}", 
                messageType, tenantId, parentTaskExecIds, parentTaskNames);
            
            // Log upstream task results information
            if (planExecution.hasResult()) {
                var upstreamResults = planExecution.getResult().getUpstreamTasksResultsList();
                logger.debug("{} has {} upstream task results for tenant {}", 
                    messageType, upstreamResults.size(), tenantId);
            }
        }
    }
    
    /**
     * Handle send result and log success/failure.
     * 
     * @param future the CompletableFuture from the send operation
     * @param tenantId the tenant identifier
     * @param messageId the message identifier
     * @param messageType the type of message (PlanInput/PlanExecution)
     */
    public void handleSendResult(CompletableFuture<SendResult<String, byte[]>> future, 
                                String tenantId, String messageId, String messageType) {
        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                logger.error("Failed to publish {} protobuf for tenant {} message {}: {}", 
                    messageType, tenantId, messageId, throwable.getMessage());
            } else {
                logger.debug("Successfully published {} protobuf for tenant {} message {} to topic {}", 
                    messageType, tenantId, messageId, result.getRecordMetadata().topic());
            }
        });
    }

    private static void validateInputContext(String graphId, String lifetimeId, String messageType) {
        if (graphId == null || graphId.isBlank()) {
            throw new IllegalArgumentException(messageType + ".graph_id is required");
        }
        if (lifetimeId == null || lifetimeId.isBlank()) {
            throw new IllegalArgumentException(messageType + ".lifetime_id is required");
        }
    }
}
