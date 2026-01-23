package ai.eigloo.agentic.dataplane;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Main Spring Boot application class for the Data Plane microservice.
 * 
 * This service is responsible for:
 * - Consuming TaskExecution and PlanExecution messages from Kafka
 * - Persisting execution data to PostgreSQL database
 * - Publishing control references to control plane topics
 * - Providing REST API endpoints for data access
 */
@SpringBootApplication
@EnableJpaRepositories(basePackages = "ai.eigloo.agentic.dataplane.repository")
@EnableKafka
public class DataPlaneApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(DataPlaneApplication.class, args);
    }
} 