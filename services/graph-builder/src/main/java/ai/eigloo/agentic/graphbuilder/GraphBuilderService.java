package ai.eigloo.agentic.graphbuilder;

import ai.eigloo.agentic.graph.model.AgentGraph;
import ai.eigloo.agentic.graph.model.Plan;
import ai.eigloo.agentic.graph.model.Task;
import ai.eigloo.agentic.graph.exception.GraphParsingException;
import ai.eigloo.agentic.graph.exception.GraphValidationException;
import ai.eigloo.agentic.graphbuilder.dto.ProcessingStatus;
import ai.eigloo.agentic.graphbuilder.enums.BundleStatus;
import ai.eigloo.agentic.graphbuilder.enums.StepStatus;
import ai.eigloo.agentic.graphbuilder.exception.BundleProcessingException;
import ai.eigloo.agentic.graphbuilder.exception.DockerBuildException;
import ai.eigloo.agentic.graph.exception.GraphPersistenceException;
import ai.eigloo.agentic.graphbuilder.parser.GraphParser;
import ai.eigloo.agentic.graphbuilder.parser.GraphVizDotParser;
import ai.eigloo.agentic.graphbuilder.service.BundleProcessingService;
import ai.eigloo.agentic.graphbuilder.service.CleanupService;
import ai.eigloo.agentic.graphbuilder.service.DockerImageService;
import ai.eigloo.agentic.graph.service.GraphPersistenceService;
import ai.eigloo.agentic.graphbuilder.service.ProcessingStatusService;
import ai.eigloo.agentic.graphbuilder.validation.PythonCodeBundleValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Facade service for building agent graphs from specifications.
 * 
 * <p>This service orchestrates the parsing and validation process for agent graph
 * specifications. It discovers available parsers, selects the appropriate parser
 * for a given specification file, and delegates the parsing and validation work.</p>
 * 
 * <p>The service supports multiple graph specification formats and can be easily
 * extended with new parsers by implementing the GraphParser interface.</p>
 * 
 * <p>For bundle processing, this service orchestrates the work of specialized services:
 * BundleProcessingService, DockerImageService, GraphPersistenceService, and ProcessingStatusService.</p>
 */
@Service
public class GraphBuilderService {
    
    private static final Logger logger = LoggerFactory.getLogger(GraphBuilderService.class);
    
    private final List<GraphParser> parsers;
    private final BundleProcessingService bundleProcessingService;
    private final DockerImageService dockerImageService;
    private final GraphPersistenceService graphPersistenceService;
    private final ProcessingStatusService processingStatusService;
    private final CleanupService cleanupService;
    
    /**
     * Creates a new GraphBuilderService with the default parsers (for testing).
     */
    public GraphBuilderService() {
        this.parsers = new ArrayList<>();
        this.parsers.add(new GraphVizDotParser());
        this.bundleProcessingService = null;
        this.dockerImageService = null;
        this.graphPersistenceService = null;
        this.processingStatusService = null;
        this.cleanupService = null;
        logger.info("Initialized GraphBuilderService with {} parsers (test mode)", parsers.size());
    }
    
    /**
     * Creates a new GraphBuilderService with the default parsers and services.
     */
    @Autowired
    public GraphBuilderService(BundleProcessingService bundleProcessingService,
                              DockerImageService dockerImageService,
                              GraphPersistenceService graphPersistenceService,
                              ProcessingStatusService processingStatusService,
                              CleanupService cleanupService) {
        this.parsers = new ArrayList<>();
        this.parsers.add(new GraphVizDotParser());
        this.bundleProcessingService = bundleProcessingService;
        this.dockerImageService = dockerImageService;
        this.graphPersistenceService = graphPersistenceService;
        this.processingStatusService = processingStatusService;
        this.cleanupService = cleanupService;
        logger.info("Initialized GraphBuilderService with {} parsers", parsers.size());
    }
    
