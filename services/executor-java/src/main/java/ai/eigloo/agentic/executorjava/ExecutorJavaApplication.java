package ai.eigloo.agentic.executorjava;

import ai.eigloo.agentic.common.KafkaTopicPatterns;
import ai.eigloo.agentic.common.TenantAwareKafkaConfig;
import ai.eigloo.agentic.executorjava.config.DataPlaneClientProperties;
import ai.eigloo.agentic.executorjava.config.ExecutorPythonProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Main Spring Boot application for executor-java.
 */
@SpringBootApplication(
        scanBasePackages = "ai.eigloo.agentic.executorjava",
        exclude = {
                DataSourceAutoConfiguration.class,
                DataSourceTransactionManagerAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class
        })
@EnableKafka
@EnableConfigurationProperties({ExecutorPythonProperties.class, DataPlaneClientProperties.class})
@Import({TenantAwareKafkaConfig.class, KafkaTopicPatterns.class})
public class ExecutorJavaApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExecutorJavaApplication.class, args);
    }
}
