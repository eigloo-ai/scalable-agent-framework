package ai.eigloo.agentic.executorjava.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configures executor-java internal read access to data-plane APIs.
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