    /**
     * Creates a new GraphBuilderService with the specified parsers and services.
     * 
     * @param parsers The parsers to use
     * @param bundleProcessingService Service for bundle processing operations
     * @param dockerImageService Service for Docker image operations
     * @param graphPersistenceService Service for graph persistence operations
     * @param processingStatusService Service for processing status tracking
     * @param cleanupService Service for cleanup operations
     */
    public GraphBuilderService(List<GraphParser> parsers,
                              BundleProcessingService bundleProcessingService,
                              DockerImageService dockerImageService,
                              GraphPersistenceService graphPersistenceService,
                              ProcessingStatusService processingStatusService,
                              CleanupService cleanupService) {
        this.parsers = new ArrayList<>(parsers);
        this.bundleProcessingService = bundleProcessingService;
        this.dockerImageService = dockerImageService;
        this.graphPersistenceService = graphPersistenceService;
        this.processingStatusService = processingStatusService;
        this.cleanupService = cleanupService;
        logger.info("Initialized GraphBuilderService with {} parsers", parsers.size());
    }
    
    /**
     * Builds an agent graph from a specification directory.
     * 
     * <p>This method:</p>
     * <ol>
     *   <li>Discovers the specification file in the directory</li>
     *   <li>Selects the appropriate parser for the file format</li>
     *   <li>Delegates parsing to the selected parser</li>
     *   <li>Returns the validated AgentGraph</li>
     * </ol>
     * 
     * @param specificationDirectory The directory containing the graph specification
     * @return A fully constructed and validated AgentGraph
     * @throws GraphParsingException if the specification cannot be parsed
     * @throws GraphValidationException if the graph violates integrity constraints
     * @throws IllegalArgumentException if the specification directory is invalid
     */
    public AgentGraph buildGraph(Path specificationDirectory) throws GraphParsingException, GraphValidationException {
        logger.info("Building agent graph from specification directory: {}", specificationDirectory);
        
        // Validate input
        if (specificationDirectory == null) {
            throw new IllegalArgumentException("Specification directory cannot be null");
        }
        
        if (!Files.exists(specificationDirectory)) {
            throw new IllegalArgumentException("Specification directory does not exist: " + specificationDirectory);
        }
        
        if (!Files.isDirectory(specificationDirectory)) {
            throw new IllegalArgumentException("Specification path is not a directory: " + specificationDirectory);
        }
        
        // Find the specification file
        Path specificationFile = findSpecificationFile(specificationDirectory);
        logger.debug("Found specification file: {}", specificationFile);
        
        // Find the appropriate parser
        GraphParser parser = findParser(specificationFile);
        if (parser == null) {
            throw GraphParsingException.parsingError(
                "No parser found for specification file: " + specificationFile.getFileName());
        }
        
        logger.info("Using parser: {} for file: {}", parser.getName(), specificationFile.getFileName());
        
        // Parse the graph
        try {
            AgentGraph graph = parser.parse(specificationDirectory);
            
            // Validate Python code bundles
            PythonCodeBundleValidator.validate(graph, specificationDirectory);
            
            logger.info("Successfully built agent graph with {} plans and {} tasks", 
                graph.planCount(), graph.taskCount());
            return graph;
        } catch (GraphParsingException e) {
            logger.error("Failed to parse graph specification: {}", e.getMessage());
            throw e;
        } catch (GraphValidationException e) {
            logger.error("Graph validation failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error building graph: {}", e.getMessage(), e);
            throw GraphParsingException.parsingError(
                "Unexpected error building graph: " + e.getMessage(), e);
        }
    }
    
    /**
     * Gets a list of supported file formats.
     * 
     * @return List of supported file extensions (e.g., [".dot", ".json"])
     */
    public List<String> getSupportedFormats() {
        return parsers.stream()
            .flatMap(parser -> Arrays.stream(parser.getSupportedExtensions()))
            .distinct()
            .collect(Collectors.toList());
    }
    
    /**
     * Gets information about available parsers.
     * 
     * @return List of parser information
     */
    public List<ParserInfo> getAvailableParsers() {
        return parsers.stream()
            .map(parser -> new ParserInfo(
                parser.getName(),
                Arrays.asList(parser.getSupportedExtensions())))
            .collect(Collectors.toList());
    }
    
    /**
     * Adds a new parser to the service.
     * 
     * @param parser The parser to add
     */
    public void addParser(GraphParser parser) {
        if (parser != null) {
            parsers.add(parser);
            logger.info("Added parser: {}", parser.getName());
        }
    }
    
