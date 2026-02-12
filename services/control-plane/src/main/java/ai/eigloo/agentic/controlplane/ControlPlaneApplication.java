package ai.eigloo.agentic.controlplane;

import ai.eigloo.agentic.common.KafkaTopicPatterns;
import ai.eigloo.agentic.common.TenantAwareKafkaConfig;
import ai.eigloo.agentic.controlplane.config.DataPlaneClientProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
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
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
@EnableKafka
@EnableConfigurationProperties(DataPlaneClientProperties.class)
@Import({TenantAwareKafkaConfig.class, KafkaTopicPatterns.class})
public class ControlPlaneApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ControlPlaneApplication.class, args);
    }
} 
