package ai.eigloo.agentic.graphcomposer.service;

import ai.eigloo.agentic.common.ProtobufUtils;
import ai.eigloo.agentic.common.TopicNames;
import ai.eigloo.proto.model.Common.PlanInput;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GraphExecutionBootstrapPublisher {

    private static final Logger logger = LoggerFactory.getLogger(GraphExecutionBootstrapPublisher.class);

    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    public GraphExecutionBootstrapPublisher(
            @Qualifier("graphComposerKafkaTemplate") KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishStartPlanInput(String tenantId, String graphId, String lifetimeId, String planName) {
        PlanInput planInput = PlanInput.newBuilder()
                .setInputId(UUID.randomUUID().toString())
                .setPlanName(planName)
                .setGraphId(graphId)
                .setLifetimeId(lifetimeId)
                .build();

        byte[] payload = ProtobufUtils.serializePlanInput(planInput);
        if (payload == null) {
            throw new IllegalStateException("Failed to serialize bootstrap PlanInput for plan " + planName);
        }

        String topic = TopicNames.planInputs(tenantId);
        String key = TopicNames.graphNodeKey(graphId, planName);
        logger.info(
                "Publishing bootstrap PlanInput tenant={} graph={} lifetime={} plan={} topic={} key={}",
                tenantId, graphId, lifetimeId, planName, topic, key);

        kafkaTemplate.send(new ProducerRecord<>(topic, key, payload)).join();
    }
}
