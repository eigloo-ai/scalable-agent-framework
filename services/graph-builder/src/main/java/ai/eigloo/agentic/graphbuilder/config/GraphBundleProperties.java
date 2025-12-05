package ai.eigloo.agentic.graphbuilder.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.time.Duration;
import java.util.List;

/**
 * Configuration properties for graph bundle processing.
 * Maps to graph-bundle.* properties in application.yml
 */
@ConfigurationProperties(prefix = "graph-bundle")
public class GraphBundleProperties {

    private String tempDirectory = System.getProperty("java.io.tmpdir") + "/graph-bundles";
    
    @NestedConfigurationProperty
    private Cleanup cleanup = new Cleanup();
    
    @NestedConfigurationProperty
    private Upload upload = new Upload();
    
    @NestedConfigurationProperty
    private Docker docker = new Docker();
    
    @NestedConfigurationProperty
    private Processing processing = new Processing();

    // Getters and setters
    public String getTempDirectory() {
        return tempDirectory;
    }

    public void setTempDirectory(String tempDirectory) {
        this.tempDirectory = tempDirectory;
    }

    public Cleanup getCleanup() {
        return cleanup;
    }

    public void setCleanup(Cleanup cleanup) {
        this.cleanup = cleanup;
    }

    public Upload getUpload() {
        return upload;
    }

    public void setUpload(Upload upload) {
        this.upload = upload;
    }

    public Docker getDocker() {
        return docker;
    }

    public void setDocker(Docker docker) {
        this.docker = docker;
    }

    public Processing getProcessing() {
        return processing;
    }

    public void setProcessing(Processing processing) {
        this.processing = processing;
    }

    public static class Cleanup {
        private int tempFilesRetentionHours = 24;
        private int failedUploadsRetentionDays = 7;
        private int completedUploadsRetentionDays = 30;

        public int getTempFilesRetentionHours() {
            return tempFilesRetentionHours;
        }

        public void setTempFilesRetentionHours(int tempFilesRetentionHours) {
            this.tempFilesRetentionHours = tempFilesRetentionHours;
        }

        public int getFailedUploadsRetentionDays() {
            return failedUploadsRetentionDays;
        }

        public void setFailedUploadsRetentionDays(int failedUploadsRetentionDays) {
            this.failedUploadsRetentionDays = failedUploadsRetentionDays;
        }

        public int getCompletedUploadsRetentionDays() {
            return completedUploadsRetentionDays;
        }

        public void setCompletedUploadsRetentionDays(int completedUploadsRetentionDays) {
            this.completedUploadsRetentionDays = completedUploadsRetentionDays;
        }
    }

    public static class Upload {
        private String maxFileSize = "100MB";
        private List<String> allowedExtensions = List.of(".tar", ".tar.gz", ".zip");
        private int maxConcurrentUploads = 10;

        public String getMaxFileSize() {
            return maxFileSize;
        }

        public void setMaxFileSize(String maxFileSize) {
            this.maxFileSize = maxFileSize;
        }

        public List<String> getAllowedExtensions() {
            return allowedExtensions;
        }

        public void setAllowedExtensions(List<String> allowedExtensions) {
            this.allowedExtensions = allowedExtensions;
        }

        public int getMaxConcurrentUploads() {
            return maxConcurrentUploads;
        }

        public void setMaxConcurrentUploads(int maxConcurrentUploads) {
            this.maxConcurrentUploads = maxConcurrentUploads;
        }
    }

    public static class Docker {
        private String host = "unix:///var/run/docker.sock";
        private boolean tlsVerify = false;
        private String certPath = "";
        private String apiVersion = "1.41";
        private String registryUrl = "";
        private Duration connectionTimeout = Duration.ofSeconds(30);
        private Duration readTimeout = Duration.ofSeconds(60);
        private String baseImage = "services/executors-py:latest";
        
        @NestedConfigurationProperty
        private Build build = new Build();
        
        @NestedConfigurationProperty
        private Image image = new Image();

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public boolean isTlsVerify() {
            return tlsVerify;
        }

        public void setTlsVerify(boolean tlsVerify) {
            this.tlsVerify = tlsVerify;
        }

        public String getCertPath() {
            return certPath;
        }

        public void setCertPath(String certPath) {
            this.certPath = certPath;
        }

        public String getApiVersion() {
            return apiVersion;
        }

        public void setApiVersion(String apiVersion) {
            this.apiVersion = apiVersion;
        }

        public String getRegistryUrl() {
            return registryUrl;
        }

        public void setRegistryUrl(String registryUrl) {
            this.registryUrl = registryUrl;
        }

        public Duration getConnectionTimeout() {
            return connectionTimeout;
        }

        public void setConnectionTimeout(Duration connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
        }

        public Duration getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
        }

