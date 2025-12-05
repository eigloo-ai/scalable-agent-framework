package ai.eigloo.agentic.graphcomposer.exception;

import ai.eigloo.agentic.graphcomposer.service.GraphService;
import ai.eigloo.agentic.graphcomposer.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GraphComposerExceptionHandlerTest {

    private GraphComposerExceptionHandler exceptionHandler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GraphComposerExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/graphs");
    }

    @Test
    void handleGraphValidation_ShouldReturnValidationErrorResponse() {
        // Given
        List<String> errors = List.of("Invalid node connection", "Missing required field");
        List<String> warnings = List.of("Performance warning");
        GraphValidationException exception = new GraphValidationException(
            "Graph validation failed", errors, warnings);

        // When
        ResponseEntity<GraphComposerExceptionHandler.ValidationErrorResponse> response = 
            exceptionHandler.handleGraphValidation(exception, request);

        // Then
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("GRAPH_VALIDATION_FAILED", response.getBody().getError());
        assertEquals("Graph validation failed", response.getBody().getMessage());
        assertEquals("/api/v1/graphs", response.getBody().getPath());
        assertNotNull(response.getBody().getDetails());
        assertEquals(errors, response.getBody().getDetails().get("errors"));
        assertEquals(warnings, response.getBody().getDetails().get("warnings"));
    }

    @Test
    void handleFileProcessing_ShouldReturnErrorResponse() {
        // Given
        FileProcessingException exception = new FileProcessingException(
            "File processing failed", "plan.py", "save");

        // When
        ResponseEntity<GraphComposerExceptionHandler.ErrorResponse> response = 
            exceptionHandler.handleFileProcessing(exception, request);

        // Then
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("FILE_PROCESSING_ERROR", response.getBody().getError());
        assertEquals("File processing failed", response.getBody().getMessage());
        assertEquals("/api/v1/graphs", response.getBody().getPath());
        assertNotNull(response.getBody().getDetails());
        assertEquals("plan.py", response.getBody().getDetails().get("fileName"));
        assertEquals("save", response.getBody().getDetails().get("operation"));
    }

    @Test
    void handleTenantAccess_ShouldReturnForbiddenResponse() {
        // Given
        TenantAccessException exception = new TenantAccessException(
            "Access denied", "tenant-123", "graph-456");

        // When
        ResponseEntity<GraphComposerExceptionHandler.ErrorResponse> response = 
            exceptionHandler.handleTenantAccess(exception, request);

        // Then
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("TENANT_ACCESS_DENIED", response.getBody().getError());
        assertEquals("Access denied", response.getBody().getMessage());
        assertEquals("/api/v1/graphs", response.getBody().getPath());
        assertNotNull(response.getBody().getDetails());
        assertEquals("tenant-123", response.getBody().getDetails().get("tenantId"));
        assertEquals("graph-456", response.getBody().getDetails().get("resource"));
    }

    @Test
    void handleGraphNotFound_ShouldReturnNotFoundResponse() {
        // Given
        GraphService.GraphNotFoundException exception = 
            new GraphService.GraphNotFoundException("Graph not found: graph-123");

        // When
        ResponseEntity<GraphComposerExceptionHandler.ErrorResponse> response = 
            exceptionHandler.handleGraphNotFound(exception, request);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("GRAPH_NOT_FOUND", response.getBody().getError());
        assertEquals("Graph not found: graph-123", response.getBody().getMessage());
        assertEquals("/api/v1/graphs", response.getBody().getPath());
    }



    @Test
    void handlePlanNotFound_ShouldReturnNotFoundResponse() {
        // Given
        FileService.PlanNotFoundException exception = 
            new FileService.PlanNotFoundException("Plan not found: plan-123");

        // When
        ResponseEntity<GraphComposerExceptionHandler.ErrorResponse> response = 
            exceptionHandler.handlePlanNotFound(exception, request);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("PLAN_NOT_FOUND", response.getBody().getError());
        assertEquals("Plan not found: plan-123", response.getBody().getMessage());
    }

    @Test
    void handleTaskNotFound_ShouldReturnNotFoundResponse() {
        // Given
        FileService.TaskNotFoundException exception = 
            new FileService.TaskNotFoundException("Task not found: task-123");

        // When
        ResponseEntity<GraphComposerExceptionHandler.ErrorResponse> response = 
            exceptionHandler.handleTaskNotFound(exception, request);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("TASK_NOT_FOUND", response.getBody().getError());
        assertEquals("Task not found: task-123", response.getBody().getMessage());
    }

    @Test
    void handleFileSizeExceeded_ShouldReturnPayloadTooLargeResponse() {
        // Given
        FileService.FileSizeExceededException exception = 
            new FileService.FileSizeExceededException("File size exceeded: 10MB");

        // When
        ResponseEntity<GraphComposerExceptionHandler.ErrorResponse> response = 
            exceptionHandler.handleFileSizeExceeded(exception, request);

        // Then
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("FILE_SIZE_EXCEEDED", response.getBody().getError());
        assertEquals("File size exceeded: 10MB", response.getBody().getMessage());
    }

    @Test
    void handleValidationErrors_ShouldReturnBadRequestResponse() {
        // Given
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("graph", "name", "Name is required");
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));
        
        // Create a mock MethodParameter to avoid null pointer exception
        org.springframework.core.MethodParameter methodParameter = mock(org.springframework.core.MethodParameter.class);
        when(methodParameter.getParameterIndex()).thenReturn(0);
        
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
            methodParameter, bindingResult);

        // When
        ResponseEntity<GraphComposerExceptionHandler.ValidationErrorResponse> response = 
            exceptionHandler.handleValidationErrors(exception, request);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("VALIDATION_ERROR", response.getBody().getError());
        assertEquals("Request validation failed", response.getBody().getMessage());
        assertNotNull(response.getBody().getDetails());
        assertEquals("Name is required", response.getBody().getDetails().get("name"));
    }

    @Test
    void handleDataIntegrityViolation_ShouldReturnConflictResponse() {
        // Given
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
            "unique constraint violation");

        // When
        ResponseEntity<GraphComposerExceptionHandler.ErrorResponse> response = 
            exceptionHandler.handleDataIntegrityViolation(exception, request);

        // Then
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("DATA_INTEGRITY_VIOLATION", response.getBody().getError());
        assertEquals("A record with this information already exists", response.getBody().getMessage());
    }

    @Test
    void handleIllegalArgument_ShouldReturnBadRequestResponse() {
        // Given
        IllegalArgumentException exception = new IllegalArgumentException("Invalid argument");

        // When
        ResponseEntity<GraphComposerExceptionHandler.ErrorResponse> response = 
            exceptionHandler.handleIllegalArgument(exception, request);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INVALID_ARGUMENT", response.getBody().getError());
        assertEquals("Invalid argument", response.getBody().getMessage());
    }

    @Test
    void handleRuntimeException_ShouldReturnInternalServerErrorResponse() {
        // Given
        RuntimeException exception = new RuntimeException("Unexpected error");

        // When
        ResponseEntity<GraphComposerExceptionHandler.ErrorResponse> response = 
            exceptionHandler.handleRuntimeException(exception, request);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_ERROR", response.getBody().getError());
        assertEquals("An unexpected error occurred", response.getBody().getMessage());
    }

    @Test
    void handleGenericException_ShouldReturnInternalServerErrorResponse() {
        // Given
        Exception exception = new Exception("Generic error");

        // When
        ResponseEntity<GraphComposerExceptionHandler.ErrorResponse> response = 
            exceptionHandler.handleGenericException(exception, request);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_ERROR", response.getBody().getError());
        assertEquals("An unexpected error occurred while processing your request", 
            response.getBody().getMessage());
    }
}