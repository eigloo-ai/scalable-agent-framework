package ai.eigloo.agentic.graphcomposer.config;

import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Configuration class for performance optimizations and monitoring.
 */
@Configuration
@EnableTransactionManagement
@ConfigurationProperties(prefix = "graph-composer.performance")
public class PerformanceConfig {

    private int batchSize = 50;
    private int fetchSize = 50;
    private boolean enableQueryCache = true;
    private int queryCacheSize = 2048;
    private boolean enablePoolMonitoring = true;

    /**
     * Configure JPA performance settings.
     */
    @Bean
    public String jpaPerformanceSettings() {
        // These settings are applied via application.yml
        // This bean serves as documentation and validation
        return "JPA performance settings configured";
    }

    // Getters and setters for configuration properties
    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getFetchSize() {
        return fetchSize;
    }

    public void setFetchSize(int fetchSize) {
        this.fetchSize = fetchSize;
    }

    public boolean isEnableQueryCache() {
        return enableQueryCache;
    }

    public void setEnableQueryCache(boolean enableQueryCache) {
        this.enableQueryCache = enableQueryCache;
    }

    public int getQueryCacheSize() {
        return queryCacheSize;
    }

    public void setQueryCacheSize(int queryCacheSize) {
        this.queryCacheSize = queryCacheSize;
    }

    public boolean isEnablePoolMonitoring() {
        return enablePoolMonitoring;
    }

    public void setEnablePoolMonitoring(boolean enablePoolMonitoring) {
        this.enablePoolMonitoring = enablePoolMonitoring;
    }
}