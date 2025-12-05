package ai.eigloo.agentic.graphbuilder.exception;

import ai.eigloo.agentic.graph.exception.GraphPersistenceException;

import ai.eigloo.agentic.graph.exception.GraphParsingException;
import ai.eigloo.agentic.graph.exception.GraphValidationException;
import ai.eigloo.agentic.graphbuilder.dto.BundleUploadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import jakarta.validation.ConstraintViolationException;
import java.util.UUID;

/**
 * Global exception handler for the graph bundle upload API.
 * Provides consistent error responses and logging with correlation IDs.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Handles bundle processing exceptions.
     */
    @ExceptionHandler(BundleProcessingException.class)
    public ResponseEntity<BundleUploadResponse> handleBundleProcessingException(BundleProcessingException e) {
        String correlationId = getOrCreateCorrelationId(e.getCorrelationId());
        
        logger.error("Bundle processing failed [correlationId={}] [step={}]: {}", 
            correlationId, e.getStep(), e.getMessage(), e);
        
        return ResponseEntity.badRequest()
            .header("X-Correlation-ID", correlationId)
            .body(new BundleUploadResponse(null, "FAILED", 
                "Bundle processing failed: " + e.getMessage()));
    }
    
    /**
     * Handles Docker build exceptions.
     */
    @ExceptionHandler(DockerBuildException.class)
    public ResponseEntity<BundleUploadResponse> handleDockerBuildException(DockerBuildException e) {
        String correlationId = getOrCreateCorrelationId(e.getCorrelationId());
        
        logger.error("Docker build failed [correlationId={}] [imageName={}]: {}", 
            correlationId, e.getImageName(), e.getMessage(), e);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .header("X-Correlation-ID", correlationId)
            .body(new BundleUploadResponse(null, "FAILED", 
                "Docker image build failed: " + e.getMessage()));
    }
    
    /**
     * Handles graph persistence exceptions.
     */
    @ExceptionHandler(GraphPersistenceException.class)
    public ResponseEntity<BundleUploadResponse> handleGraphPersistenceException(GraphPersistenceException e) {
        String correlationId = getOrCreateCorrelationId(e.getCorrelationId());
        
        logger.error("Graph persistence failed [correlationId={}] [graphName={}]: {}", 
            correlationId, e.getGraphName(), e.getMessage(), e);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .header("X-Correlation-ID", correlationId)
            .body(new BundleUploadResponse(null, "FAILED", 
                "Graph persistence failed: " + e.getMessage()));
    }
    
    /**
     * Handles graph parsing exceptions.
     */
    @ExceptionHandler(GraphParsingException.class)
    public ResponseEntity<BundleUploadResponse> handleGraphParsingException(GraphParsingException e) {
        String correlationId = getOrCreateCorrelationId();
        
        logger.error("Graph parsing failed [correlationId={}]: {}", correlationId, e.getMessage(), e);
        
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .header("X-Correlation-ID", correlationId)
            .body(new BundleUploadResponse(null, "FAILED", 
                "Graph parsing failed: " + e.getMessage()));
    }
    
    /**
     * Handles graph validation exceptions.
     */
    @ExceptionHandler(GraphValidationException.class)
    public ResponseEntity<BundleUploadResponse> handleGraphValidationException(GraphValidationException e) {
        String correlationId = getOrCreateCorrelationId();
        
        logger.error("Graph validation failed [correlationId={}]: {}", correlationId, e.getMessage(), e);
        
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .header("X-Correlation-ID", correlationId)
            .body(new BundleUploadResponse(null, "FAILED", 
                "Graph validation failed: " + e.getMessage()));
    }
    
    /**
     * Handles file upload size exceeded exceptions.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<BundleUploadResponse> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        String correlationId = getOrCreateCorrelationId();
        
        logger.error("File upload size exceeded [correlationId={}]: {}", correlationId, e.getMessage());
        
        return ResponseEntity.badRequest()
            .header("X-Correlation-ID", correlationId)
            .body(new BundleUploadResponse(null, "FAILED", 
                "File size exceeds maximum allowed size"));
    }
    
    /**
     * Handles validation constraint violations.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<BundleUploadResponse> handleConstraintViolationException(ConstraintViolationException e) {
        String correlationId = getOrCreateCorrelationId();
        
        logger.error("Validation constraint violation [correlationId={}]: {}", correlationId, e.getMessage());
        
        return ResponseEntity.badRequest()
            .header("X-Correlation-ID", correlationId)
            .body(new BundleUploadResponse(null, "FAILED", 
                "Validation failed: " + e.getMessage()));
    }
    
    /**
     * Handles illegal argument exceptions.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<BundleUploadResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        String correlationId = getOrCreateCorrelationId();
        
        logger.error("Invalid argument [correlationId={}]: {}", correlationId, e.getMessage());
        
        return ResponseEntity.badRequest()
            .header("X-Correlation-ID", correlationId)
            .body(new BundleUploadResponse(null, "FAILED", e.getMessage()));
    }
    
    /**
     * Handles missing request parameters (e.g., missing tenantId or file).
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<BundleUploadResponse> handleMissingServletRequestParameter(MissingServletRequestParameterException e) {
        String correlationId = getOrCreateCorrelationId();
        String name = e.getParameterName();
        String message;
        if ("tenantId".equals(name)) {
            message = "Tenant ID cannot be null";
        } else if ("file".equals(name)) {
            message = "File cannot be null";
        } else {
            message = "Missing required parameter: " + name;
        }
        logger.error("Missing request parameter '{}' [correlationId={}]: {}", name, correlationId, e.getMessage());
        return ResponseEntity.badRequest()
            .header("X-Correlation-ID", correlationId)
            .body(new BundleUploadResponse(null, "FAILED", message));
    }
    
    /**
     * Handles missing multipart parts (e.g., missing file part).
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<BundleUploadResponse> handleMissingServletRequestPart(MissingServletRequestPartException e) {
        String correlationId = getOrCreateCorrelationId();
        String name = e.getRequestPartName();
        String message = "file".equals(name) ? "File cannot be null" : ("Missing required part: " + name);
        logger.error("Missing request part '{}' [correlationId={}]: {}", name, correlationId, e.getMessage());
        return ResponseEntity.badRequest()
            .header("X-Correlation-ID", correlationId)
            .body(new BundleUploadResponse(null, "FAILED", message));
    }
    
    /**
     * Handles all other unexpected exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<BundleUploadResponse> handleGenericException(Exception e) {
        String correlationId = getOrCreateCorrelationId();
        
        logger.error("Unexpected error [correlationId={}]: {}", correlationId, e.getMessage(), e);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .header("X-Correlation-ID", correlationId)
            .body(new BundleUploadResponse(null, "FAILED", 
                "An unexpected error occurred. Please contact support with correlation ID: " + correlationId));
    }
    
    /**
     * Gets or creates a correlation ID for error tracking.
     */
    private String getOrCreateCorrelationId() {
        return getOrCreateCorrelationId(null);
    }
    
    /**
     * Gets or creates a correlation ID for error tracking.
     */
    private String getOrCreateCorrelationId(String existingId) {
        if (existingId != null && !existingId.trim().isEmpty()) {
            return existingId;
        }
        
        // Try to get from MDC first
        String mdcId = MDC.get("correlationId");
        if (mdcId != null && !mdcId.trim().isEmpty()) {
            return mdcId;
        }
        
        // Generate new correlation ID
        String newId = UUID.randomUUID().toString();
        MDC.put("correlationId", newId);
        return newId;
    }
}