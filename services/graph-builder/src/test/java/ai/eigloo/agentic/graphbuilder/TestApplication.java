package ai.eigloo.agentic.graphbuilder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Test application configuration for integration tests.
 */
@SpringBootApplication(scanBasePackages = {
    "ai.eigloo.agentic"
})
@EnableJpaRepositories(basePackages = {
    "ai.eigloo.agentic.graph.repository",
    "ai.eigloo.agentic.graphbuilder.repository"
})
@EntityScan(basePackages = {
    "ai.eigloo.agentic.graph.entity",
    "ai.eigloo.agentic.graphbuilder.entity"
})
public class TestApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}