        public String getBaseImage() {
            return baseImage;
        }

        public void setBaseImage(String baseImage) {
            this.baseImage = baseImage;
        }

        public Build getBuild() {
            return build;
        }

        public void setBuild(Build build) {
            this.build = build;
        }

        public Image getImage() {
            return image;
        }

        public void setImage(Image image) {
            this.image = image;
        }

        public static class Build {
            private Duration timeout = Duration.ofSeconds(300);
            private int maxConcurrentBuilds = 5;
            private int retryAttempts = 3;
            private Duration retryDelay = Duration.ofSeconds(5);

            public Duration getTimeout() {
                return timeout;
            }

            public void setTimeout(Duration timeout) {
                this.timeout = timeout;
            }

            public int getMaxConcurrentBuilds() {
                return maxConcurrentBuilds;
            }

            public void setMaxConcurrentBuilds(int maxConcurrentBuilds) {
                this.maxConcurrentBuilds = maxConcurrentBuilds;
            }

            public int getRetryAttempts() {
                return retryAttempts;
            }

            public void setRetryAttempts(int retryAttempts) {
                this.retryAttempts = retryAttempts;
            }

            public Duration getRetryDelay() {
                return retryDelay;
            }

            public void setRetryDelay(Duration retryDelay) {
                this.retryDelay = retryDelay;
            }
        }

        public static class Image {
            private String registry = "localhost:5000";
            private String namespace = "agentic";
            private String tagPrefix = "";

            public String getRegistry() {
                return registry;
            }

            public void setRegistry(String registry) {
                this.registry = registry;
            }

            public String getNamespace() {
                return namespace;
            }

            public void setNamespace(String namespace) {
                this.namespace = namespace;
            }

            public String getTagPrefix() {
                return tagPrefix;
            }

            public void setTagPrefix(String tagPrefix) {
                this.tagPrefix = tagPrefix;
            }
        }
    }

    public static class Processing {
        @NestedConfigurationProperty
        private Status status = new Status();
        
        @NestedConfigurationProperty
        private Queue queue = new Queue();
        
        @NestedConfigurationProperty
        private Steps steps = new Steps();

        public Status getStatus() {
            return status;
        }

        public void setStatus(Status status) {
            this.status = status;
        }

        public Queue getQueue() {
            return queue;
        }

        public void setQueue(Queue queue) {
            this.queue = queue;
        }

        public Steps getSteps() {
            return steps;
        }

        public void setSteps(Steps steps) {
            this.steps = steps;
        }

        public static class Status {
            private int retentionDays = 30;
            private Duration cleanupInterval = Duration.ofHours(24);
            private Duration maxProcessingTime = Duration.ofSeconds(1800);

            public int getRetentionDays() {
                return retentionDays;
            }

            public void setRetentionDays(int retentionDays) {
                this.retentionDays = retentionDays;
            }

            public Duration getCleanupInterval() {
                return cleanupInterval;
            }

            public void setCleanupInterval(Duration cleanupInterval) {
                this.cleanupInterval = cleanupInterval;
            }

            public Duration getMaxProcessingTime() {
                return maxProcessingTime;
            }

            public void setMaxProcessingTime(Duration maxProcessingTime) {
                this.maxProcessingTime = maxProcessingTime;
            }
        }

        public static class Queue {
            private int maxSize = 1000;
            private Duration processingTimeout = Duration.ofSeconds(1800);
            private int maxConcurrentProcessing = 10;

            public int getMaxSize() {
                return maxSize;
            }

            public void setMaxSize(int maxSize) {
                this.maxSize = maxSize;
            }

            public Duration getProcessingTimeout() {
                return processingTimeout;
            }

            public void setProcessingTimeout(Duration processingTimeout) {
                this.processingTimeout = processingTimeout;
            }

            public int getMaxConcurrentProcessing() {
                return maxConcurrentProcessing;
            }

            public void setMaxConcurrentProcessing(int maxConcurrentProcessing) {
                this.maxConcurrentProcessing = maxConcurrentProcessing;
            }
        }

        public static class Steps {
            private boolean trackDetailedProgress = true;
            private boolean logStepDuration = true;
            private boolean enableStepRollback = true;

            public boolean isTrackDetailedProgress() {
                return trackDetailedProgress;
            }

            public void setTrackDetailedProgress(boolean trackDetailedProgress) {
                this.trackDetailedProgress = trackDetailedProgress;
            }

            public boolean isLogStepDuration() {
                return logStepDuration;
            }

            public void setLogStepDuration(boolean logStepDuration) {
                this.logStepDuration = logStepDuration;
            }

            public boolean isEnableStepRollback() {
                return enableStepRollback;
            }

            public void setEnableStepRollback(boolean enableStepRollback) {
                this.enableStepRollback = enableStepRollback;
            }
        }
    }
}