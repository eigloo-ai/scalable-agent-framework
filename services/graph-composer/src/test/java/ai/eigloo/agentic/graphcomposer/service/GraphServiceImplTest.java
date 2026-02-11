package ai.eigloo.agentic.graphcomposer.service;

import ai.eigloo.agentic.graphcomposer.dto.AgentGraphDto;
import ai.eigloo.agentic.graphcomposer.dto.AgentGraphSummary;
import ai.eigloo.agentic.graphcomposer.dto.CreateGraphRequest;
import ai.eigloo.agentic.graphcomposer.dto.ExecutionResponse;
import ai.eigloo.agentic.graphcomposer.dto.GraphStatus;
import ai.eigloo.agentic.graph.entity.AgentGraphEntity;
import ai.eigloo.agentic.graph.entity.GraphRunEntity;
import ai.eigloo.agentic.graph.entity.GraphRunStatus;
import ai.eigloo.agentic.graph.entity.PlanEntity;
import ai.eigloo.agentic.graph.repository.AgentGraphRepository;
import ai.eigloo.agentic.graph.repository.GraphRunRepository;
import ai.eigloo.agentic.graph.repository.PlanRepository;
import ai.eigloo.agentic.graph.repository.TaskRepository;
import ai.eigloo.agentic.graphcomposer.dto.GraphStatusUpdate;
import ai.eigloo.agentic.graphcomposer.dto.ValidationResult;
import ai.eigloo.agentic.graphcomposer.dto.*;
import ai.eigloo.agentic.graphcomposer.exception.GraphValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GraphServiceImplTest {

    @Mock
    private AgentGraphRepository agentGraphRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private GraphRunRepository graphRunRepository;

    @Mock
    private FileService fileService;

    @Mock
    private ValidationService validationService;

    @Mock
    private GraphExecutionBootstrapPublisher graphExecutionBootstrapPublisher;

    @InjectMocks
    private GraphServiceImpl graphService;

    private AgentGraphEntity testGraphEntity;
    private AgentGraphDto testGraphDto;
    private CreateGraphRequest testCreateRequest;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        
        testGraphEntity = new AgentGraphEntity();
        testGraphEntity.setId("test-graph-id");
        testGraphEntity.setName("Test Graph");
        testGraphEntity.setTenantId("test-tenant");
        testGraphEntity.setStatus(ai.eigloo.agentic.graph.entity.GraphStatus.NEW);
        testGraphEntity.setCreatedAt(now);
        testGraphEntity.setUpdatedAt(now);

        testGraphDto = new AgentGraphDto();
        testGraphDto.setId("test-graph-id");
        testGraphDto.setName("Test Graph");
        testGraphDto.setTenantId("test-tenant");
        testGraphDto.setStatus(GraphStatus.NEW);
        testGraphDto.setCreatedAt(now);
        testGraphDto.setUpdatedAt(now);

        testCreateRequest = new CreateGraphRequest("New Graph", "test-tenant");
    }

    @Test
    void listGraphs_ShouldReturnGraphSummaries() {
        // Given
        String tenantId = "test-tenant";
        List<AgentGraphEntity> entities = List.of(testGraphEntity);
        when(agentGraphRepository.findByTenantIdOptimized(tenantId)).thenReturn(entities);

        // When
        List<AgentGraphSummary> result = graphService.listGraphs(tenantId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        AgentGraphSummary summary = result.get(0);
        assertEquals(testGraphEntity.getId(), summary.getId());
        assertEquals(testGraphEntity.getName(), summary.getName());
        assertEquals(GraphStatus.NEW, summary.getStatus());
        
        verify(agentGraphRepository).findByTenantIdOptimized(tenantId);
    }

    @Test
    void getGraph_ShouldReturnGraphDto_WhenGraphExists() {
        // Given
        String graphId = "test-graph-id";
        String tenantId = "test-tenant";

        when(agentGraphRepository.findByIdAndTenantId(graphId, tenantId)).thenReturn(Optional.of(testGraphEntity));
        when(planRepository.findByAgentGraphIdWithFiles(graphId)).thenReturn(List.of());
        when(taskRepository.findByAgentGraphIdWithFiles(graphId)).thenReturn(List.of());

        // When
        AgentGraphDto result = graphService.getGraph(graphId, tenantId);

        // Then
        assertNotNull(result);
        assertEquals(testGraphEntity.getId(), result.getId());
        assertEquals(testGraphEntity.getName(), result.getName());
        assertEquals(testGraphEntity.getTenantId(), result.getTenantId());
        
        verify(agentGraphRepository).findByIdAndTenantId(graphId, tenantId);
        verify(planRepository).findByAgentGraphIdWithFiles(graphId);
        verify(taskRepository).findByAgentGraphIdWithFiles(graphId);
    }

    @Test
    void getGraph_ShouldThrowException_WhenGraphNotFound() {
        // Given
        String graphId = "non-existent-id";
        String tenantId = "test-tenant";
        when(agentGraphRepository.findByIdAndTenantId(graphId, tenantId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(GraphService.GraphNotFoundException.class, 
                    () -> graphService.getGraph(graphId, tenantId));
        
        verify(agentGraphRepository).findByIdAndTenantId(graphId, tenantId);
        verifyNoInteractions(planRepository);
        verifyNoInteractions(taskRepository);
    }

    @Test
    void createGraph_ShouldCreateAndReturnGraph() {
        // Given
        AgentGraphEntity savedEntity = new AgentGraphEntity();
        savedEntity.setId("new-graph-id");
        savedEntity.setName(testCreateRequest.getName());
        savedEntity.setTenantId(testCreateRequest.getTenantId());
        savedEntity.setStatus(ai.eigloo.agentic.graph.entity.GraphStatus.NEW);
        savedEntity.setCreatedAt(LocalDateTime.now());
        savedEntity.setUpdatedAt(LocalDateTime.now());
        
        when(agentGraphRepository.save(any(AgentGraphEntity.class))).thenReturn(savedEntity);
        when(planRepository.findByAgentGraphIdWithFiles(anyString())).thenReturn(List.of());
        when(taskRepository.findByAgentGraphIdWithFiles(anyString())).thenReturn(List.of());

        // When
        AgentGraphDto result = graphService.createGraph(testCreateRequest);

        // Then
        assertNotNull(result);
        assertEquals(savedEntity.getId(), result.getId());
        assertEquals(testCreateRequest.getName(), result.getName());
        assertEquals(testCreateRequest.getTenantId(), result.getTenantId());
        assertEquals(GraphStatus.NEW, result.getStatus());
        
        verify(agentGraphRepository).save(any(AgentGraphEntity.class));
    }

    @Test
    void updateGraph_ShouldUpdateAndReturnGraph_WhenValidationPasses() {
        // Given
        String graphId = "test-graph-id";
        ValidationResult validationResult = new ValidationResult(true, List.of(), List.of());
        
        when(agentGraphRepository.findByIdAndTenantId(graphId, testGraphDto.getTenantId()))
                .thenReturn(Optional.of(testGraphEntity));
        when(validationService.validateGraph(testGraphDto)).thenReturn(validationResult);
        when(agentGraphRepository.save(any(AgentGraphEntity.class))).thenReturn(testGraphEntity);

        // When
        AgentGraphDto result = graphService.updateGraph(graphId, testGraphDto);

        // Then
        assertNotNull(result);
        assertEquals(testGraphDto.getName(), result.getName());
        
        verify(agentGraphRepository).findByIdAndTenantId(graphId, testGraphDto.getTenantId());
        verify(validationService).validateGraph(testGraphDto);
        verify(agentGraphRepository).save(any(AgentGraphEntity.class));
    }

    @Test
    void updateGraph_ShouldSetDownstreamPlan_FromPlanUpstreamTaskIds() {
        // Given
        String graphId = "test-graph-id";
        ValidationResult validationResult = new ValidationResult(true, List.of(), List.of());

        PlanDto planA = new PlanDto();
        planA.setName("PlanA");
        planA.setLabel("Plan A");
        planA.setUpstreamTaskIds(Set.of());
        planA.setFiles(List.of());

        PlanDto planB = new PlanDto();
        planB.setName("PlanB");
        planB.setLabel("Plan B");
        planB.setUpstreamTaskIds(Set.of("Task1A"));
        planB.setFiles(List.of());

        TaskDto task1A = new TaskDto();
        task1A.setName("Task1A");
        task1A.setLabel("Task 1A");
        task1A.setUpstreamPlanId("PlanA");
        task1A.setFiles(List.of());

        TaskDto task1B = new TaskDto();
        task1B.setName("Task1B");
        task1B.setLabel("Task 1B");
        task1B.setUpstreamPlanId("PlanA");
        task1B.setFiles(List.of());

        TaskDto task2 = new TaskDto();
        task2.setName("Task2");
        task2.setLabel("Task 2");
        task2.setUpstreamPlanId("PlanB");
        task2.setFiles(List.of());

        AgentGraphDto updateDto = new AgentGraphDto();
        updateDto.setId(graphId);
        updateDto.setName("Test Graph");
        updateDto.setTenantId("test-tenant");
        updateDto.setStatus(GraphStatus.NEW);
        updateDto.setPlans(List.of(planA, planB));
        updateDto.setTasks(List.of(task1A, task1B, task2));
        updateDto.setPlanToTasks(Map.of(
                "PlanA", Set.of("Task1A", "Task1B"),
                "PlanB", Set.of("Task2")
        ));
        updateDto.setTaskToPlan(Map.of(
                "Task1A", "PlanA",
                "Task1B", "PlanA",
                "Task2", "PlanB"
        ));

        when(agentGraphRepository.findByIdAndTenantId(graphId, updateDto.getTenantId()))
                .thenReturn(Optional.of(testGraphEntity));
        when(validationService.validateGraph(updateDto)).thenReturn(validationResult);
        when(agentGraphRepository.save(any(AgentGraphEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        graphService.updateGraph(graphId, updateDto);

        // Then
        ArgumentCaptor<AgentGraphEntity> graphCaptor = ArgumentCaptor.forClass(AgentGraphEntity.class);
        verify(agentGraphRepository).save(graphCaptor.capture());

        AgentGraphEntity savedGraph = graphCaptor.getValue();
        assertNotNull(savedGraph);
        assertEquals(3, savedGraph.getTasks().size());

        Map<String, String> downstreamByTask = new java.util.HashMap<>();
        savedGraph.getTasks().forEach(task -> downstreamByTask.put(
                task.getName(),
                task.getDownstreamPlan() != null ? task.getDownstreamPlan().getName() : ""
        ));

        assertEquals("PlanB", downstreamByTask.get("Task1A"));
        assertEquals("", downstreamByTask.get("Task1B"));
        assertEquals("", downstreamByTask.get("Task2"));
    }

    @Test
    void updateGraph_ShouldThrowException_WhenValidationFails() {
        // Given
        String graphId = "test-graph-id";
        ValidationResult validationResult = new ValidationResult(false, List.of("Validation error"), List.of());
        
        when(agentGraphRepository.findByIdAndTenantId(graphId, testGraphDto.getTenantId()))
                .thenReturn(Optional.of(testGraphEntity));
        when(validationService.validateGraph(testGraphDto)).thenReturn(validationResult);

        // When & Then
        assertThrows(GraphValidationException.class, 
                    () -> graphService.updateGraph(graphId, testGraphDto));
        
        verify(agentGraphRepository).findByIdAndTenantId(graphId, testGraphDto.getTenantId());
        verify(validationService).validateGraph(testGraphDto);
        verify(agentGraphRepository, never()).save(any(AgentGraphEntity.class));
    }

    @Test
    void deleteGraph_ShouldDeleteGraph_WhenGraphExists() {
        // Given
        String graphId = "test-graph-id";
        String tenantId = "test-tenant";
        when(agentGraphRepository.findByIdAndTenantId(graphId, tenantId)).thenReturn(Optional.of(testGraphEntity));

        // When
        graphService.deleteGraph(graphId, tenantId);

        // Then
        verify(agentGraphRepository).findByIdAndTenantId(graphId, tenantId);
        verify(agentGraphRepository).delete(testGraphEntity);
    }

    @Test
    void deleteGraph_ShouldThrowException_WhenGraphNotFound() {
        // Given
        String graphId = "non-existent-id";
        String tenantId = "test-tenant";
        when(agentGraphRepository.findByIdAndTenantId(graphId, tenantId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(GraphService.GraphNotFoundException.class, 
                    () -> graphService.deleteGraph(graphId, tenantId));
        
        verify(agentGraphRepository).findByIdAndTenantId(graphId, tenantId);
        verify(agentGraphRepository, never()).delete(any(AgentGraphEntity.class));
    }

    @Test
    void submitForExecution_ShouldSubmitGraph_WhenValidationPasses() {
        // Given
        String graphId = "test-graph-id";
        String tenantId = "test-tenant";
        ValidationResult validationResult = new ValidationResult(true, List.of(), List.of());
        PlanEntity planA = new PlanEntity();
        planA.setName("PlanA");
        
        when(agentGraphRepository.findByIdAndTenantId(graphId, tenantId)).thenReturn(Optional.of(testGraphEntity));
        when(planRepository.findByAgentGraphIdWithFiles(graphId)).thenReturn(List.of(planA));
        when(taskRepository.findByAgentGraphIdWithFiles(graphId)).thenReturn(List.of());
        when(validationService.validateGraph(any(AgentGraphDto.class))).thenReturn(validationResult);
        when(agentGraphRepository.save(any(AgentGraphEntity.class))).thenReturn(testGraphEntity);
        when(graphRunRepository.save(any(GraphRunEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ExecutionResponse result = graphService.submitForExecution(graphId, tenantId);

        // Then
        assertNotNull(result);
        assertNotNull(result.getExecutionId());
        assertEquals("RUNNING", result.getStatus());
        assertTrue(result.getMessage().contains("entry plan"));
        
        verify(agentGraphRepository).findByIdAndTenantId(graphId, tenantId);
        verify(validationService).validateGraph(any(AgentGraphDto.class));
        verify(agentGraphRepository).save(argThat(graph -> graph.getStatus() == ai.eigloo.agentic.graph.entity.GraphStatus.ACTIVE));
        verify(graphRunRepository, times(2)).save(any(GraphRunEntity.class));
        verify(graphExecutionBootstrapPublisher).publishStartPlanInput(
                eq(tenantId), eq(graphId), anyString(), eq("PlanA"));
    }

    @Test
    void submitForExecution_ShouldThrowException_WhenValidationFails() {
        // Given
        String graphId = "test-graph-id";
        String tenantId = "test-tenant";
        ValidationResult validationResult = new ValidationResult(false, List.of("Validation error"), List.of());
        
        when(agentGraphRepository.findByIdAndTenantId(graphId, tenantId)).thenReturn(Optional.of(testGraphEntity));
        when(planRepository.findByAgentGraphIdWithFiles(graphId)).thenReturn(List.of());
        when(taskRepository.findByAgentGraphIdWithFiles(graphId)).thenReturn(List.of());
        when(validationService.validateGraph(any(AgentGraphDto.class))).thenReturn(validationResult);

        // When & Then
        assertThrows(GraphValidationException.class, 
                    () -> graphService.submitForExecution(graphId, tenantId));
        
        verify(agentGraphRepository).findByIdAndTenantId(graphId, tenantId);
        verify(validationService).validateGraph(any(AgentGraphDto.class));
        verifyNoInteractions(graphRunRepository);
    }

    @Test
    void submitForExecution_ShouldThrowException_WhenNoEntryPlanExists() {
        // Given
        String graphId = "test-graph-id";
        String tenantId = "test-tenant";
        ValidationResult validationResult = new ValidationResult(true, List.of(), List.of());

        when(agentGraphRepository.findByIdAndTenantId(graphId, tenantId)).thenReturn(Optional.of(testGraphEntity));
        when(planRepository.findByAgentGraphIdWithFiles(graphId)).thenReturn(List.of());
        when(taskRepository.findByAgentGraphIdWithFiles(graphId)).thenReturn(List.of());
        when(validationService.validateGraph(any(AgentGraphDto.class))).thenReturn(validationResult);

        // When + Then
        assertThrows(GraphValidationException.class, () -> graphService.submitForExecution(graphId, tenantId));
        verify(graphExecutionBootstrapPublisher, never()).publishStartPlanInput(anyString(), anyString(), anyString(), anyString());
        verifyNoInteractions(graphRunRepository);
    }

    @Test
    void submitForExecution_ShouldMarkRunFailed_WhenBootstrapPublishFails() {
        // Given
        String graphId = "test-graph-id";
        String tenantId = "test-tenant";
        ValidationResult validationResult = new ValidationResult(true, List.of(), List.of());
        PlanEntity planA = new PlanEntity();
        planA.setName("PlanA");

        when(agentGraphRepository.findByIdAndTenantId(graphId, tenantId)).thenReturn(Optional.of(testGraphEntity));
        when(planRepository.findByAgentGraphIdWithFiles(graphId)).thenReturn(List.of(planA));
        when(taskRepository.findByAgentGraphIdWithFiles(graphId)).thenReturn(List.of());
        when(validationService.validateGraph(any(AgentGraphDto.class))).thenReturn(validationResult);
        when(agentGraphRepository.save(any(AgentGraphEntity.class))).thenReturn(testGraphEntity);
        when(graphRunRepository.save(any(GraphRunEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("kafka down")).when(graphExecutionBootstrapPublisher)
                .publishStartPlanInput(eq(tenantId), eq(graphId), anyString(), eq("PlanA"));

        // When + Then
        assertThrows(IllegalStateException.class, () -> graphService.submitForExecution(graphId, tenantId));

        verify(graphRunRepository, atLeastOnce()).save(argThat(run -> run.getStatus() == GraphRunStatus.FAILED));
    }

    @Test
    void updateGraphStatus_ShouldUpdateStatus() {
        // Given
        String graphId = "test-graph-id";
        GraphStatusUpdate statusUpdate = new GraphStatusUpdate(GraphStatus.ACTIVE);
        
        when(agentGraphRepository.findById(graphId)).thenReturn(Optional.of(testGraphEntity));
        when(agentGraphRepository.save(any(AgentGraphEntity.class))).thenReturn(testGraphEntity);

        // When
        graphService.updateGraphStatus(graphId, statusUpdate);

        // Then
        verify(agentGraphRepository).findById(graphId);
        verify(agentGraphRepository).save(any(AgentGraphEntity.class));
    }

    @Test
    void updateGraphStatus_ShouldRejectIllegalTransition() {
        // Given
        String graphId = "test-graph-id";
        testGraphEntity.setStatus(ai.eigloo.agentic.graph.entity.GraphStatus.ACTIVE);
        GraphStatusUpdate statusUpdate = new GraphStatusUpdate(GraphStatus.NEW);

        when(agentGraphRepository.findById(graphId)).thenReturn(Optional.of(testGraphEntity));

        // When + Then
        assertThrows(IllegalArgumentException.class, () -> graphService.updateGraphStatus(graphId, statusUpdate));
        verify(agentGraphRepository).findById(graphId);
        verify(agentGraphRepository, never()).save(any(AgentGraphEntity.class));
    }

    @Test
    void submitForExecution_ShouldRejectArchivedGraph() {
        // Given
        String graphId = "test-graph-id";
        String tenantId = "test-tenant";
        testGraphEntity.setStatus(ai.eigloo.agentic.graph.entity.GraphStatus.ARCHIVED);

        when(agentGraphRepository.findByIdAndTenantId(graphId, tenantId)).thenReturn(Optional.of(testGraphEntity));

        // When + Then
        assertThrows(IllegalArgumentException.class, () -> graphService.submitForExecution(graphId, tenantId));
        verifyNoInteractions(graphRunRepository);
        verify(graphExecutionBootstrapPublisher, never())
                .publishStartPlanInput(anyString(), anyString(), anyString(), anyString());
    }
}
