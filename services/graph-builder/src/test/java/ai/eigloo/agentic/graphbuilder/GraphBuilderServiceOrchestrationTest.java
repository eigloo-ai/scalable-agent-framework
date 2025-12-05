package ai.eigloo.agentic.graphbuilder;

import ai.eigloo.agentic.graph.model.AgentGraph;
import ai.eigloo.agentic.graph.model.Plan;
import ai.eigloo.agentic.graph.model.Task;
import ai.eigloo.agentic.graphbuilder.dto.ProcessingStatus;
import ai.eigloo.agentic.graphbuilder.entity.GraphBundleEntity;
import ai.eigloo.agentic.graphbuilder.enums.BundleStatus;
import ai.eigloo.agentic.graphbuilder.enums.StepStatus;
import ai.eigloo.agentic.graphbuilder.exception.BundleProcessingException;
import ai.eigloo.agentic.graphbuilder.exception.DockerBuildException;
import ai.eigloo.agentic.graph.exception.GraphPersistenceException;
import ai.eigloo.agentic.graphbuilder.service.BundleProcessingService;
import ai.eigloo.agentic.graphbuilder.service.CleanupService;
import ai.eigloo.agentic.graphbuilder.service.DockerImageService;
import ai.eigloo.agentic.graph.service.GraphPersistenceService;
import ai.eigloo.agentic.graphbuilder.service.ProcessingStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GraphBuilderService orchestration logic.
 * Tests the coordination between different services during bundle processing.
 */
class GraphBuilderServiceOrchestrationTest {
    
    @Mock
    private BundleProcessingService bundleProcessingService;
    
    @Mock
    private DockerImageService dockerImageService;
    
    @Mock
    private GraphPersistenceService graphPersistenceService;
    
    @Mock
    private ProcessingStatusService processingStatusService;
    
    @Mock
    private CleanupService cleanupService;
    
    @Mock
    private MultipartFile mockFile;
    
