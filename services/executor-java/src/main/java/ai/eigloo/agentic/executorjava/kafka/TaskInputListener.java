package ai.eigloo.agentic.executorjava.kafka;

import ai.eigloo.agentic.common.ProtobufUtils;
import ai.eigloo.agentic.common.TopicNames;
import ai.eigloo.agentic.executorjava.service.ExecutorOrchestrationService;
import ai.eigloo.proto.model.Common.TaskExecution;
import ai.eigloo.proto.model.Common.TaskInput;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class TaskInputListener {

    private static final Logger logger = LoggerFactory.getLogger(TaskInputListener.class);

    private final ExecutorOrchestrationService orchestrationService;
    private final ExecutorOutputProducer executorOutputProducer;

    public TaskInputListener(
            ExecutorOrchestrationService orchestrationService,
            ExecutorOutputProducer executorOutputProducer) {
        this.orchestrationService = orchestrationService;
        this.executorOutputProducer = executorOutputProducer;
    }

    @KafkaListener(
            topics = "#{@kafkaTopicPatterns.taskInputsPattern}",
            groupId = "executor-java-task-inputs",
            containerFactory = "tenantAwareKafkaListenerContainerFactory"
    )
    public void handleTaskInput(
            ConsumerRecord<String, byte[]> record,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {
        try {
            String tenantId = TopicNames.extractTenantId(topic);
            if (tenantId == null || tenantId.isBlank()) {
                logger.error("Could not extract tenant id from topic {}", topic);
                acknowledgment.acknowledge();
                return;
            }

            TaskInput taskInput = ProtobufUtils.deserializeTaskInput(record.value());
            if (taskInput == null) {
                logger.error("Could not deserialize TaskInput from topic {}", topic);
                acknowledgment.acknowledge();
                return;
            }

            TaskExecution execution = orchestrationService.handleTaskInput(tenantId, taskInput);
            executorOutputProducer.publishTaskExecution(tenantId, execution).join();
            acknowledgment.acknowledge();
        } catch (Exception e) {
            logger.error("Error handling TaskInput from topic {}: {}", topic, e.getMessage(), e);
        }
    }
}
