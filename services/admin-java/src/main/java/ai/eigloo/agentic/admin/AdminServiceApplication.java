package ai.eigloo.agentic.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Main Spring Boot application class for the Admin microservice.
 * 
 * This service is responsible for:
 * - Managing tenant provisioning and lifecycle
 * - Creating and deleting Kafka topics for tenants
 * - Providing REST API endpoints for tenant management
 * - Serving web interface for tenant administration
 */
@SpringBootApplication
@EnableJpaRepositories(basePackages = "ai.eigloo.agentic.admin.repository")
@EnableKafka
public class AdminServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(AdminServiceApplication.class, args);
    }
} 