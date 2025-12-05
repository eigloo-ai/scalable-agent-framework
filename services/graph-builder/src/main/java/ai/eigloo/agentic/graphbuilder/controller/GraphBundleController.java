package ai.eigloo.agentic.graphbuilder.controller;

import ai.eigloo.agentic.graphbuilder.GraphBuilderService;
import ai.eigloo.agentic.graphbuilder.dto.BundleInfo;
import ai.eigloo.agentic.graphbuilder.dto.BundleUploadResponse;
import ai.eigloo.agentic.graphbuilder.dto.ProcessingStatus;
import ai.eigloo.agentic.graphbuilder.entity.GraphBundleEntity;
import ai.eigloo.agentic.graphbuilder.repository.GraphBundleRepository;
import ai.eigloo.agentic.graphbuilder.config.GraphBundleProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API controller for graph bundle upload and management operations.
 * Provides endpoints for uploading compressed graph bundles, checking processing status,
 * and listing uploaded bundles.
 */
@RestController
@RequestMapping("/api/graph-bundle")
public class GraphBundleController {
    
    private static final Logger logger = LoggerFactory.getLogger(GraphBundleController.class);
    
    private final GraphBuilderService graphBuilderService;
    private final GraphBundleRepository graphBundleRepository;
    private final GraphBundleProperties properties;
    
    @Autowired
    public GraphBundleController(GraphBuilderService graphBuilderService,
                                GraphBundleRepository graphBundleRepository,
                                GraphBundleProperties properties) {
        this.graphBuilderService = graphBuilderService;
        this.graphBundleRepository = graphBundleRepository;
        this.properties = properties;
    }
    
    /**
     * Upload a compressed graph bundle for processing.
     * 
     * @param tenantId The tenant ID (required)
     * @param file The uploaded file (tar, tar.gz, or zip)
     * @return Response containing process ID and status
     */
    @PostMapping("/upload")
    public ResponseEntity<BundleUploadResponse> uploadBundle(
            @RequestParam("tenantId") @NotBlank(message = "Tenant ID is required") String tenantId,
            @RequestParam("file") MultipartFile file) {
        
        String correlationId = java.util.UUID.randomUUID().toString();
        org.slf4j.MDC.put("correlationId", correlationId);
        
        try {
            logger.info("Received bundle upload request for tenant: {}, file: {} [correlationId={}]", 
                tenantId, file.getOriginalFilename(), correlationId);
            
            // Validate tenant ID
            if (tenantId == null || tenantId.trim().isEmpty()) {
                logger.warn("Upload rejected: Missing or empty tenant ID [correlationId={}]", correlationId);
                return ResponseEntity.badRequest()
                    .header("X-Correlation-ID", correlationId)
                    .body(new BundleUploadResponse(null, "FAILED", "Tenant ID cannot be null or empty"));
            }
            
            // Validate file
            if (file == null || file.isEmpty()) {
                logger.warn("Upload rejected: No file provided [correlationId={}]", correlationId);
                return ResponseEntity.badRequest()
                    .header("X-Correlation-ID", correlationId)
                    .body(new BundleUploadResponse(null, "FAILED", "File cannot be null or empty"));
            }
            
            // Validate file size using properties
            long maxBytes = parseSizeToBytes(properties.getUpload().getMaxFileSize());
            if (file.getSize() > maxBytes) {
                logger.warn("Upload rejected: File size {} exceeds maximum {} [correlationId={}]", 
                    file.getSize(), maxBytes, correlationId);
                return ResponseEntity.badRequest()
                    .header("X-Correlation-ID", correlationId)
                    .body(new BundleUploadResponse(null, "FAILED", 
                        String.format("File size exceeds maximum allowed size of %d MB", maxBytes / (1024 * 1024))));
            }
            
            // Validate file extension
            String filename = file.getOriginalFilename();
            if (filename == null || !isValidFileExtension(filename)) {
                logger.warn("Upload rejected: Invalid file extension for file: {} [correlationId={}]", filename, correlationId);
                return ResponseEntity.badRequest()
                    .header("X-Correlation-ID", correlationId)
                    .body(new BundleUploadResponse(null, "FAILED", 
                        "File must have one of the following extensions: " + String.join(", ", properties.getUpload().getAllowedExtensions())));
            }
            
            // Process the bundle asynchronously
            java.util.concurrent.CompletableFuture<String> processIdFuture = graphBuilderService.processBundle(tenantId, file);
            String processId = processIdFuture.join();
            
            logger.info("Bundle upload accepted for processing with ID: {} [correlationId={}]", processId, correlationId);
            return ResponseEntity.status(HttpStatus.CREATED)
                .header("X-Correlation-ID", correlationId)
                .body(new BundleUploadResponse(processId, "UPLOADED", "Upload successful: processing started"));
                
        } catch (Exception e) {
            // Let the GlobalExceptionHandler handle the exception
            // It will add the correlation ID to the response
            throw e;
        } finally {
            org.slf4j.MDC.remove("correlationId");
        }
    }
    
