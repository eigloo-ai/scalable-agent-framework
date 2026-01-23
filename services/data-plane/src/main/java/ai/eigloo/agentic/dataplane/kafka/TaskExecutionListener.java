package ai.eigloo.agentic.dataplane.kafka;

import ai.eigloo.agentic.common.ProtobufUtils;
import ai.eigloo.agentic.common.TopicNames;
import ai.eigloo.agentic.dataplane.service.PersistenceService;
import ai.eigloo.proto.model.Common.TaskExecution;
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
 * Kafka listener for TaskExecution messages from task-executors.
 * Consumes messages from task-executions-{tenantId} topics and persists them to the database.
 * Publishes TaskExecution protobuf messages to persisted-task-executions-{tenantId} topics for the control plane.
 */
@Component
public class TaskExecutionListener {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskExecutionListener.class);
    
    private final PersistenceService persistenceService;
    private final ControlPlaneProducer controlPlaneProducer;
    
    @Autowired
    public TaskExecutionListener(PersistenceService persistenceService, 
                               ControlPlaneProducer controlPlaneProducer) {
        this.persistenceService = persistenceService;
        this.controlPlaneProducer = controlPlaneProducer;
    }
    
    /**
     * Listen for TaskExecution messages from task-executions topics.
     * 
     * @param record the Kafka consumer record
     * @param topic the topic name
     * @param acknowledgment manual acknowledgment
     */
    @KafkaListener(
        topics = "#{@kafkaTopicPatterns.taskExecutionsPattern}",
        groupId = "data-plane-task-executions",
        containerFactory = "tenantAwareKafkaListenerContainerFactory"
    )
    public void handleTaskExecution(
            ConsumerRecord<String, byte[]> record,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {
        
        try {
            logger.debug("Received TaskExecution message from topic: {}", topic);
            
            // Extract tenant ID from topic name
            String tenantId = TopicNames.extractTenantId(topic);
            if (tenantId == null) {
                logger.error("Could not extract tenant ID from topic: {}", topic);
                acknowledgment.acknowledge();
                return;
            }
            
            // Deserialize protobuf message
            TaskExecution taskExecution = ProtobufUtils.deserializeTaskExecution(record.value());
            if (taskExecution == null) {
                logger.error("Failed to deserialize TaskExecution message from topic: {}", topic);
                acknowledgment.acknowledge();
                return;
            }
            
            // Validate message
            if (!ProtobufUtils.isValidMessage(taskExecution)) {
                logger.error("Invalid TaskExecution message received from topic: {}", topic);
                acknowledgment.acknowledge();
                return;
            }
            
            // Process the task execution and persist to database
            boolean success = persistenceService.processTaskExecution(taskExecution, tenantId);
            
            if (success) {
                logger.debug("Successfully processed TaskExecution {}/{} for tenant {}", 
                    taskExecution.getHeader().getName(), taskExecution.getHeader().getExecId(), tenantId);
                
                // Publish TaskExecution protobuf message to control plane
                controlPlaneProducer.publishTaskExecution(tenantId, taskExecution);
                    
            } else {
                logger.error("Failed to process TaskExecution {}/{} for tenant {}", 
                    taskExecution.getHeader().getName(), taskExecution.getHeader().getExecId(), tenantId);
            }
            
            // Acknowledge the message
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            logger.error("Error processing TaskExecution message from topic {}: {}", topic, e.getMessage(), e);
            // Don't acknowledge on error to allow retry
        }
    }
} 