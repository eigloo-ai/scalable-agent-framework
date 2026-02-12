package ai.eigloo.agentic.graphcomposer;

import ai.eigloo.agentic.graphcomposer.config.DataPlaneClientProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Main Spring Boot application class for the Graph Composer service.
 * This service provides a visual editor for creating and managing Agent Graphs
 * in the Agentic Framework.
 */
@SpringBootApplication
@EntityScan(basePackages = {
    "ai.eigloo.agentic.graphcomposer.entity",
    "ai.eigloo.agentic.graph.entity"
})
@EnableJpaRepositories(basePackages = {
    "ai.eigloo.agentic.graphcomposer.repository",
    "ai.eigloo.agentic.graph.repository"
})
@EnableConfigurationProperties(DataPlaneClientProperties.class)
public class GraphComposerApplication {

    public static void main(String[] args) {
        SpringApplication.run(GraphComposerApplication.class, args);
    }
}
