package ai.eigloo.agentic.controlplane;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
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
public class ControlPlaneApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ControlPlaneApplication.class, args);
    }
} 