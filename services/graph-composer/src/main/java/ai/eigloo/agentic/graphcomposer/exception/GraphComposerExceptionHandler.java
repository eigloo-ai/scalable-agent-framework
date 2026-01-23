package ai.eigloo.agentic.graphcomposer.exception;

import ai.eigloo.agentic.graphcomposer.service.GraphService;
import ai.eigloo.agentic.graphcomposer.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the Graph Composer application.
 * Provides centralized error handling and user-friendly error responses.
 */
@ControllerAdvice
public class GraphComposerExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GraphComposerExceptionHandler.class);

    /**
     * Handle graph validation exceptions.
     */
    @ExceptionHandler(GraphValidationException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ResponseEntity<ValidationErrorResponse> handleGraphValidation(
            GraphValidationException ex, HttpServletRequest request) {
        logger.warn("Graph validation failed: {}", ex.getMessage());
        
        Map<String, Object> details = new HashMap<>();
        details.put("errors", ex.getValidationErrors());
        details.put("warnings", ex.getValidationWarnings());
        
        ValidationErrorResponse error = new ValidationErrorResponse(
            "GRAPH_VALIDATION_FAILED",
            ex.getMessage(),
            LocalDateTime.now(),
            request.getRequestURI(),
            details
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }

    /**
     * Handle file processing exceptions.
     */
    @ExceptionHandler(FileProcessingException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ResponseEntity<ErrorResponse> handleFileProcessing(
            FileProcessingException ex, HttpServletRequest request) {
        logger.error("File processing error: {}", ex.getMessage(), ex);
        
        Map<String, Object> details = new HashMap<>();
        if (ex.getFileName() != null) {
            details.put("fileName", ex.getFileName());
        }
        if (ex.getOperation() != null) {
            details.put("operation", ex.getOperation());
        }
        
        ErrorResponse error = new ErrorResponse(
            "FILE_PROCESSING_ERROR",
            ex.getMessage(),
            LocalDateTime.now(),
            request.getRequestURI(),
            details
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }

    /**
     * Handle tenant access exceptions.
     */
    @ExceptionHandler(TenantAccessException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<ErrorResponse> handleTenantAccess(
            TenantAccessException ex, HttpServletRequest request) {
        logger.warn("Tenant access denied: {}", ex.getMessage());
        
        Map<String, Object> details = new HashMap<>();
        if (ex.getTenantId() != null) {
            details.put("tenantId", ex.getTenantId());
        }
        if (ex.getResource() != null) {
            details.put("resource", ex.getResource());
        }
        
        ErrorResponse error = new ErrorResponse(
            "TENANT_ACCESS_DENIED",
            ex.getMessage(),
            LocalDateTime.now(),
            request.getRequestURI(),
            details
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Handle graph not found exceptions.
     */
    @ExceptionHandler(GraphService.GraphNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handleGraphNotFound(
            GraphService.GraphNotFoundException ex, HttpServletRequest request) {
        logger.warn("Graph not found: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            "GRAPH_NOT_FOUND",
            ex.getMessage(),
            LocalDateTime.now(),
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }



    /**
     * Handle legacy tenant access exceptions from GraphService.
     */
    @ExceptionHandler(GraphService.TenantAccessException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<ErrorResponse> handleLegacyTenantAccess(
            GraphService.TenantAccessException ex, HttpServletRequest request) {
        logger.warn("Legacy tenant access denied: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            "TENANT_ACCESS_DENIED",
            ex.getMessage(),
            LocalDateTime.now(),
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Handle plan not found exceptions.
     */
    @ExceptionHandler(FileService.PlanNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handlePlanNotFound(
            FileService.PlanNotFoundException ex, HttpServletRequest request) {
        logger.warn("Plan not found: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            "PLAN_NOT_FOUND",
            ex.getMessage(),
            LocalDateTime.now(),
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handle task not found exceptions.
     */
    @ExceptionHandler(FileService.TaskNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handleTaskNotFound(
            FileService.TaskNotFoundException ex, HttpServletRequest request) {
        logger.warn("Task not found: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            "TASK_NOT_FOUND",
            ex.getMessage(),
            LocalDateTime.now(),
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handle file size exceeded exceptions.
     */
    @ExceptionHandler(FileService.FileSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public ResponseEntity<ErrorResponse> handleFileSizeExceeded(
            FileService.FileSizeExceededException ex, HttpServletRequest request) {
        logger.warn("File size exceeded: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            "FILE_SIZE_EXCEEDED",
            ex.getMessage(),
            LocalDateTime.now(),
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(error);
    }

    /**
     * Handle validation errors from @Valid annotations.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ValidationErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        logger.warn("Request validation failed");
        
        Map<String, Object> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        ValidationErrorResponse errorResponse = new ValidationErrorResponse(
            "VALIDATION_ERROR",
            "Request validation failed",
            LocalDateTime.now(),
            request.getRequestURI(),
            fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle method-level constraint violations (e.g., @Size on @RequestBody String).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ValidationErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, Object> fieldErrors = new HashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String fieldPath = violation.getPropertyPath() != null ? violation.getPropertyPath().toString() : "parameter";
            fieldErrors.put(fieldPath, violation.getMessage());
        }

        ValidationErrorResponse errorResponse = new ValidationErrorResponse(
            "VALIDATION_ERROR",
            "Request validation failed",
            LocalDateTime.now(),
            request.getRequestURI(),
            fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle missing request parameters.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        logger.warn("Missing request parameter: {}", ex.getMessage());
        
        Map<String, Object> details = new HashMap<>();
        details.put("parameterName", ex.getParameterName());
        details.put("parameterType", ex.getParameterType());
        
        ErrorResponse error = new ErrorResponse(
            "MISSING_PARAMETER",
            "Required parameter '" + ex.getParameterName() + "' is missing",
            LocalDateTime.now(),
            request.getRequestURI(),
            details
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle method argument type mismatch.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        logger.warn("Method argument type mismatch: {}", ex.getMessage());
        
        Map<String, Object> details = new HashMap<>();
        details.put("parameterName", ex.getName());
        details.put("providedValue", ex.getValue());
        details.put("expectedType", ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        
        ErrorResponse error = new ErrorResponse(
            "INVALID_PARAMETER_TYPE",
            "Invalid value for parameter '" + ex.getName() + "'",
            LocalDateTime.now(),
            request.getRequestURI(),
            details
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle malformed JSON requests.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleMalformedJson(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        logger.warn("Malformed JSON request: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            "MALFORMED_REQUEST",
            "Request body is malformed or invalid JSON",
            LocalDateTime.now(),
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle unsupported HTTP methods.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        logger.warn("Method not supported: {}", ex.getMessage());
        
        Map<String, Object> details = new HashMap<>();
        details.put("method", ex.getMethod());
        details.put("supportedMethods", ex.getSupportedMethods());
        
        ErrorResponse error = new ErrorResponse(
            "METHOD_NOT_ALLOWED",
            "HTTP method '" + ex.getMethod() + "' is not supported for this endpoint",
            LocalDateTime.now(),
            request.getRequestURI(),
            details
        );
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
    }

    /**
     * Handle 404 errors.
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handleNotFound(
            NoHandlerFoundException ex, HttpServletRequest request) {
        logger.warn("Endpoint not found: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            "ENDPOINT_NOT_FOUND",
            "The requested endpoint was not found",
            LocalDateTime.now(),
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handle database constraint violations.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        logger.error("Data integrity violation: {}", ex.getMessage(), ex);
        
        String message = "Data integrity constraint violation";
        if (ex.getMessage() != null && ex.getMessage().contains("unique")) {
            message = "A record with this information already exists";
        }
        
        ErrorResponse error = new ErrorResponse(
            "DATA_INTEGRITY_VIOLATION",
            message,
            LocalDateTime.now(),
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handle general database access exceptions.
     */
    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleDataAccessException(
            DataAccessException ex, HttpServletRequest request) {
        logger.error("Database access error: {}", ex.getMessage(), ex);
        
        ErrorResponse error = new ErrorResponse(
            "DATABASE_ERROR",
            "A database error occurred while processing your request",
            LocalDateTime.now(),
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Handle illegal argument exceptions.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        logger.warn("Illegal argument: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            "INVALID_ARGUMENT",
            ex.getMessage(),
            LocalDateTime.now(),
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle general runtime exceptions (catch-all).
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex, HttpServletRequest request) {
        logger.error("Unexpected runtime exception: {}", ex.getMessage(), ex);
        
        // Don't expose internal error details in production
        String message = "An unexpected error occurred";
        if (logger.isDebugEnabled()) {
            message += ": " + ex.getMessage();
        }
        
        ErrorResponse error = new ErrorResponse(
            "INTERNAL_ERROR",
            message,
            LocalDateTime.now(),
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Handle all other exceptions (ultimate catch-all).
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        logger.error("Unexpected exception: {}", ex.getMessage(), ex);
        
        ErrorResponse error = new ErrorResponse(
            "INTERNAL_ERROR",
            "An unexpected error occurred while processing your request",
            LocalDateTime.now(),
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Standard error response structure.
     */
    public static class ErrorResponse {
        private String error;
        private String message;
        private int status;
        private LocalDateTime timestamp;
        private String path;
        private Map<String, Object> details;

        public ErrorResponse(String error, String message, LocalDateTime timestamp) {
            this.error = error;
            this.message = message;
            this.timestamp = timestamp;
        }

        public ErrorResponse(String error, String message, LocalDateTime timestamp, String path) {
            this.error = error;
            this.message = message;
            this.timestamp = timestamp;
            this.path = path;
        }

        public ErrorResponse(String error, String message, LocalDateTime timestamp, String path, Map<String, Object> details) {
            this.error = error;
            this.message = message;
            this.timestamp = timestamp;
            this.path = path;
            this.details = details;
        }

        // Getters and setters
        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public Map<String, Object> getDetails() {
            return details;
        }

        public void setDetails(Map<String, Object> details) {
            this.details = details;
        }
    }

    /**
     * Validation error response with field-specific errors.
     */
    public static class ValidationErrorResponse extends ErrorResponse {
        public ValidationErrorResponse(String error, String message, LocalDateTime timestamp, String path, Map<String, Object> details) {
            super(error, message, timestamp, path, details);
        }
    }
}