    /**
     * Processes an uploaded bundle file asynchronously.
     * 
     * @param tenantId The tenant ID for the upload
     * @param file The uploaded multipart file
     * @return The process ID for tracking
     * @throws BundleProcessingException if processing fails
     */
    public java.util.concurrent.CompletableFuture<String> processBundle(String tenantId, MultipartFile file) {
        String processId = UUID.randomUUID().toString();
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);

        logger.info("Received bundle upload for tenant {}. Assigning process ID {} [correlationId={}]", tenantId, processId, correlationId);

        try {
            // Early validation of tenantId
            if (tenantId == null || tenantId.isBlank()) {
                throw new BundleProcessingException("Tenant ID cannot be null", correlationId, "VALIDATION");
            }

            // Early null check for file to handle tests that mock the service and skip validation
            if (file == null) {
                throw new BundleProcessingException("File cannot be null", correlationId, "VALIDATION");
            }

            // Quick validation to fail fast (also checks null/empty file)
            bundleProcessingService.validateFile(file);

            // Create bundle entity and initial processing step
            processingStatusService.createBundleEntity(tenantId, file.getOriginalFilename(), processId);
            processingStatusService.createProcessingStep(processId, "UPLOAD", StepStatus.COMPLETED);
            processingStatusService.updateBundleStatus(processId, BundleStatus.UPLOADED);

            // Continue processing asynchronously
            continueBundleProcessing(tenantId, file, processId, correlationId);

            // Return processId immediately (completed future)
            return java.util.concurrent.CompletableFuture.completedFuture(processId);

        } catch (BundleProcessingException e) {
            logger.error("Bundle validation failed for process {} [correlationId={}]: {}", processId, correlationId, e.getMessage());
            // Perform cleanup and mark failed, then still return the processId
            String failedStep = normalizeStepName(e.getStep() != null ? e.getStep() : "VALIDATION");
            performComprehensiveCleanup(processId, null, failedStep, e.getMessage(), null, tenantId);
            return java.util.concurrent.CompletableFuture.completedFuture(processId);
        } catch (Exception e) {
            logger.error("Unexpected error preparing processing for process {} [correlationId={}]: {}", processId, correlationId, e.getMessage(), e);
            performComprehensiveCleanup(processId, null, "UPLOAD", e.getMessage(), null, tenantId);
            return java.util.concurrent.CompletableFuture.completedFuture(processId);
        } finally {
            MDC.remove("correlationId");
        }
    }

    /**
     * Continues the bundle processing asynchronously after initial validation and entity creation.
     */
    @Async
    public void continueBundleProcessing(String tenantId, MultipartFile file, String processId, String correlationId) {
        MDC.put("correlationId", correlationId);
        Path extractDir = null;
        try {
            // Step 2: Extract bundle
            logger.debug("Step 2: Extracting bundle [correlationId={}]: processId={}", correlationId, processId);
            processingStatusService.createProcessingStep(processId, "EXTRACTING", StepStatus.IN_PROGRESS);
            extractDir = bundleProcessingService.extractBundle(file, processId);
            processingStatusService.updateProcessingStep(processId, "EXTRACTING", StepStatus.COMPLETED);
            processingStatusService.updateBundleStatus(processId, BundleStatus.EXTRACTING);

            // Step 3: Parse graph
            logger.debug("Step 3: Parsing graph [correlationId={}]: processId={}", correlationId, processId);
            processingStatusService.createProcessingStep(processId, "PARSING", StepStatus.IN_PROGRESS);
            AgentGraph graph;
            try {
                graph = buildGraph(extractDir);
                logger.info("Successfully parsed graph with {} plans and {} tasks for process {} [correlationId={}]", 
                    graph.planCount(), graph.taskCount(), processId, correlationId);
            } catch (GraphParsingException e) {
                logger.error("Graph parsing failed for process {} [correlationId={}]: {}", processId, correlationId, e.getMessage());
                processingStatusService.updateProcessingStep(processId, "PARSING", StepStatus.FAILED, e.getMessage());
                throw e;
            } catch (GraphValidationException e) {
                logger.error("Graph validation failed for process {} [correlationId={}]: {}", processId, correlationId, e.getMessage());
                processingStatusService.updateProcessingStep(processId, "PARSING", StepStatus.FAILED, e.getMessage());
                throw e;
            }
            processingStatusService.updateProcessingStep(processId, "PARSING", StepStatus.COMPLETED);
            processingStatusService.updateBundleStatus(processId, BundleStatus.PARSING);

            // Step 4: Persist graph
            logger.debug("Step 4: Persisting graph [correlationId={}]: processId={}", correlationId, processId);
            processingStatusService.createProcessingStep(processId, "PERSISTING", StepStatus.IN_PROGRESS);
            graphPersistenceService.persistGraph(graph, tenantId);
            processingStatusService.updateProcessingStep(processId, "PERSISTING", StepStatus.COMPLETED);
            processingStatusService.updateBundleStatus(processId, BundleStatus.PERSISTING);

            // Step 5: Build Docker images
            logger.debug("Step 5: Building Docker images [correlationId={}]: processId={}", correlationId, processId);
            processingStatusService.createProcessingStep(processId, "BUILDING_IMAGES", StepStatus.IN_PROGRESS);
            buildDockerImages(tenantId, graph, extractDir, processId);
            processingStatusService.updateProcessingStep(processId, "BUILDING_IMAGES", StepStatus.COMPLETED);

            // Completed
            processingStatusService.markProcessAsCompleted(processId);
            logger.info("Bundle processing completed successfully for process ID {} [correlationId={}]", processId, correlationId);
            // Cleanup extraction directory after successful processing
            if (extractDir != null) {
                try {
                    bundleProcessingService.cleanupTempDirectory(extractDir);
                } catch (Exception e) {
                    logger.warn("Failed to cleanup temporary directory after success for process {} [correlationId={}]: {}", processId, correlationId, e.getMessage());
                }
            }

        } catch (BundleProcessingException e) {
            logger.error("Bundle processing failed for process ID {} [correlationId={}]: {}", processId, correlationId, e.getMessage());
            String failedStep = normalizeStepName(e.getStep() != null ? e.getStep() : "UNKNOWN");
            performComprehensiveCleanup(processId, extractDir, failedStep, e.getMessage(), null, tenantId);
        } catch (GraphParsingException | GraphValidationException e) {
            logger.error("Graph processing failed for process ID {} [correlationId={}]: {}", processId, correlationId, e.getMessage());
            performComprehensiveCleanup(processId, extractDir, "PARSING", e.getMessage(), null, tenantId);
        } catch (GraphPersistenceException e) {
            logger.error("Graph persistence failed for process ID {} [correlationId={}]: {}", processId, correlationId, e.getMessage());
            String graphName = e.getGraphName();
            performComprehensiveCleanup(processId, extractDir, "PERSISTING", e.getMessage(), graphName, tenantId);
        } catch (DockerBuildException e) {
            logger.error("Docker build failed for process ID {} [correlationId={}]: {}", processId, correlationId, e.getMessage());
            performComprehensiveCleanup(processId, extractDir, "BUILDING_IMAGES", e.getMessage(), null, tenantId);
        } catch (Exception e) {
            logger.error("Unexpected error during bundle processing for process ID {} [correlationId={}]: {}", processId, correlationId, e.getMessage(), e);
            String failedStep = determineFailedStep(e);
            performComprehensiveCleanup(processId, extractDir, failedStep, e.getMessage(), null, tenantId);
        } finally {
            MDC.remove("correlationId");
        }
    }
    
    /**
     * Gets the processing status for a given process ID.
     * 
     * @param processId The process ID to query
     * @return The processing status
     */
    public ProcessingStatus getProcessingStatus(String processId) {
        return processingStatusService.getProcessingStatus(processId);
    }
    
    /**
     * Retrieves a persisted AgentGraph by ID.
     * 
     * @param graphId The graph ID
     * @return The AgentGraph
     */
    public AgentGraph getPersistedGraph(String graphId) {
        return graphPersistenceService.getPersistedGraph(graphId);
    }
    
    /**
     * Lists all persisted graphs for a tenant.
     * 
     * @param tenantId The tenant ID
     * @return List of graph information
     */
    public List<GraphPersistenceService.GraphInfo> listPersistedGraphs(String tenantId) {
        return graphPersistenceService.listPersistedGraphs(tenantId);
    }
    
    /**
     * Builds Docker images for all tasks and plans in the graph.
     * 
     * @param tenantId The tenant ID
     * @param graph The agent graph
     * @param extractDir The extraction directory containing task and plan files
     * @param processId The process ID for tracking
     * @throws DockerBuildException if image building fails
     */
    private void buildDockerImages(String tenantId, AgentGraph graph, Path extractDir, String processId) {
        String correlationId = MDC.get("correlationId");
        
        try {
            logger.info("Building Docker images for {} tasks and {} plans [correlationId={}]", 
                graph.taskCount(), graph.planCount(), correlationId);
            
            // Build task images
            Path tasksDir = extractDir.resolve("tasks");
            if (Files.exists(tasksDir) && Files.isDirectory(tasksDir)) {
                for (Task task : graph.tasks().values()) {
                    Path taskDir = tasksDir.resolve(task.name());
                    if (Files.exists(taskDir)) {
                        try {
                            logger.debug("Building Docker image for task '{}' [correlationId={}]", task.name(), correlationId);
                            dockerImageService.buildTaskImage(tenantId, task, taskDir, processId);
                        } catch (Exception e) {
                            logger.error("Failed to build Docker image for task '{}' [correlationId={}]: {}", task.name(), correlationId, e.getMessage(), e);
                            throw new DockerBuildException("Failed to build image for task '" + task.name() + "': " + e.getMessage(), e, correlationId, task.name());
                        }
                    } else {
                        logger.warn("Task directory not found for task '{}' [correlationId={}]", task.name(), correlationId);
                    }
                }
            }
            
            // Build plan images
            Path plansDir = extractDir.resolve("plans");
            if (Files.exists(plansDir) && Files.isDirectory(plansDir)) {
                for (Plan plan : graph.plans().values()) {
                    Path planDir = plansDir.resolve(plan.name());
                    if (Files.exists(planDir)) {
                        try {
                            logger.debug("Building Docker image for plan '{}' [correlationId={}]", plan.name(), correlationId);
                            dockerImageService.buildPlanImage(tenantId, plan, planDir, processId);
                        } catch (Exception e) {
                            logger.error("Failed to build Docker image for plan '{}' [correlationId={}]: {}", plan.name(), correlationId, e.getMessage(), e);
                            throw new DockerBuildException("Failed to build image for plan '" + plan.name() + "': " + e.getMessage(), e, correlationId, plan.name());
                        }
                    } else {
                        logger.warn("Plan directory not found for plan '{}' [correlationId={}]", plan.name(), correlationId);
                    }
                }
            }
            
            logger.info("Successfully built all Docker images [correlationId={}]", correlationId);
            
        } catch (DockerBuildException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during Docker image building [correlationId={}]: {}", correlationId, e.getMessage(), e);
            throw new DockerBuildException("Unexpected error during Docker image building: " + e.getMessage(), e, correlationId, null);
        }
    }
    
    /**
     * Performs comprehensive cleanup operations on failure.
     * 
     * @param processId The process ID
     * @param extractDir The extraction directory to clean up
     * @param failedStep The step that failed
     * @param errorMessage The error message
     * @param graphName The graph name (for rollback operations)
     * @param tenantId The tenant ID
     */
    private void performComprehensiveCleanup(String processId, Path extractDir, String failedStep, 
                                           String errorMessage, String graphName, String tenantId) {
        String correlationId = MDC.get("correlationId");
        
        try {
            logger.info("Starting comprehensive cleanup for process {} [correlationId={}]", processId, correlationId);
            
            if (cleanupService != null) {
                // Always perform comprehensive cleanup
                CleanupService.CleanupLevel cleanupLevel = determineCleanupLevel(failedStep);
                cleanupService.performComprehensiveCleanup(processId, extractDir, cleanupLevel);
                // Always cleanup temp directory
                if (extractDir != null) {
                    try {
                        bundleProcessingService.cleanupTempDirectory(extractDir);
                    } catch (Exception e) {
                        logger.warn("Failed to cleanup temporary directory during comprehensive cleanup for process {} [correlationId={}]: {}", processId, correlationId, e.getMessage());
                    }
                }
                
                // Mark process as failed if not already done
                if (processingStatusService != null) {
                    try {
                        processingStatusService.markProcessAsFailed(processId, normalizeStepName(failedStep), errorMessage);
                    } catch (Exception e) {
                        logger.warn("Failed to mark process as failed during cleanup [correlationId={}]: {}", correlationId, e.getMessage());
                    }
                }
            } else {
                // Fallback to basic cleanup if CleanupService is not available
                performBasicCleanup(processId, extractDir, correlationId);
            }
            
            logger.info("Comprehensive cleanup completed for process {} [correlationId={}]", processId, correlationId);
            
        } catch (Exception e) {
            logger.error("Error during comprehensive cleanup for process {} [correlationId={}]: {}", processId, correlationId, e.getMessage(), e);
            // Don't throw exception for cleanup failures
        }
    }
    
    /**
     * Performs basic cleanup operations as fallback.
     */
    private void performBasicCleanup(String processId, Path extractDir, String correlationId) {
        // Cleanup Docker images
        if (dockerImageService != null) {
            try {
                dockerImageService.cleanupDockerImages(processId);
            } catch (Exception e) {
                logger.warn("Failed to cleanup Docker images for process {} [correlationId={}]: {}", processId, correlationId, e.getMessage());
            }
        }
        
        // Cleanup temporary directory
        if (extractDir != null && bundleProcessingService != null) {
            try {
                bundleProcessingService.cleanupTempDirectory(extractDir);
            } catch (Exception e) {
                logger.warn("Failed to cleanup temporary directory for process {} [correlationId={}]: {}", processId, correlationId, e.getMessage());
            }
        }
    }
    
    /**
     * Determines the appropriate cleanup level based on the failed step.
     */
    private CleanupService.CleanupLevel determineCleanupLevel(String failedStep) {
        // For early failures (validation, extraction), use full cleanup
        if ("VALIDATION".equals(failedStep) || "EXTRACTING".equals(failedStep)) {
            return CleanupService.CleanupLevel.FULL;
        }
        
        // For later failures, keep records for debugging
        return CleanupService.CleanupLevel.PARTIAL;
    }
    
    /**
     * Determines which step failed based on the exception type.
     * 
     * @param e The exception that occurred
     * @return The name of the failed step
     */
    private String determineFailedStep(Exception e) {
        if (e instanceof BundleProcessingException) {
            BundleProcessingException bpe = (BundleProcessingException) e;
            return normalizeStepName(bpe.getStep() != null ? bpe.getStep() : "UNKNOWN");
        } else if (e instanceof IllegalArgumentException) {
            return "UPLOAD";
        } else if (e instanceof IOException) {
            return "EXTRACTING";
        } else if (e instanceof GraphParsingException || e instanceof GraphValidationException) {
            return "PARSING";
        } else if (e instanceof GraphPersistenceException || (e.getMessage() != null && e.getMessage().contains("persist"))) {
            return "PERSISTING";
        } else if (e instanceof DockerBuildException || (e.getMessage() != null && e.getMessage().contains("Docker"))) {
            return "BUILDING_IMAGES";
        } else {
            return "UNKNOWN";
        }
    }

    /**
     * Normalizes internal step names to the canonical names expected by tests and UI.
     */
    private String normalizeStepName(String step) {
        if (step == null) return null;
        if ("EXTRACTION".equals(step)) return "EXTRACTING";
        return step;
    }
    
    private Path findSpecificationFile(Path specificationDirectory) throws GraphParsingException {
        try {
            return Files.list(specificationDirectory)
                .filter(Files::isRegularFile)
                .filter(this::isSupportedFile)
                .findFirst()
                .orElseThrow(() -> GraphParsingException.parsingError(
                    "No supported specification file found in directory: " + specificationDirectory));
        } catch (Exception e) {
            throw GraphParsingException.parsingError(
                "Error searching for specification file in directory: " + specificationDirectory, e);
        }
    }
    
    private boolean isSupportedFile(Path file) {
        return parsers.stream().anyMatch(parser -> parser.supports(file));
    }
    
    private GraphParser findParser(Path specificationFile) {
        return parsers.stream()
            .filter(parser -> parser.supports(specificationFile))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Information about a graph parser.
     */
    public static class ParserInfo {
        private final String name;
        private final List<String> supportedExtensions;
        
        public ParserInfo(String name, List<String> supportedExtensions) {
            this.name = name;
            this.supportedExtensions = supportedExtensions;
        }
        
        public String getName() {
            return name;
        }
        
        public List<String> getSupportedExtensions() {
            return supportedExtensions;
        }
        
        @Override
        public String toString() {
            return "ParserInfo{" +
                "name='" + name + '\'' +
                ", supportedExtensions=" + supportedExtensions +
                '}';
        }
    }
}