    /**
     * Get the processing status of an uploaded bundle.
     * 
     * @param processId The process ID to query
     * @return Processing status with step details
     */
    @GetMapping("/status/{processId}")
    public ResponseEntity<?> getProcessingStatus(@PathVariable String processId) {
        
        String correlationId = java.util.UUID.randomUUID().toString();
        org.slf4j.MDC.put("correlationId", correlationId);
        
        try {
            logger.debug("Received status request for process ID: {} [correlationId={}]", processId, correlationId);
            
            if (processId == null || processId.trim().isEmpty()) {
                logger.warn("Status request rejected: Missing process ID [correlationId={}]", correlationId);
                return ResponseEntity.badRequest()
                    .header("X-Correlation-ID", correlationId)
                    .body("Process ID is required");
            }
            
            try {
                ProcessingStatus status = graphBuilderService.getProcessingStatus(processId);
                logger.debug("Returning status for process ID {}: {} [correlationId={}]", processId, status.getStatus(), correlationId);
                return ResponseEntity.ok()
                    .header("X-Correlation-ID", correlationId)
                    .body(status);
            } catch (IllegalArgumentException e) {
                logger.warn("Status request rejected: {} [correlationId={}]", e.getMessage(), correlationId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .header("X-Correlation-ID", correlationId)
                    .body("Process ID not found");
            }
            
        } catch (Exception e) {
            logger.error("Error retrieving status for process ID {} [correlationId={}]: {}", processId, correlationId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("X-Correlation-ID", correlationId)
                .body("Internal error fetching status");
        } finally {
            org.slf4j.MDC.remove("correlationId");
        }
    }
    
    /**
     * List all uploaded bundles for a tenant.
     * 
     * @param tenantId The tenant ID to filter by (optional)
     * @return List of bundle information
     */
    @GetMapping("/list")
    public ResponseEntity<?> listBundles(
            @RequestParam(value = "tenantId", required = false) String tenantId) {
        
        logger.debug("Received bundle list request for tenant: {}", tenantId);
        
        try {
            // Validate tenantId
            if (tenantId == null) {
                logger.warn("Bundle list request rejected: Missing tenant ID");
                return ResponseEntity.badRequest().body("Tenant ID is required");
            }
            if (tenantId.trim().isEmpty()) {
                logger.warn("Bundle list request rejected: Empty tenant ID");
                return ResponseEntity.badRequest().body("Tenant ID cannot be empty");
            }

            List<GraphBundleEntity> bundles = graphBundleRepository.findByTenantIdOrderByUploadTimeDesc(tenantId);
            logger.debug("Found {} bundles for tenant: {}", bundles.size(), tenantId);
            
            // Convert entities to DTOs
            List<BundleInfo> bundleInfos = bundles.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(bundleInfos);
            
        } catch (Exception e) {
            logger.error("Error listing bundles for tenant {}: {}", tenantId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to list bundles");
        }
    }
    
    /**
     * Validates if the file has an allowed extension.
     * 
     * @param filename The filename to validate
     * @return true if the extension is allowed
     */
    private boolean isValidFileExtension(String filename) {
        String lowerFilename = filename.toLowerCase();
        return properties.getUpload().getAllowedExtensions()
            .stream()
            .anyMatch(lowerFilename::endsWith);
    }

    private long parseSizeToBytes(String size) {
        if (size == null || size.isEmpty()) return 0L;
        String normalized = size.trim().toUpperCase();
        try {
            if (normalized.endsWith("KB")) {
                return Long.parseLong(normalized.substring(0, normalized.length() - 2).trim()) * 1024L;
            } else if (normalized.endsWith("MB")) {
                return Long.parseLong(normalized.substring(0, normalized.length() - 2).trim()) * 1024L * 1024L;
            } else if (normalized.endsWith("GB")) {
                return Long.parseLong(normalized.substring(0, normalized.length() - 2).trim()) * 1024L * 1024L * 1024L;
            } else if (normalized.endsWith("B")) {
                return Long.parseLong(normalized.substring(0, normalized.length() - 1).trim());
            } else {
                // assume bytes if no unit
                return Long.parseLong(normalized);
            }
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse max file size '{}', defaulting to 100MB", size);
            return 100L * 1024L * 1024L;
        }
    }
    
    /**
     * Converts a GraphBundleEntity to a BundleInfo DTO.
     * 
     * @param entity The entity to convert
     * @return The converted DTO
     */
    private BundleInfo convertToDto(GraphBundleEntity entity) {
        BundleInfo dto = new BundleInfo(
            entity.getProcessId(),
            entity.getTenantId(),
            entity.getFileName(),
            entity.getStatus()
        );
        
        dto.setGraphName(entity.getGraphName());
        dto.setUploadTime(entity.getUploadTime());
        dto.setCompletionTime(entity.getCompletionTime());
        dto.setErrorMessage(entity.getErrorMessage());
        
        return dto;
    }
}