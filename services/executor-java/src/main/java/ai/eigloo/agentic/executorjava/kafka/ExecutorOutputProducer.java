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
        byte[] payload = ProtobufUtils.serializePlanExecution(planExecution);
        if (payload == null) {
            CompletableFuture<SendResult<String, byte[]>> failed = new CompletableFuture<>();
            failed.completeExceptionally(new IllegalStateException("Failed to serialize PlanExecution"));
            return failed;
        }
        String topic = TopicNames.planExecutions(tenantId);
        String key = planExecution.getHeader().getName();
        logger.debug("Publishing PlanExecution to topic {} key {}", topic, key);
        return kafkaTemplate.send(new ProducerRecord<>(topic, key, payload));
    }

    public CompletableFuture<SendResult<String, byte[]>> publishTaskExecution(String tenantId, TaskExecution taskExecution) {
        byte[] payload = ProtobufUtils.serializeTaskExecution(taskExecution);
        if (payload == null) {
            CompletableFuture<SendResult<String, byte[]>> failed = new CompletableFuture<>();
            failed.completeExceptionally(new IllegalStateException("Failed to serialize TaskExecution"));
            return failed;
        }
        String topic = TopicNames.taskExecutions(tenantId);
        String key = taskExecution.getHeader().getName();
        logger.debug("Publishing TaskExecution to topic {} key {}", topic, key);
        return kafkaTemplate.send(new ProducerRecord<>(topic, key, payload));
    }
}
