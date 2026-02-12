package ai.eigloo.agentic.controlplane.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configures control-plane internal read access to data-plane APIs.
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
