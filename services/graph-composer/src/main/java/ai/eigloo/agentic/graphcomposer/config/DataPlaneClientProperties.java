package ai.eigloo.agentic.graphcomposer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configures graph-composer read access to data-plane observability APIs.
 */
@ConfigurationProperties(prefix = "agentic.data-plane")
public class DataPlaneClientProperties {

    private String baseUrl = "http://localhost:8081";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
