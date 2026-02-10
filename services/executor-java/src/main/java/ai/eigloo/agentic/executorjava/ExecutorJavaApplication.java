package ai.eigloo.agentic.executorjava;

import ai.eigloo.agentic.common.KafkaTopicPatterns;
import ai.eigloo.agentic.common.TenantAwareKafkaConfig;
import ai.eigloo.agentic.executorjava.config.ExecutorPythonProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Main Spring Boot application for executor-java.
 */
@SpringBootApplication(scanBasePackages = "ai.eigloo.agentic.executorjava")
@EnableKafka
@EntityScan(basePackages = "ai.eigloo.agentic.graph.entity")
@EnableJpaRepositories(basePackages = "ai.eigloo.agentic.graph.repository")
@EnableConfigurationProperties(ExecutorPythonProperties.class)
@Import({TenantAwareKafkaConfig.class, KafkaTopicPatterns.class})
public class ExecutorJavaApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExecutorJavaApplication.class, args);
    }
}
