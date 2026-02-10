package ai.eigloo.agentic.controlplane;

import ai.eigloo.agentic.common.KafkaTopicPatterns;
import ai.eigloo.agentic.common.TenantAwareKafkaConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Main Spring Boot application class for the Control Plane microservice.
 * 
 * This service is responsible for:
 * - Evaluating guardrails for task and plan executions
 * - Routing executions to appropriate queues
 * - Managing execution status and lifecycle
 * - Providing gRPC endpoints for control plane operations
 */
@SpringBootApplication
@EnableKafka
@EntityScan(basePackages = "ai.eigloo.agentic.graph.entity")
@EnableJpaRepositories(basePackages = "ai.eigloo.agentic.graph.repository")
@Import({TenantAwareKafkaConfig.class, KafkaTopicPatterns.class})
public class ControlPlaneApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ControlPlaneApplication.class, args);
    }
} 
