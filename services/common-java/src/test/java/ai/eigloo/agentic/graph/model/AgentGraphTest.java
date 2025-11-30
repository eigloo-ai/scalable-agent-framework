package ai.eigloo.agentic.graph.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentGraphTest {
    
    @TempDir
    Path tempDir;
    
    private Path testPlanPath;
    private Path testTaskPath;
    
    @BeforeEach
    void setUp() {
        testPlanPath = tempDir.resolve("test_plan");
        testTaskPath = tempDir.resolve("test_task");
    }
    
    @Test
    void testValidConstruction() {
        // Given
        Map<String, Plan> plans = new HashMap<>();
        Map<String, Task> tasks = new HashMap<>();
        Map<String, Set<String>> planToTasks = new HashMap<>();
        Map<String, String> taskToPlan = new HashMap<>();
        
        Plan plan = Plan.of("test_plan", testPlanPath);
        Task task = Task.of("test_task", testTaskPath, "test_plan");
        
        plans.put("test_plan", plan);
        tasks.put("test_task", task);
        planToTasks.put("test_plan", Set.of("test_task"));
        taskToPlan.put("test_task", "test_plan");
        
        // When
        AgentGraph graph = AgentGraph.of("TestGraph", plans, tasks, planToTasks, taskToPlan);
        
        // Then
        assertThat(graph.name()).isEqualTo("TestGraph");
        assertThat(graph.planCount()).isEqualTo(1);
        assertThat(graph.taskCount()).isEqualTo(1);
        assertThat(graph.totalNodeCount()).isEqualTo(2);
        assertThat(graph.isEmpty()).isFalse();
    }
    
    @Test
    void testImmutability() {
        // Given
        Map<String, Plan> plans = new HashMap<>();
        Map<String, Task> tasks = new HashMap<>();
        Map<String, Set<String>> planToTasks = new HashMap<>();
        Map<String, String> taskToPlan = new HashMap<>();
        
        Plan plan = Plan.of("test_plan", testPlanPath);
        Task task = Task.of("test_task", testTaskPath, "test_plan");
        
        plans.put("test_plan", plan);
        tasks.put("test_task", task);
        planToTasks.put("test_plan", Set.of("test_task"));
        taskToPlan.put("test_task", "test_plan");
        
        AgentGraph graph = AgentGraph.of("TestGraph", plans, tasks, planToTasks, taskToPlan);
        
        // When & Then
        assertThatThrownBy(() -> graph.plans().put("new_plan", plan))
            .isInstanceOf(UnsupportedOperationException.class);
        
        assertThatThrownBy(() -> graph.tasks().put("new_task", task))
            .isInstanceOf(UnsupportedOperationException.class);
        
        assertThatThrownBy(() -> graph.planToTasks().put("new_plan", Set.of("test_task")))
            .isInstanceOf(UnsupportedOperationException.class);
        
        assertThatThrownBy(() -> graph.taskToPlan().put("new_task", "test_plan"))
            .isInstanceOf(UnsupportedOperationException.class);
    }
    
    @Test
    void testConvenienceMethods() {
        // Given
        Map<String, Plan> plans = new HashMap<>();
        Map<String, Task> tasks = new HashMap<>();
        Map<String, Set<String>> planToTasks = new HashMap<>();
        Map<String, String> taskToPlan = new HashMap<>();
        
        Plan plan = Plan.of("test_plan", testPlanPath);
        Task task = Task.of("test_task", testTaskPath, "test_plan");
        
        plans.put("test_plan", plan);
        tasks.put("test_task", task);
        planToTasks.put("test_plan", Set.of("test_task"));
        taskToPlan.put("test_task", "test_plan");
        
        AgentGraph graph = AgentGraph.of("TestGraph", plans, tasks, planToTasks, taskToPlan);
        
        // When & Then
        assertThat(graph.getPlan("test_plan")).isEqualTo(plan);
        assertThat(graph.getTask("test_task")).isEqualTo(task);
        assertThat(graph.getPlan("nonexistent")).isNull();
        assertThat(graph.getTask("nonexistent")).isNull();
        
        assertThat(graph.getDownstreamTasks("test_plan")).containsExactly("test_task");
        assertThat(graph.getUpstreamPlan("test_task")).isEqualTo("test_plan");
        assertThat(graph.getDownstreamTasks("nonexistent")).isEmpty();
        assertThat(graph.getUpstreamPlan("nonexistent")).isNull();
        
        assertThat(graph.getAllPlanNames()).containsExactly("test_plan");
        assertThat(graph.getAllTaskNames()).containsExactly("test_task");
    }
    
    @Test
    void testValidationInConstructor() {
        // Given & When & Then
        assertThatThrownBy(() -> AgentGraph.of(null, Map.of(), Map.of(), Map.of(), Map.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Graph name cannot be null");
        
        assertThatThrownBy(() -> AgentGraph.of("TestGraph", null, Map.of(), Map.of(), Map.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Plans map cannot be null");
        
        assertThatThrownBy(() -> AgentGraph.of("TestGraph", Map.of(), null, Map.of(), Map.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Tasks map cannot be null");
        
        assertThatThrownBy(() -> AgentGraph.of("TestGraph", Map.of(), Map.of(), null, Map.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Plan-to-tasks mapping cannot be null");
        
        assertThatThrownBy(() -> AgentGraph.of("TestGraph", Map.of(), Map.of(), Map.of(), null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Task-to-plan mapping cannot be null");
    }
    
    @Test
    void testDefensiveCopying() {
        // Given
        Map<String, Plan> plans = new HashMap<>();
        Map<String, Task> tasks = new HashMap<>();
        Map<String, Set<String>> planToTasks = new HashMap<>();
        Map<String, String> taskToPlan = new HashMap<>();
        
        Plan plan = Plan.of("test_plan", testPlanPath);
        Task task = Task.of("test_task", testTaskPath, "test_plan");
        
        plans.put("test_plan", plan);
        tasks.put("test_task", task);
        planToTasks.put("test_plan", Set.of("test_task"));
        taskToPlan.put("test_task", "test_plan");
        
        // When
        AgentGraph graph = AgentGraph.of("TestGraph", plans, tasks, planToTasks, taskToPlan);
        
        // Then - modifying original maps should not affect the graph
        plans.put("new_plan", plan);
        tasks.put("new_task", task);
        planToTasks.put("new_plan", Set.of("test_task"));
        taskToPlan.put("new_task", "test_plan");
        
        assertThat(graph.planCount()).isEqualTo(1);
        assertThat(graph.taskCount()).isEqualTo(1);
        assertThat(graph.getAllPlanNames()).containsExactly("test_plan");
        assertThat(graph.getAllTaskNames()).containsExactly("test_task");
    }
    
    @Test
    void testEmptyGraph() {
        // Given & When
        AgentGraph emptyGraph = AgentGraph.empty();
        
        // Then
        assertThat(emptyGraph.name()).isEqualTo("EmptyGraph");
        assertThat(emptyGraph.isEmpty()).isTrue();
        assertThat(emptyGraph.planCount()).isEqualTo(0);
        assertThat(emptyGraph.taskCount()).isEqualTo(0);
        assertThat(emptyGraph.totalNodeCount()).isEqualTo(0);
        assertThat(emptyGraph.getAllPlanNames()).isEmpty();
        assertThat(emptyGraph.getAllTaskNames()).isEmpty();
    }
    
    @Test
    void testEmptyGraphWithCustomName() {
        // Given & When
        AgentGraph emptyGraph = AgentGraph.empty("CustomEmptyGraph");
        
        // Then
        assertThat(emptyGraph.name()).isEqualTo("CustomEmptyGraph");
        assertThat(emptyGraph.isEmpty()).isTrue();
        assertThat(emptyGraph.planCount()).isEqualTo(0);
        assertThat(emptyGraph.taskCount()).isEqualTo(0);
    }
} 