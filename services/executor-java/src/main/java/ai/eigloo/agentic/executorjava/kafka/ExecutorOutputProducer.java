package ai.eigloo.agentic.executorjava.kafka;

import ai.eigloo.agentic.common.ProtobufUtils;
import ai.eigloo.agentic.common.TopicNames;
import ai.eigloo.proto.model.Common.PlanExecution;
import ai.eigloo.proto.model.Common.TaskExecution;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class ExecutorOutputProducer {

    private static final Logger logger = LoggerFactory.getLogger(ExecutorOutputProducer.class);

    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    public ExecutorOutputProducer(KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public CompletableFuture<SendResult<String, byte[]>> publishPlanExecution(String tenantId, PlanExecution planExecution) {
        try {
            if (!planExecution.hasHeader()) {
                throw new IllegalArgumentException("PlanExecution.header is required");
            }
            if (planExecution.getHeader().getLifetimeId().isBlank()) {
                throw new IllegalArgumentException("PlanExecution.header.lifetime_id is required");
            }

            byte[] payload = ProtobufUtils.serializePlanExecution(planExecution);
            if (payload == null) {
                throw new IllegalStateException("Failed to serialize PlanExecution");
            }

            String topic = TopicNames.planExecutions(tenantId);
            String key = TopicNames.graphNodeKey(
                    planExecution.getHeader().getGraphId(),
                    planExecution.getHeader().getName());

            logger.debug("Publishing PlanExecution to topic {} key {}", topic, key);
            return kafkaTemplate.send(new ProducerRecord<>(topic, key, payload));
        } catch (Exception e) {
            CompletableFuture<SendResult<String, byte[]>> failed = new CompletableFuture<>();
            failed.completeExceptionally(e);
            return failed;
        }
    }

    public CompletableFuture<SendResult<String, byte[]>> publishTaskExecution(String tenantId, TaskExecution taskExecution) {
        try {
            if (!taskExecution.hasHeader()) {
                throw new IllegalArgumentException("TaskExecution.header is required");
            }
            if (taskExecution.getHeader().getLifetimeId().isBlank()) {
                throw new IllegalArgumentException("TaskExecution.header.lifetime_id is required");
            }

            byte[] payload = ProtobufUtils.serializeTaskExecution(taskExecution);
            if (payload == null) {
                throw new IllegalStateException("Failed to serialize TaskExecution");
            }

            String topic = TopicNames.taskExecutions(tenantId);
            String key = TopicNames.graphNodeKey(
                    taskExecution.getHeader().getGraphId(),
                    taskExecution.getHeader().getName());

            logger.debug("Publishing TaskExecution to topic {} key {}", topic, key);
            return kafkaTemplate.send(new ProducerRecord<>(topic, key, payload));
        } catch (Exception e) {
            CompletableFuture<SendResult<String, byte[]>> failed = new CompletableFuture<>();
            failed.completeExceptionally(e);
            return failed;
        }
    }
}
