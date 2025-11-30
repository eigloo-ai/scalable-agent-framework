package ai.eigloo.agentic.graph.service;

import ai.eigloo.agentic.graph.model.AgentGraph;
import ai.eigloo.agentic.graph.model.Plan;
import ai.eigloo.agentic.graph.model.Task;
import ai.eigloo.agentic.graph.entity.AgentGraphEntity;
import ai.eigloo.agentic.graph.entity.PlanEntity;
import ai.eigloo.agentic.graph.entity.TaskEntity;
import ai.eigloo.agentic.graph.exception.GraphPersistenceException;
import ai.eigloo.agentic.graph.repository.AgentGraphRepository;
import ai.eigloo.agentic.graph.repository.PlanRepository;
import ai.eigloo.agentic.graph.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GraphPersistenceService.
 */
class GraphPersistenceServiceTest {
    
    @Mock
    private AgentGraphRepository agentGraphRepository;
    
    @Mock
    private PlanRepository planRepository;
    
    @Mock
    private TaskRepository taskRepository;
    
    private GraphPersistenceService graphPersistenceService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        graphPersistenceService = new GraphPersistenceService(
            agentGraphRepository, planRepository, taskRepository);
    }
    
    @Test
    void persistGraph_ValidGraph_ShouldPersistSuccessfully() {
        // Given
        String tenantId = "test-tenant";
        
        // Create test graph
        Plan plan = new Plan("test_plan", "Test Plan", Path.of("/plans/test_plan"), Set.of("task1"), java.util.List.of());
        Task task = new Task("task1", "Task 1", Path.of("/tasks/task1"), "test_plan", java.util.List.of());
        
        Map<String, Plan> plans = Map.of("test_plan", plan);
        Map<String, Task> tasks = Map.of("task1", task);
        Map<String, Set<String>> planToTasks = Map.of("test_plan", Set.of("task1"));
        Map<String, String> taskToPlan = Map.of("task1", "test_plan");
        
        AgentGraph graph = new AgentGraph("test_graph", plans, tasks, planToTasks, taskToPlan);
        
        // Mock repository behavior
        when(agentGraphRepository.findByTenantIdAndName(tenantId, "test_graph"))
            .thenReturn(Optional.empty());
        when(agentGraphRepository.save(any(AgentGraphEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(planRepository.save(any(PlanEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(taskRepository.save(any(TaskEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        String graphId = graphPersistenceService.persistGraph(graph, tenantId);
        
        // Then
        assertThat(graphId).isNotNull();
        
        // Verify repository interactions
        verify(agentGraphRepository).save(any(AgentGraphEntity.class));
        verify(planRepository, times(1)).save(any(PlanEntity.class)); // Once for creation
        verify(taskRepository, times(2)).save(any(TaskEntity.class)); // Once for creation, once for relationship update
        
        // Verify graph entity creation
        ArgumentCaptor<AgentGraphEntity> graphCaptor = ArgumentCaptor.forClass(AgentGraphEntity.class);
        verify(agentGraphRepository).save(graphCaptor.capture());
        AgentGraphEntity savedGraph = graphCaptor.getValue();
        assertThat(savedGraph.getName()).isEqualTo("test_graph");
        assertThat(savedGraph.getTenantId()).isEqualTo(tenantId);
        // processId is no longer persisted on the graph entity
    }
    
    @Test
    void persistGraph_ExistingGraph_ShouldUpdateGraph() {
        // Given
        String tenantId = "test-tenant";
        String existingGraphId = "existing-graph-id";
        
        // Create test graph
        Plan plan = new Plan("test_plan", "Test Plan", Path.of("/plans/test_plan"), Set.of(), java.util.List.of());
        Task task = new Task("task1", "Task 1", Path.of("/tasks/task1"), "test_plan", java.util.List.of());
        
        Map<String, Plan> plans = Map.of("test_plan", plan);
        Map<String, Task> tasks = Map.of("task1", task);
        Map<String, Set<String>> planToTasks = Map.of("test_plan", Set.of("task1"));
        Map<String, String> taskToPlan = Map.of("task1", "test_plan");
        
        AgentGraph graph = new AgentGraph("test_graph", plans, tasks, planToTasks, taskToPlan);
        
        // Mock existing graph
        AgentGraphEntity existingGraph = new AgentGraphEntity(existingGraphId, tenantId, "test_graph");
        when(agentGraphRepository.findByTenantIdAndName(tenantId, "test_graph"))
            .thenReturn(Optional.of(existingGraph));
        when(agentGraphRepository.save(any(AgentGraphEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(planRepository.save(any(PlanEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(taskRepository.save(any(TaskEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        String graphId = graphPersistenceService.persistGraph(graph, tenantId);
        
        // Then
        assertThat(graphId).isNotNull();
        
        // Verify deletion of existing graph data
        verify(taskRepository).deleteByAgentGraphId(existingGraphId);
        verify(planRepository).deleteByAgentGraphId(existingGraphId);
        verify(agentGraphRepository).deleteById(existingGraphId);
        
        // Verify new graph creation
        verify(agentGraphRepository).save(any(AgentGraphEntity.class)); // Only once for create
    }
    
    @Test
    void persistGraph_NullGraph_ShouldThrowException() {
        // Given
        String tenantId = "test-tenant";
        
        // When & Then
        assertThatThrownBy(() -> graphPersistenceService.persistGraph(null, tenantId))
            .isInstanceOf(GraphPersistenceException.class)
            .hasMessageContaining("Failed to persist graph");
    }
    
    @Test
    void persistGraph_NullTenantId_ShouldThrowException() {
        // Given
        Plan plan = new Plan("test_plan", "Test Plan", Path.of("/plans/test_plan"), Set.of(), java.util.List.of());
        Map<String, Plan> plans = Map.of("test_plan", plan);
        AgentGraph graph = new AgentGraph("test_graph", plans, Map.of(), Map.of(), Map.of());
        
        // When & Then
        assertThatThrownBy(() -> graphPersistenceService.persistGraph(graph, null))
            .isInstanceOf(GraphPersistenceException.class)
            .hasMessageContaining("Tenant ID cannot be null or empty");
    }
    
    @Test
    void persistGraph_EmptyTenantId_ShouldThrowException() {
        // Given
        Plan plan = new Plan("test_plan", "Test Plan", Path.of("/plans/test_plan"), Set.of(), java.util.List.of());
        Map<String, Plan> plans = Map.of("test_plan", plan);
        AgentGraph graph = new AgentGraph("test_graph", plans, Map.of(), Map.of(), Map.of());
        
        // When & Then
        assertThatThrownBy(() -> graphPersistenceService.persistGraph(graph, ""))
            .isInstanceOf(GraphPersistenceException.class)
            .hasMessageContaining("Tenant ID cannot be null or empty");
    }
    
    @Test
    void persistGraph_RepositoryException_ShouldThrowGraphPersistenceException() {
        // Given
        String tenantId = "test-tenant";
        
        Plan plan = new Plan("test_plan", "Test Plan", Path.of("/plans/test_plan"), Set.of(), java.util.List.of());
        Map<String, Plan> plans = Map.of("test_plan", plan);
        AgentGraph graph = new AgentGraph("test_graph", plans, Map.of(), Map.of(), Map.of());
        
        // Mock repository to throw exception
        when(agentGraphRepository.findByTenantIdAndName(tenantId, "test_graph"))
            .thenReturn(Optional.empty());
        when(agentGraphRepository.save(any(AgentGraphEntity.class)))
            .thenThrow(new RuntimeException("Database error"));
        
        // When & Then
        assertThatThrownBy(() -> graphPersistenceService.persistGraph(graph, tenantId))
            .isInstanceOf(GraphPersistenceException.class)
            .hasMessageContaining("Failed to persist graph");
    }
    
    @Test
    void getPersistedGraph_ExistingGraph_ShouldReturnGraph() {
        // Given
        String graphId = "test-graph-id";
        String tenantId = "test-tenant";
        
        // Mock entities
        AgentGraphEntity graphEntity = new AgentGraphEntity(graphId, tenantId, "test_graph");
        
        PlanEntity planEntity = new PlanEntity("plan-id", "test_plan", "Test Plan", "/plans/test_plan", graphEntity);
        TaskEntity taskEntity = new TaskEntity("task-id", "task1", "Task 1", "/tasks/task1", graphEntity, planEntity);
        // Relationship is set via TaskEntity constructor (upstreamPlan parameter)
        
        when(agentGraphRepository.findById(graphId)).thenReturn(Optional.of(graphEntity));
        when(planRepository.findByAgentGraphId(graphId)).thenReturn(List.of(planEntity));
        when(taskRepository.findByAgentGraphId(graphId)).thenReturn(List.of(taskEntity));
        
        // When
        AgentGraph result = graphPersistenceService.getPersistedGraph(graphId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("test_graph");
        assertThat(result.plans()).hasSize(1);
        assertThat(result.tasks()).hasSize(1);
        assertThat(result.plans().get("test_plan")).isNotNull();
        assertThat(result.tasks().get("task1")).isNotNull();
    }
    
    @Test
    void getPersistedGraph_NonExistentGraph_ShouldThrowException() {
        // Given
        String graphId = "non-existent-id";
        when(agentGraphRepository.findById(graphId)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> graphPersistenceService.getPersistedGraph(graphId))
            .isInstanceOf(GraphPersistenceException.class)
            .hasMessageContaining("Graph not found with ID");
    }
    
    @Test
    void getPersistedGraph_NullGraphId_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> graphPersistenceService.getPersistedGraph(null))
            .isInstanceOf(GraphPersistenceException.class)
            .hasMessageContaining("Graph ID cannot be null or empty");
    }
    
    @Test
    void getPersistedGraph_EmptyGraphId_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> graphPersistenceService.getPersistedGraph(""))
            .isInstanceOf(GraphPersistenceException.class)
            .hasMessageContaining("Graph ID cannot be null or empty");
    }
    
    @Test
    void listPersistedGraphs_ExistingGraphs_ShouldReturnGraphInfoList() {
        // Given
        String tenantId = "test-tenant";
        LocalDateTime now = LocalDateTime.now();
        
        List<AgentGraphEntity> mockGraphs = Arrays.asList(
            createMockGraphEntity("id1", "graph1", tenantId, now.minusHours(2), now.minusHours(1), "process1"),
            createMockGraphEntity("id2", "graph2", tenantId, now.minusHours(1), now, "process2")
        );
        
        when(agentGraphRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)).thenReturn(mockGraphs);
        
        // When
        List<GraphPersistenceService.GraphInfo> result = graphPersistenceService.listPersistedGraphs(tenantId);
        
        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("graph1");
        assertThat(result.get(0).tenantId()).isEqualTo(tenantId);
        // processId no longer part of GraphInfo
        assertThat(result.get(1).name()).isEqualTo("graph2");
    }
    
    @Test
    void listPersistedGraphs_NoGraphs_ShouldReturnEmptyList() {
        // Given
        String tenantId = "test-tenant";
        when(agentGraphRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)).thenReturn(Arrays.asList());
        
        // When
        List<GraphPersistenceService.GraphInfo> result = graphPersistenceService.listPersistedGraphs(tenantId);
        
        // Then
        assertThat(result).isEmpty();
    }
    
    @Test
    void listPersistedGraphs_NullTenantId_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> graphPersistenceService.listPersistedGraphs(null))
            .isInstanceOf(GraphPersistenceException.class)
            .hasMessageContaining("Tenant ID cannot be null or empty");
    }
    
    @Test
    void listPersistedGraphs_EmptyTenantId_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> graphPersistenceService.listPersistedGraphs(""))
            .isInstanceOf(GraphPersistenceException.class)
            .hasMessageContaining("Tenant ID cannot be null or empty");
    }
    
    @Test
    void listPersistedGraphs_RepositoryException_ShouldThrowGraphPersistenceException() {
        // Given
        String tenantId = "test-tenant";
        when(agentGraphRepository.findByTenantIdOrderByCreatedAtDesc(tenantId))
            .thenThrow(new RuntimeException("Database error"));
        
        // When & Then
        assertThatThrownBy(() -> graphPersistenceService.listPersistedGraphs(tenantId))
            .isInstanceOf(GraphPersistenceException.class)
            .hasMessageContaining("Failed to list graphs");
    }
    
    /**
     * Helper method to create mock AgentGraphEntity.
     */
    private AgentGraphEntity createMockGraphEntity(String id, String name, String tenantId, 
                                                  LocalDateTime createdAt, LocalDateTime updatedAt, String processId) {
        AgentGraphEntity entity = new AgentGraphEntity(id, tenantId, name);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(updatedAt);
        return entity;
    }
}