package ai.eigloo.agentic.executorjava.kafka;

import ai.eigloo.agentic.common.ProtobufUtils;
import ai.eigloo.agentic.common.TopicNames;
import ai.eigloo.agentic.executorjava.service.ExecutorOrchestrationService;
import ai.eigloo.proto.model.Common.PlanExecution;
import ai.eigloo.proto.model.Common.PlanInput;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class PlanInputListener {

    private static final Logger logger = LoggerFactory.getLogger(PlanInputListener.class);

    private final ExecutorOrchestrationService orchestrationService;
    private final ExecutorOutputProducer executorOutputProducer;

    public PlanInputListener(
            ExecutorOrchestrationService orchestrationService,
            ExecutorOutputProducer executorOutputProducer) {
        this.orchestrationService = orchestrationService;
        this.executorOutputProducer = executorOutputProducer;
    }

    @KafkaListener(
            topics = "#{@kafkaTopicPatterns.planInputsPattern}",
            groupId = "executor-java-plan-inputs",
            containerFactory = "tenantAwareKafkaListenerContainerFactory"
    )
    public void handlePlanInput(
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

            PlanInput planInput = ProtobufUtils.deserializePlanInput(record.value());
            if (planInput == null) {
                logger.error("Could not deserialize PlanInput from topic {}", topic);
                acknowledgment.acknowledge();
                return;
            }

            PlanExecution execution = orchestrationService.handlePlanInput(tenantId, planInput);
            executorOutputProducer.publishPlanExecution(tenantId, execution).join();
            acknowledgment.acknowledge();
        } catch (Exception e) {
            logger.error("Error handling PlanInput from topic {}: {}", topic, e.getMessage(), e);
        }
    }
}