    private GraphBuilderService graphBuilderService;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        graphBuilderService = new GraphBuilderService(
            bundleProcessingService,
            dockerImageService,
            graphPersistenceService,
            processingStatusService,
            cleanupService
        );
    }
    
    @Test
    void processBundle_SuccessfulFlow_ShouldOrchestrateProperly() throws Exception {
        // Given
        String tenantId = "test-tenant";
        String fileName = "test.zip";
        String processId = "test-process-123";
        
        when(mockFile.getOriginalFilename()).thenReturn(fileName);
        
        // Mock successful bundle processing
        Path extractDir = tempDir.resolve("extracted");
        Files.createDirectories(extractDir);
        Files.createFile(extractDir.resolve("agent_graph.dot"));
        Files.write(extractDir.resolve("agent_graph.dot"), 
            "digraph G {\n  plan1 [type=\"plan\"];\n  task1 [type=\"task\"];\n  plan1 -> task1;\n}".getBytes());
        
        Path plansDir = extractDir.resolve("plans/plan1");
        Files.createDirectories(plansDir);
        Files.createFile(plansDir.resolve("plan.py"));
        
        Path tasksDir = extractDir.resolve("tasks/task1");
        Files.createDirectories(tasksDir);
        Files.createFile(tasksDir.resolve("task.py"));
        
        GraphBundleEntity bundleEntity = new GraphBundleEntity("bundle-id", tenantId, fileName, BundleStatus.UPLOADED.getValue(), processId);
        
        // Mock service responses
        when(processingStatusService.createBundleEntity(eq(tenantId), eq(fileName), anyString())).thenReturn(bundleEntity);
        when(bundleProcessingService.extractBundle(eq(mockFile), anyString())).thenReturn(extractDir);
        when(graphPersistenceService.persistGraph(any(AgentGraph.class), eq(tenantId))).thenReturn("graph-id");
        when(dockerImageService.buildTaskImage(eq(tenantId), any(Task.class), any(Path.class), anyString())).thenReturn("task-image:latest");
        when(dockerImageService.buildPlanImage(eq(tenantId), any(Plan.class), any(Path.class), anyString())).thenReturn("plan-image:latest");
        
        // When
        java.util.concurrent.CompletableFuture<String> result = graphBuilderService.processBundle(tenantId, mockFile);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.join()).isNotBlank();
        
        // Verify orchestration order
        InOrder inOrder = inOrder(bundleProcessingService, processingStatusService, graphPersistenceService, dockerImageService, cleanupService);
        
        // 1. Validate file
        inOrder.verify(bundleProcessingService).validateFile(eq(mockFile));
        
        // 2. Create bundle entity
        inOrder.verify(processingStatusService).createBundleEntity(eq(tenantId), eq(fileName), anyString());
        
        // 3. Create extraction step
        inOrder.verify(processingStatusService).createProcessingStep(anyString(), eq("EXTRACTING"), eq(StepStatus.IN_PROGRESS));
        
        // 4. Extract bundle
        inOrder.verify(bundleProcessingService).extractBundle(eq(mockFile), anyString());
        
        // 5. Update extraction step to completed
        inOrder.verify(processingStatusService).updateProcessingStep(anyString(), eq("EXTRACTING"), eq(StepStatus.COMPLETED));
        
        // 6. Create parsing step
        inOrder.verify(processingStatusService).createProcessingStep(anyString(), eq("PARSING"), eq(StepStatus.IN_PROGRESS));
        
        // 7. Update parsing step to completed
        inOrder.verify(processingStatusService).updateProcessingStep(anyString(), eq("PARSING"), eq(StepStatus.COMPLETED));
        
        // 8. Create persistence step
        inOrder.verify(processingStatusService).createProcessingStep(anyString(), eq("PERSISTING"), eq(StepStatus.IN_PROGRESS));
        
        // 9. Persist graph
        inOrder.verify(graphPersistenceService).persistGraph(any(AgentGraph.class), eq(tenantId));
        
        // 10. Update persistence step to completed
        inOrder.verify(processingStatusService).updateProcessingStep(anyString(), eq("PERSISTING"), eq(StepStatus.COMPLETED));
        
        // 11. Create docker build step
        inOrder.verify(processingStatusService).createProcessingStep(anyString(), eq("BUILDING_IMAGES"), eq(StepStatus.IN_PROGRESS));
        
        // 12. Build docker images
        inOrder.verify(dockerImageService).buildTaskImage(eq(tenantId), any(Task.class), any(Path.class), anyString());
        inOrder.verify(dockerImageService).buildPlanImage(eq(tenantId), any(Plan.class), any(Path.class), anyString());
        
        // 13. Update docker build step to completed
        inOrder.verify(processingStatusService).updateProcessingStep(anyString(), eq("BUILDING_IMAGES"), eq(StepStatus.COMPLETED));
        
        // 14. Mark process as completed
        inOrder.verify(processingStatusService).markProcessAsCompleted(anyString());
    }
    
    @Test
    void processBundle_ValidationFailure_ShouldHandleErrorProperly() throws Exception {
        // Given
        String tenantId = "test-tenant";
        String fileName = "invalid.txt";
        
        when(mockFile.getOriginalFilename()).thenReturn(fileName);
        
        GraphBundleEntity bundleEntity = new GraphBundleEntity("bundle-id", tenantId, fileName, BundleStatus.UPLOADED.getValue(), "process-id");
        when(processingStatusService.createBundleEntity(eq(tenantId), eq(fileName), anyString())).thenReturn(bundleEntity);
        
        // Mock validation failure
        BundleProcessingException validationError = new BundleProcessingException("Invalid file format", "correlation-id", "VALIDATION");
        doThrow(validationError).when(bundleProcessingService).validateFile(eq(mockFile));
        
        // When
        java.util.concurrent.CompletableFuture<String> result = graphBuilderService.processBundle(tenantId, mockFile);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.join()).isNotBlank();
        
        // Verify error handling
        verify(processingStatusService).markProcessAsFailed(anyString(), eq("VALIDATION"), contains("Invalid file format"));
        verify(cleanupService).performComprehensiveCleanup(anyString(), isNull(), any(CleanupService.CleanupLevel.class));
        
        // Verify no further processing occurred
        verify(bundleProcessingService, never()).extractBundle(any(), anyString());
        verify(graphPersistenceService, never()).persistGraph(any(), anyString());
        verify(dockerImageService, never()).buildTaskImage(anyString(), any(), any(), anyString());
    }
    
    @Test
    void processBundle_ExtractionFailure_ShouldHandleErrorProperly() throws Exception {
        // Given
        String tenantId = "test-tenant";
        String fileName = "test.zip";
        
        when(mockFile.getOriginalFilename()).thenReturn(fileName);
        
        GraphBundleEntity bundleEntity = new GraphBundleEntity("bundle-id", tenantId, fileName, BundleStatus.UPLOADED.getValue(), "process-id");
        when(processingStatusService.createBundleEntity(eq(tenantId), eq(fileName), anyString())).thenReturn(bundleEntity);
        
        // Mock extraction failure
        BundleProcessingException extractionError = new BundleProcessingException("Failed to extract archive", "correlation-id", "EXTRACTION");
        when(bundleProcessingService.extractBundle(eq(mockFile), anyString())).thenThrow(extractionError);
        
        // When
        java.util.concurrent.CompletableFuture<String> result = graphBuilderService.processBundle(tenantId, mockFile);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.join()).isNotBlank();
        
        // Verify error handling
        verify(processingStatusService).markProcessAsFailed(anyString(), eq("EXTRACTING"), contains("Failed to extract archive"));
        verify(cleanupService).performComprehensiveCleanup(anyString(), isNull(), any(CleanupService.CleanupLevel.class));
        
        // Verify no further processing occurred
        verify(graphPersistenceService, never()).persistGraph(any(), anyString());
        verify(dockerImageService, never()).buildTaskImage(anyString(), any(), any(), anyString());
    }
    
    @Test
    void processBundle_PersistenceFailure_ShouldHandleErrorProperly() throws Exception {
        // Given
        String tenantId = "test-tenant";
        String fileName = "test.zip";
        
        when(mockFile.getOriginalFilename()).thenReturn(fileName);
        
        // Mock successful extraction
        Path extractDir = tempDir.resolve("extracted");
        Files.createDirectories(extractDir);
        Files.createFile(extractDir.resolve("agent_graph.dot"));
        Files.write(extractDir.resolve("agent_graph.dot"), 
            "digraph G {\n  plan1 [type=\"plan\"];\n  task1 [type=\"task\"];\n  plan1 -> task1;\n}".getBytes());
        
        Path plan1Dir = extractDir.resolve("plans/plan1");
        Files.createDirectories(plan1Dir);
        Files.createFile(plan1Dir.resolve("plan.py"));
        
        Path task1Dir = extractDir.resolve("tasks/task1");
        Files.createDirectories(task1Dir);
        Files.createFile(task1Dir.resolve("task.py"));
        
        GraphBundleEntity bundleEntity = new GraphBundleEntity("bundle-id", tenantId, fileName, BundleStatus.UPLOADED.getValue(), "process-id");
        when(processingStatusService.createBundleEntity(eq(tenantId), eq(fileName), anyString())).thenReturn(bundleEntity);
        when(bundleProcessingService.extractBundle(eq(mockFile), anyString())).thenReturn(extractDir);
        
        // Mock persistence failure
        GraphPersistenceException persistenceError = new GraphPersistenceException("Database error", "correlation-id", "test_graph");
        when(graphPersistenceService.persistGraph(any(AgentGraph.class), eq(tenantId))).thenThrow(persistenceError);
        
        // When
        java.util.concurrent.CompletableFuture<String> result = graphBuilderService.processBundle(tenantId, mockFile);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.join()).isNotBlank();
        
        // Verify error handling
        verify(processingStatusService).markProcessAsFailed(anyString(), eq("PERSISTING"), contains("Database error"));
        verify(cleanupService).performComprehensiveCleanup(anyString(), any(Path.class), any(CleanupService.CleanupLevel.class));
        verify(bundleProcessingService).cleanupTempDirectory(extractDir);
        
        // Verify no Docker builds occurred
        verify(dockerImageService, never()).buildTaskImage(anyString(), any(), any(), anyString());
        verify(dockerImageService, never()).buildPlanImage(anyString(), any(), any(), anyString());
    }
    
    @Test
    void processBundle_DockerBuildFailure_ShouldHandleErrorProperly() throws Exception {
        // Given
        String tenantId = "test-tenant";
        String fileName = "test.zip";
        
        when(mockFile.getOriginalFilename()).thenReturn(fileName);
        
        // Mock successful extraction and persistence
        Path extractDir = tempDir.resolve("extracted");
        Files.createDirectories(extractDir);
        Files.createFile(extractDir.resolve("agent_graph.dot"));
        Files.write(extractDir.resolve("agent_graph.dot"), 
            "digraph G {\n  plan1 [type=\"plan\"];\n  task1 [type=\"task\"];\n  plan1 -> task1;\n}".getBytes());
        
        Path tasksDir = extractDir.resolve("tasks/task1");
        Files.createDirectories(tasksDir);
        Files.createFile(tasksDir.resolve("task.py"));
        
        Path plan1Dir = extractDir.resolve("plans/plan1");
        Files.createDirectories(plan1Dir);
        Files.createFile(plan1Dir.resolve("plan.py"));
        
        GraphBundleEntity bundleEntity = new GraphBundleEntity("bundle-id", tenantId, fileName, BundleStatus.UPLOADED.getValue(), "process-id");
        when(processingStatusService.createBundleEntity(eq(tenantId), eq(fileName), anyString())).thenReturn(bundleEntity);
        when(bundleProcessingService.extractBundle(eq(mockFile), anyString())).thenReturn(extractDir);
        when(graphPersistenceService.persistGraph(any(AgentGraph.class), eq(tenantId))).thenReturn("graph-id");
        
        // Mock Docker build failure
        DockerBuildException dockerError = new DockerBuildException("Docker build failed", "correlation-id", "task-image");
        when(dockerImageService.buildTaskImage(eq(tenantId), any(Task.class), any(Path.class), anyString())).thenThrow(dockerError);
        
        // When
        java.util.concurrent.CompletableFuture<String> result = graphBuilderService.processBundle(tenantId, mockFile);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.join()).isNotBlank();
        
        // Verify error handling
        verify(processingStatusService).markProcessAsFailed(anyString(), eq("BUILDING_IMAGES"), contains("Docker build failed"));
        verify(cleanupService).performComprehensiveCleanup(anyString(), any(Path.class), any(CleanupService.CleanupLevel.class));
        verify(bundleProcessingService).cleanupTempDirectory(extractDir);
    }
    
    @Test
    void getProcessingStatus_ExistingProcess_ShouldDelegateToStatusService() {
        // Given
        String processId = "test-process-123";
        ProcessingStatus expectedStatus = new ProcessingStatus(processId, BundleStatus.COMPLETED.getValue());
        when(processingStatusService.getProcessingStatus(eq(processId))).thenReturn(expectedStatus);
        
        // When
        ProcessingStatus result = graphBuilderService.getProcessingStatus(processId);
        
        // Then
        assertThat(result).isEqualTo(expectedStatus);
        verify(processingStatusService).getProcessingStatus(eq(processId));
    }
    
    @Test
    void processBundle_NullFile_ShouldHandleErrorProperly() {
        // Given
        String tenantId = "test-tenant";
        
        // When
        java.util.concurrent.CompletableFuture<String> result = graphBuilderService.processBundle(tenantId, null);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.join()).isNotBlank();
        
        // Verify error handling for null file
        verify(processingStatusService).markProcessAsFailed(anyString(), eq("VALIDATION"), contains("File cannot be null"));
        verify(cleanupService).performComprehensiveCleanup(anyString(), isNull(), any(CleanupService.CleanupLevel.class));
    }
    
    @Test
    void processBundle_NullTenantId_ShouldHandleErrorProperly() {
        // Given
        when(mockFile.getOriginalFilename()).thenReturn("test.zip");
        
        // When
        java.util.concurrent.CompletableFuture<String> result = graphBuilderService.processBundle(null, mockFile);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.join()).isNotBlank();
        
        // Verify error handling for null tenant ID
        verify(processingStatusService).markProcessAsFailed(anyString(), eq("VALIDATION"), contains("Tenant ID cannot be null"));
        verify(cleanupService).performComprehensiveCleanup(anyString(), isNull(), any(CleanupService.CleanupLevel.class));
    }
    
    @Test
    void processBundle_EmptyGraph_ShouldHandleGracefully() throws Exception {
        // Given
        String tenantId = "test-tenant";
        String fileName = "empty.zip";
        
        when(mockFile.getOriginalFilename()).thenReturn(fileName);
        
        // Mock extraction of empty graph
        Path extractDir = tempDir.resolve("extracted");
        Files.createDirectories(extractDir);
        Files.createFile(extractDir.resolve("agent_graph.dot"));
        Files.write(extractDir.resolve("agent_graph.dot"), "digraph G { }".getBytes()); // Empty graph
        
        GraphBundleEntity bundleEntity = new GraphBundleEntity("bundle-id", tenantId, fileName, BundleStatus.UPLOADED.getValue(), "process-id");
        when(processingStatusService.createBundleEntity(eq(tenantId), eq(fileName), anyString())).thenReturn(bundleEntity);
        when(bundleProcessingService.extractBundle(eq(mockFile), anyString())).thenReturn(extractDir);
        when(graphPersistenceService.persistGraph(any(AgentGraph.class), eq(tenantId))).thenReturn("graph-id");
        
        // When
        java.util.concurrent.CompletableFuture<String> result = graphBuilderService.processBundle(tenantId, mockFile);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.join()).isNotBlank();
        
        // Verify processing completed even with empty graph
        verify(processingStatusService).markProcessAsCompleted(anyString());
        verify(bundleProcessingService).cleanupTempDirectory(extractDir);
        
        // Verify no Docker builds for empty graph
        verify(dockerImageService, never()).buildTaskImage(anyString(), any(), any(), anyString());
        verify(dockerImageService, never()).buildPlanImage(anyString(), any(), any(), anyString());
    }
}