package ai.eigloo.agentic.graphbuilder.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Configuration class for graph bundle processing.
 * Enables configuration properties and async processing.
 */
@Configuration
@EnableConfigurationProperties(GraphBundleProperties.class)
@EnableAsync
public class GraphBundleConfiguration {
    // Configuration is handled through @EnableConfigurationProperties
}