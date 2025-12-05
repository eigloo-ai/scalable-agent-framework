package ai.eigloo.agentic.graphbuilder.service;

import ai.eigloo.agentic.graph.model.Plan;
import ai.eigloo.agentic.graph.model.Task;
import ai.eigloo.agentic.graphbuilder.entity.DockerImageEntity;
import ai.eigloo.agentic.graphbuilder.exception.DockerBuildException;
import ai.eigloo.agentic.graphbuilder.repository.DockerImageRepository;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageCmd;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.model.BuildResponseItem;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service responsible for building and managing Docker images for task and plan executors.
 */
@Service
public class DockerImageService {
    
    private static final Logger logger = LoggerFactory.getLogger(DockerImageService.class);
    
    private final DockerImageRepository dockerImageRepository;
    private final DockerClient dockerClient;
    
    @Value("${graph-bundle.docker.base-image:services/executors-py:latest}")
    private String baseImage;
    
    @Value("${graph-bundle.docker.build-timeout:300}")
    private int buildTimeoutSeconds;
    
    @Autowired
    public DockerImageService(DockerImageRepository dockerImageRepository) {
        this.dockerImageRepository = dockerImageRepository;
        this.dockerClient = initializeDockerClient();
    }
    
    /**
     * Constructor for testing with mock Docker client.
     */
    public DockerImageService(DockerImageRepository dockerImageRepository, DockerClient dockerClient) {
        this.dockerImageRepository = dockerImageRepository;
        this.dockerClient = dockerClient;
    }
    
