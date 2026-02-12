package ai.eigloo.agentic.graph.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        Map<String, Plan> plans = new HashMap<>();
        Map<String, Task> tasks = new HashMap<>();

        Plan plan = Plan.of("test_plan", testPlanPath);
        Task task = Task.of("test_task", testTaskPath);

        plans.put("test_plan", plan);
        tasks.put("test_task", task);

        AgentGraph graph = AgentGraph.of(
                "TestGraph",
                plans,
                tasks,
                List.of(new GraphEdge("test_plan", GraphNodeType.PLAN, "test_task", GraphNodeType.TASK)));

        assertThat(graph.name()).isEqualTo("TestGraph");
        assertThat(graph.planCount()).isEqualTo(1);
        assertThat(graph.taskCount()).isEqualTo(1);
        assertThat(graph.totalNodeCount()).isEqualTo(2);
        assertThat(graph.isEmpty()).isFalse();
    }

    @Test
    void testImmutability() {
        Map<String, Plan> plans = new HashMap<>();
        Map<String, Task> tasks = new HashMap<>();

        Plan plan = Plan.of("test_plan", testPlanPath);
        Task task = Task.of("test_task", testTaskPath);

        plans.put("test_plan", plan);
        tasks.put("test_task", task);

        AgentGraph graph = AgentGraph.of(
                "TestGraph",
                plans,
                tasks,
                List.of(new GraphEdge("test_plan", GraphNodeType.PLAN, "test_task", GraphNodeType.TASK)));

        assertThatThrownBy(() -> graph.plans().put("new_plan", plan))
                .isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(() -> graph.tasks().put("new_task", task))
                .isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(() -> graph.edges().add(new GraphEdge("x", GraphNodeType.PLAN, "y", GraphNodeType.TASK)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testConvenienceMethods() {
        Plan planA = Plan.of("plan_a", testPlanPath.resolve("a"));
        Plan planB = Plan.of("plan_b", testPlanPath.resolve("b"));
        Task taskA = Task.of("task_a", testTaskPath.resolve("a"));

        AgentGraph graph = AgentGraph.of(
                "TestGraph",
                Map.of("plan_a", planA, "plan_b", planB),
                Map.of("task_a", taskA),
                List.of(
                        new GraphEdge("plan_a", GraphNodeType.PLAN, "task_a", GraphNodeType.TASK),
                        new GraphEdge("task_a", GraphNodeType.TASK, "plan_b", GraphNodeType.PLAN)));

        assertThat(graph.getPlan("plan_a")).isEqualTo(planA);
        assertThat(graph.getTask("task_a")).isEqualTo(taskA);
        assertThat(graph.getPlan("missing")).isNull();
        assertThat(graph.getTask("missing")).isNull();

        assertThat(graph.getDownstreamTasks("plan_a")).containsExactly("task_a");
        assertThat(graph.getUpstreamPlan("task_a")).isEqualTo("plan_a");
        assertThat(graph.getDownstreamPlan("task_a")).isEqualTo("plan_b");
        assertThat(graph.getUpstreamTasks("plan_b")).containsExactly("task_a");
    }

    @Test
    void testValidationInConstructor() {
        assertThatThrownBy(() -> AgentGraph.of(null, Map.of(), Map.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Graph name cannot be null");

        assertThatThrownBy(() -> AgentGraph.of("TestGraph", null, Map.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Plans map cannot be null");

        assertThatThrownBy(() -> AgentGraph.of("TestGraph", Map.of(), null, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tasks map cannot be null");

        assertThatThrownBy(() -> AgentGraph.of("TestGraph", Map.of(), Map.of(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Edges cannot be null");
    }

    @Test
    void testDefensiveCopying() {
        Map<String, Plan> plans = new HashMap<>();
        Map<String, Task> tasks = new HashMap<>();
        List<GraphEdge> edges = List.of(new GraphEdge("test_plan", GraphNodeType.PLAN, "test_task", GraphNodeType.TASK));

        Plan plan = Plan.of("test_plan", testPlanPath);
        Task task = Task.of("test_task", testTaskPath);

        plans.put("test_plan", plan);
        tasks.put("test_task", task);

        AgentGraph graph = AgentGraph.of("TestGraph", plans, tasks, edges);

        plans.put("new_plan", plan);
        tasks.put("new_task", task);

        assertThat(graph.planCount()).isEqualTo(1);
        assertThat(graph.taskCount()).isEqualTo(1);
        assertThat(graph.getAllPlanNames()).containsExactly("test_plan");
        assertThat(graph.getAllTaskNames()).containsExactly("test_task");
    }

    @Test
    void testEmptyGraph() {
        AgentGraph emptyGraph = AgentGraph.empty();

        assertThat(emptyGraph.name()).isEqualTo("EmptyGraph");
        assertThat(emptyGraph.isEmpty()).isTrue();
        assertThat(emptyGraph.planCount()).isEqualTo(0);
        assertThat(emptyGraph.taskCount()).isEqualTo(0);
        assertThat(emptyGraph.totalNodeCount()).isEqualTo(0);
        assertThat(emptyGraph.getAllPlanNames()).isEmpty();
        assertThat(emptyGraph.getAllTaskNames()).isEmpty();
    }
}