    /**
     * Builds a Docker image for a task executor.
     * 
     * @param tenantId The tenant ID
     * @param task The task to build an image for
     * @param taskDirectory The directory containing the task files
     * @param processId The process ID for tracking
     * @return The built image name
     * @throws DockerBuildException if build fails
     */
    public String buildTaskImage(String tenantId, Task task, Path taskDirectory, String processId) {
        String correlationId = getOrCreateCorrelationId();
        String imageName = generateImageName(tenantId, task.name(), "task");
        Path buildContext = null;
        
        try {
            logger.info("Building Docker image for task '{}' in tenant '{}' [correlationId={}]", task.name(), tenantId, correlationId);
            
            // Create build context directory
            buildContext = createBuildContext(taskDirectory, "task.py", processId);
            
            // Generate Dockerfile
            String dockerfile = generateTaskDockerfile(tenantId, task.name(), taskDirectory);
            Path dockerfilePath = buildContext.resolve("Dockerfile");
            Files.write(dockerfilePath, dockerfile.getBytes(StandardCharsets.UTF_8), 
                       StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            
            // Build the image
            String imageId = buildDockerImage(buildContext, imageName, processId);
            
            // Save Docker image record
            saveDockerImageRecord(processId, imageName, "TASK", task.name(), imageId);
            
            logger.info("Successfully built Docker image '{}' for task '{}' [correlationId={}]", imageName, task.name(), correlationId);
            return imageName;
            
        } catch (DockerBuildException e) {
            logger.error("Docker build failed for task '{}' [correlationId={}]: {}", task.name(), correlationId, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error building Docker image for task '{}' [correlationId={}]: {}", task.name(), correlationId, e.getMessage(), e);
            throw new DockerBuildException("Failed to build task image: " + e.getMessage(), e, correlationId, imageName);
        } finally {
            // Always cleanup build context
            if (buildContext != null) {
                cleanupBuildContext(buildContext);
            }
        }
    }
    
    /**
     * Builds a Docker image for a plan executor.
     * 
     * @param tenantId The tenant ID
     * @param plan The plan to build an image for
     * @param planDirectory The directory containing the plan files
     * @param processId The process ID for tracking
     * @return The built image name
     * @throws DockerBuildException if build fails
     */
    public String buildPlanImage(String tenantId, Plan plan, Path planDirectory, String processId) {
        String correlationId = getOrCreateCorrelationId();
        String imageName = generateImageName(tenantId, plan.name(), "plan");
        Path buildContext = null;
        
        try {
            logger.info("Building Docker image for plan '{}' in tenant '{}' [correlationId={}]", plan.name(), tenantId, correlationId);
            
            // Create build context directory
            buildContext = createBuildContext(planDirectory, "plan.py", processId);
            
            // Generate Dockerfile
            String dockerfile = generatePlanDockerfile(tenantId, plan.name(), planDirectory);
            Path dockerfilePath = buildContext.resolve("Dockerfile");
            Files.write(dockerfilePath, dockerfile.getBytes(StandardCharsets.UTF_8), 
                       StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            
            // Build the image
            String imageId = buildDockerImage(buildContext, imageName, processId);
            
            // Save Docker image record
            saveDockerImageRecord(processId, imageName, "PLAN", plan.name(), imageId);
            
            logger.info("Successfully built Docker image '{}' for plan '{}' [correlationId={}]", imageName, plan.name(), correlationId);
            return imageName;
            
        } catch (DockerBuildException e) {
            logger.error("Docker build failed for plan '{}' [correlationId={}]: {}", plan.name(), correlationId, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error building Docker image for plan '{}' [correlationId={}]: {}", plan.name(), correlationId, e.getMessage(), e);
            throw new DockerBuildException("Failed to build plan image: " + e.getMessage(), e, correlationId, imageName);
        } finally {
            // Always cleanup build context
            if (buildContext != null) {
                cleanupBuildContext(buildContext);
            }
        }
    }
    
    /**
     * Gets a list of built Docker images for a process.
     * 
     * @param processId The process ID
     * @return List of Docker image names
     */
    public List<String> getBuiltImages(String processId) {
        if (dockerImageRepository == null) {
            return Collections.emptyList();
        }
        
        return dockerImageRepository.findByProcessId(processId)
            .stream()
            .map(DockerImageEntity::getImageName)
            .collect(Collectors.toList());
    }
    
    /**
     * Cleans up Docker images for a failed process.
     * 
     * @param processId The process ID to clean up images for
     */
    public void cleanupDockerImages(String processId) {
        String correlationId = getOrCreateCorrelationId();
        
        if (dockerImageRepository == null || dockerClient == null) {
            logger.debug("Docker repository or client not available, skipping cleanup [correlationId={}]", correlationId);
            return;
        }
        
        try {
            logger.info("Starting Docker image cleanup for process {} [correlationId={}]", processId, correlationId);
            
            List<DockerImageEntity> images = dockerImageRepository.findByProcessId(processId);
            int cleanedCount = 0;
            
            for (DockerImageEntity imageEntity : images) {
                try {
                    // Remove the Docker image
                    dockerClient.removeImageCmd(imageEntity.getImageName())
                        .withForce(true)
                        .exec();
                    cleanedCount++;
                    logger.debug("Removed Docker image: {} [correlationId={}]", imageEntity.getImageName(), correlationId);
                } catch (Exception e) {
                    logger.warn("Failed to remove Docker image '{}' [correlationId={}]: {}", imageEntity.getImageName(), correlationId, e.getMessage());
                    // Continue with other images even if one fails
                }
            }
            
            // Remove database records
            try {
                dockerImageRepository.deleteByProcessId(processId);
                logger.info("Successfully cleaned up {} Docker images for process {} [correlationId={}]", cleanedCount, processId, correlationId);
            } catch (Exception e) {
                logger.error("Failed to remove Docker image database records for process {} [correlationId={}]: {}", processId, correlationId, e.getMessage(), e);
                // Don't throw exception for database cleanup failures
            }
            
        } catch (Exception e) {
            logger.error("Unexpected error during Docker image cleanup for process {} [correlationId={}]: {}", processId, correlationId, e.getMessage(), e);
            // Don't throw exception for cleanup failures - log and continue
        }
    }
    
    /**
     * Initializes the Docker client with proper configuration.
     * 
     * @return Configured Docker client
     * @throws DockerBuildException if initialization fails
     */
    private DockerClient initializeDockerClient() {
        String correlationId = getOrCreateCorrelationId();
        
        try {
            logger.info("Initializing Docker client [correlationId={}]", correlationId);
            
            DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("unix:///var/run/docker.sock")
                .build();
            
            ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();
            
            DockerClient client = DockerClientImpl.getInstance(config, httpClient);
            
            // Test the connection
            client.pingCmd().exec();
            logger.info("Docker client initialized successfully [correlationId={}]", correlationId);
            return client;
            
        } catch (Exception e) {
            logger.error("Failed to initialize Docker client [correlationId={}]: {}", correlationId, e.getMessage(), e);
            throw new DockerBuildException("Failed to initialize Docker client: " + e.getMessage(), e, correlationId, null);
        }
    }
    
    /**
     * Creates a build context directory with the necessary files.
     * 
     * @param sourceDirectory The source directory containing the executor files
     * @param executorFile The main executor file (task.py or plan.py)
     * @param processId The process ID for unique naming
     * @return Path to the build context directory
     * @throws DockerBuildException if context creation fails
     */
    private Path createBuildContext(Path sourceDirectory, String executorFile, String processId) {
        String correlationId = getOrCreateCorrelationId();
        
        try {
            Path buildContext = Files.createTempDirectory("docker-build-" + processId + "-");
            logger.debug("Created build context directory: {} [correlationId={}]", buildContext, correlationId);
            
            // Copy executor file
            Path sourceExecutorFile = sourceDirectory.resolve(executorFile);
            if (!Files.exists(sourceExecutorFile)) {
                throw new DockerBuildException("Executor file not found: " + sourceExecutorFile, correlationId, null);
            }
            Files.copy(sourceExecutorFile, buildContext.resolve(executorFile));
            
            // Copy requirements.txt if it exists
            Path requirementsFile = sourceDirectory.resolve("requirements.txt");
            if (Files.exists(requirementsFile)) {
                Files.copy(requirementsFile, buildContext.resolve("requirements.txt"));
                logger.debug("Copied requirements.txt to build context [correlationId={}]", correlationId);
            }
            
            logger.debug("Successfully created build context [correlationId={}]", correlationId);
            return buildContext;
            
        } catch (DockerBuildException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to create build context [correlationId={}]: {}", correlationId, e.getMessage(), e);
            throw new DockerBuildException("Failed to create build context: " + e.getMessage(), e, correlationId, null);
        }
    }
    
    /**
     * Builds a Docker image using the Docker client.
     * 
     * @param buildContext The build context directory
     * @param imageName The name for the built image
     * @param processId The process ID for tracking
     * @return The image ID of the built image
     * @throws DockerBuildException if build fails
     */
    private String buildDockerImage(Path buildContext, String imageName, String processId) {
        String correlationId = getOrCreateCorrelationId();
        
        try {
            logger.info("Building Docker image '{}' from context '{}' [correlationId={}]", imageName, buildContext, correlationId);
            
            BuildImageCmd buildCmd = dockerClient.buildImageCmd()
                .withDockerfile(buildContext.resolve("Dockerfile").toFile())
                .withBaseDirectory(buildContext.toFile())
                .withTags(Set.of(imageName))
                .withNoCache(false)
                .withRemove(true);
            
            StringBuilder buildLog = new StringBuilder();
            BuildImageResultCallback callback = new BuildImageResultCallback() {
                @Override
                public void onNext(BuildResponseItem item) {
                    if (item.getStream() != null) {
                        String logLine = item.getStream().trim();
                        buildLog.append(logLine).append("\n");
                        logger.debug("Docker build [correlationId={}]: {}", correlationId, logLine);
                    }
                    if (item.getErrorDetail() != null) {
                        String errorMsg = item.getErrorDetail().getMessage();
                        buildLog.append("ERROR: ").append(errorMsg).append("\n");
                        logger.error("Docker build error [correlationId={}]: {}", correlationId, errorMsg);
                    }
                    super.onNext(item);
                }
            };
            
            String imageId = buildCmd.exec(callback)
                .awaitImageId(buildTimeoutSeconds, TimeUnit.SECONDS);
            
            if (imageId == null) {
                String errorMsg = "Docker build failed - no image ID returned. Build log:\n" + buildLog.toString();
                throw new DockerBuildException(errorMsg, correlationId, imageName);
            }
            
            logger.info("Successfully built Docker image '{}' with ID '{}' [correlationId={}]", imageName, imageId, correlationId);
            return imageId;
            
        } catch (DockerBuildException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Docker build failed for image '{}' [correlationId={}]: {}", imageName, correlationId, e.getMessage(), e);
            throw new DockerBuildException("Docker build failed: " + e.getMessage(), e, correlationId, imageName);
        }
    }
    
    /**
     * Generates a standardized image name for executors.
     * 
     * @param tenantId The tenant ID
     * @param executorName The executor name
     * @param executorType The executor type (task or plan)
     * @return The generated image name
     */
    private String generateImageName(String tenantId, String executorName, String executorType) {
        // Format: tenant-{tenantId}-{executorType}-{executorName}:latest
        String sanitizedTenantId = tenantId.toLowerCase().replaceAll("[^a-z0-9]", "-");
        String sanitizedExecutorName = executorName.toLowerCase().replaceAll("[^a-z0-9]", "-");
        return String.format("tenant-%s-%s-%s:latest", sanitizedTenantId, executorType, sanitizedExecutorName);
    }
    
    /**
     * Saves a Docker image record to the database.
     * 
     * @param processId The process ID
     * @param imageName The image name
     * @param executorType The executor type
     * @param executorName The executor name
     * @param imageId The Docker image ID
     */
    private void saveDockerImageRecord(String processId, String imageName, String executorType, 
                                     String executorName, String imageId) {
        if (dockerImageRepository != null) {
            String recordId = UUID.randomUUID().toString();
            DockerImageEntity imageEntity = new DockerImageEntity(recordId, processId, imageName, executorType, executorName);
            imageEntity.setImageTag("latest");
            dockerImageRepository.save(imageEntity);
            logger.debug("Saved Docker image record for '{}' with ID '{}'", imageName, recordId);
        }
    }
    
    /**
     * Cleans up the build context directory.
     * 
     * @param buildContext The build context directory to clean up
     */
    private void cleanupBuildContext(Path buildContext) {
        String correlationId = getOrCreateCorrelationId();
        
        if (buildContext != null && Files.exists(buildContext)) {
            try {
                logger.debug("Starting cleanup of build context directory: {} [correlationId={}]", buildContext, correlationId);
                
                Files.walk(buildContext)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            logger.warn("Failed to delete build context file: {} [correlationId={}]", path, correlationId, e);
                        }
                    });
                    
                logger.debug("Successfully cleaned up build context directory: {} [correlationId={}]", buildContext, correlationId);
                
            } catch (IOException e) {
                logger.error("Failed to cleanup build context directory: {} [correlationId={}]", buildContext, correlationId, e);
                // Don't throw exception for cleanup failures - log and continue
            } catch (Exception e) {
                logger.error("Unexpected error during build context cleanup: {} [correlationId={}]", buildContext, correlationId, e);
                // Don't throw exception for cleanup failures - log and continue
            }
        } else {
            logger.debug("Build context directory does not exist or is null, skipping cleanup [correlationId={}]", correlationId);
        }
    }
    
    /**
     * Generates a Dockerfile for a task executor.
     * 
     * @param tenantId The tenant ID
     * @param taskName The task name
     * @param taskDirectory The task directory (for checking requirements.txt)
     * @return The generated Dockerfile content
     */
    private String generateTaskDockerfile(String tenantId, String taskName, Path taskDirectory) {
        StringBuilder dockerfile = new StringBuilder();
        
        dockerfile.append("FROM ").append(baseImage).append("\n\n");
        
        // Set build arguments
        dockerfile.append("ARG TENANT_ID=").append(tenantId).append("\n");
        dockerfile.append("ARG EXECUTOR_NAME=").append(taskName).append("\n");
        dockerfile.append("ARG EXECUTOR_PATH=/app/task.py").append("\n\n");
        
        // Set environment variables
        dockerfile.append("ENV TENANT_ID=${TENANT_ID}\n");
        dockerfile.append("ENV EXECUTOR_NAME=${EXECUTOR_NAME}\n");
        dockerfile.append("ENV EXECUTOR_PATH=${EXECUTOR_PATH}\n\n");
        
        // Copy requirements.txt if it exists
        if (Files.exists(taskDirectory.resolve("requirements.txt"))) {
            dockerfile.append("COPY requirements.txt /app/requirements.txt\n");
            dockerfile.append("RUN pip install --no-cache-dir -r /app/requirements.txt\n\n");
        }
        
        // Copy the task file
        dockerfile.append("COPY task.py /app/task.py\n\n");
        
        // Set working directory and default command
        dockerfile.append("WORKDIR /app\n");
        dockerfile.append("CMD [\"python\", \"/app/task.py\"]\n");
        
        return dockerfile.toString();
    }
    
    /**
     * Generates a Dockerfile for a plan executor.
     * 
     * @param tenantId The tenant ID
     * @param planName The plan name
     * @param planDirectory The plan directory (for checking requirements.txt)
     * @return The generated Dockerfile content
     */
    private String generatePlanDockerfile(String tenantId, String planName, Path planDirectory) {
        StringBuilder dockerfile = new StringBuilder();
        
        dockerfile.append("FROM ").append(baseImage).append("\n\n");
        
        // Set build arguments
        dockerfile.append("ARG TENANT_ID=").append(tenantId).append("\n");
        dockerfile.append("ARG EXECUTOR_NAME=").append(planName).append("\n");
        dockerfile.append("ARG EXECUTOR_PATH=/app/plan.py").append("\n\n");
        
        // Set environment variables
        dockerfile.append("ENV TENANT_ID=${TENANT_ID}\n");
        dockerfile.append("ENV EXECUTOR_NAME=${EXECUTOR_NAME}\n");
        dockerfile.append("ENV EXECUTOR_PATH=${EXECUTOR_PATH}\n\n");
        
        // Copy requirements.txt if it exists
        if (Files.exists(planDirectory.resolve("requirements.txt"))) {
            dockerfile.append("COPY requirements.txt /app/requirements.txt\n");
            dockerfile.append("RUN pip install --no-cache-dir -r /app/requirements.txt\n\n");
        }
        
        // Copy the plan file
        dockerfile.append("COPY plan.py /app/plan.py\n\n");
        
        // Set working directory and default command
        dockerfile.append("WORKDIR /app\n");
        dockerfile.append("CMD [\"python\", \"/app/plan.py\"]\n");
        
        return dockerfile.toString();
    }
    
    /**
     * Gets or creates a correlation ID for error tracking.
     */
    private String getOrCreateCorrelationId() {
        String mdcId = MDC.get("correlationId");
        if (mdcId != null && !mdcId.trim().isEmpty()) {
            return mdcId;
        }
        
        String newId = UUID.randomUUID().toString();
        MDC.put("correlationId", newId);
        return newId;
    }
}