package ai.eigloo.agentic.graphbuilder.validation;

import ai.eigloo.agentic.graph.model.AgentGraph;
import ai.eigloo.agentic.graph.model.Plan;
import ai.eigloo.agentic.graph.model.Task;
import ai.eigloo.agentic.graph.exception.GraphValidationException;
import ai.eigloo.agentic.graphbuilder.parser.GraphVizDotParser;
import ai.eigloo.agentic.graphbuilder.TestResourceUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GraphValidatorTest {
    
    @TempDir
    Path tempDir;
    
    private Path testPlanPath;
    private Path testTaskPath;
    private GraphVizDotParser parser;
    
    @BeforeEach
    void setUp() {
        testPlanPath = Paths.get("/test/plans/test_plan");
        testTaskPath = Paths.get("/test/tasks/test_task");
        parser = new GraphVizDotParser();
    }
    
    @Test
    void testValidateValidGraph() {
        // Given
        AgentGraph graph = createValidGraph();
        
        // When & Then
        assertThatNoException().isThrownBy(() -> GraphValidator.validate(graph));
    }
    
    @Test
    void testValidateValidGraphFromDotFile() throws Exception {
        // Given
        Path specDir = TestResourceUtils.getResourcePath("valid_graphs/simple_graph");
        AgentGraph graph = parser.parse(specDir);
        
        // When & Then
        assertThatNoException().isThrownBy(() -> GraphValidator.validate(graph));
    }
    
    @Test
    void testValidateGraphWithCyclesFromDotFile() throws Exception {
        // Given
        Path specDir = TestResourceUtils.getResourcePath("valid_graphs/cycle_graph");
        AgentGraph graph = parser.parse(specDir);
        
        // When & Then - cycles should be allowed
        assertThatNoException().isThrownBy(() -> GraphValidator.validate(graph));
    }
    
    @Test
    void testValidateMultiPlanGraphFromDotFile() throws Exception {
        // Given
        Path specDir = TestResourceUtils.getResourcePath("valid_graphs/multi_plan_graph");
        AgentGraph graph = parser.parse(specDir);
        
        // When & Then
        assertThatNoException().isThrownBy(() -> GraphValidator.validate(graph));
        
        // Verify the graph structure
        assertThat(graph.planCount()).isEqualTo(4);
        assertThat(graph.taskCount()).isEqualTo(6);
        assertThat(graph.getAllPlanNames()).containsExactlyInAnyOrder(
            "plan_data_ingestion", "plan_data_processing", "plan_analysis", "plan_reporting");
        assertThat(graph.getAllTaskNames()).containsExactlyInAnyOrder(
            "task_fetch_data", "task_validate_data", "task_transform_data", "task_analyze_data", 
            "task_generate_report", "task_send_notification");
    }
    
    @Test
    void testValidateParallelTasksGraphFromDotFile() throws Exception {
        // Given
        Path specDir = TestResourceUtils.getResourcePath("valid_graphs/parallel_tasks_graph");
        AgentGraph graph = parser.parse(specDir);
        
        // When & Then
        assertThatNoException().isThrownBy(() -> GraphValidator.validate(graph));
        
        // Verify the graph structure
        assertThat(graph.planCount()).isEqualTo(3);
        assertThat(graph.taskCount()).isEqualTo(5);
        assertThat(graph.getAllPlanNames()).containsExactlyInAnyOrder(
            "plan_data_collection", "plan_parallel_processing", "plan_aggregation");
        assertThat(graph.getAllTaskNames()).containsExactlyInAnyOrder(
            "task_fetch_user_data", "task_fetch_product_data", "task_process_user_data", 
            "task_process_product_data", "task_aggregate_results");
    }
    
    @Test
    void testValidateDirectoryStructure() throws Exception {
        // Given
        AgentGraph graph = createValidGraph();
        Path specDir = createValidDirectoryStructure();
        
        // When & Then
        assertThatNoException().isThrownBy(() -> GraphValidator.validateDirectoryStructure(specDir, graph));
    }
    
    @Test
    void testValidateDuplicateNames() {
        // Given
        AgentGraph graph = createGraphWithDuplicateNames();
        
        // When & Then
        assertThatThrownBy(() -> GraphValidator.validate(graph))
            .isInstanceOf(GraphValidationException.class)
            .hasMessageContaining("does not feed into any tasks");
    }
    
    @Test
    void testValidateDanglingEdges() {
        // Given
        AgentGraph graph = createGraphWithDanglingEdges();
        
        // When & Then
        assertThatThrownBy(() -> GraphValidator.validate(graph))
            .isInstanceOf(GraphValidationException.class)
            .hasMessageContaining("references non-existent");
    }
    
    @Test
    void testValidateTaskMultiplePlans() {
        // Given
        AgentGraph graph = createGraphWithTaskMultiplePlans();
        
        // When & Then
        assertThatThrownBy(() -> GraphValidator.validate(graph))
            .isInstanceOf(GraphValidationException.class)
            .hasMessageContaining("upstream plan mismatch");
    }
    
    @Test
    void testValidateOrphanedNodes() {
        // Given
        AgentGraph graph = createGraphWithOrphanedNodes();
        
        // When & Then
        assertThatThrownBy(() -> GraphValidator.validate(graph))
            .isInstanceOf(GraphValidationException.class)
            .hasMessageContaining("has no upstream plan");
    }
    
    @Test
    void testValidateMissingPythonFiles() throws Exception {
        // Given
        AgentGraph graph = createValidGraph();
        Path specDir = createDirectoryStructureWithMissingPythonFiles();
        
        // When & Then
        assertThatThrownBy(() -> GraphValidator.validateDirectoryStructure(specDir, graph))
            .isInstanceOf(GraphValidationException.class)
            .hasMessageContaining("Plan Python file does not exist");
    }
    
    @Test
    void testValidateInvalidNodeNames() {
        // Given
        AgentGraph graph = createGraphWithInvalidNodeNames();
        
        // When & Then
        assertThatThrownBy(() -> GraphValidator.validate(graph))
            .isInstanceOf(GraphValidationException.class)
            .hasMessageContaining("Task invalid.task.name has no upstream plan");
    }
    
    @Test
    void testValidateGraphWithCycles() {
        // Given
        AgentGraph graph = createGraphWithCycles();
        
        // When & Then - cycles should be allowed
        assertThatNoException().isThrownBy(() -> GraphValidator.validate(graph));
    }
    
    @Test
    void testValidatePlanDoesNotFeedIntoTasks() {
        // Given
        AgentGraph graph = createGraphWithPlanNotFeedingIntoTasks();
        
        // When & Then
        assertThatThrownBy(() -> GraphValidator.validate(graph))
            .isInstanceOf(GraphValidationException.class)
            .hasMessageContaining("does not feed into any tasks");
    }
    
    @Test
    void testValidateTaskHasNoUpstreamPlan() {
        // Given
        AgentGraph graph = createGraphWithTaskNoUpstreamPlan();
        
        // When & Then
        assertThatThrownBy(() -> GraphValidator.validate(graph))
            .isInstanceOf(GraphValidationException.class)
            .hasMessageContaining("has no upstream plan");
    }
    
    private AgentGraph createValidGraph() {
        Map<String, Plan> plans = new HashMap<>();
        Map<String, Task> tasks = new HashMap<>();
        Map<String, Set<String>> planToTasks = new HashMap<>();
        Map<String, String> taskToPlan = new HashMap<>();
        
        Plan plan1 = Plan.of("plan1", testPlanPath);
        Task task1 = Task.of("task1", testTaskPath, "plan1");
        
        plans.put("plan1", plan1);
        tasks.put("task1", task1);
        
        // plan1 feeds into task1
        planToTasks.put("plan1", Set.of("task1"));
        // task1 has plan1 as upstream
        taskToPlan.put("task1", "plan1");
        
        return AgentGraph.of("ValidGraph", plans, tasks, planToTasks, taskToPlan);
    }
    
    private AgentGraph createGraphWithDuplicateNames() {
        Map<String, Plan> plans = new HashMap<>();
        Map<String, Task> tasks = new HashMap<>();
        Map<String, Set<String>> planToTasks = new HashMap<>();
        Map<String, String> taskToPlan = new HashMap<>();
        
        Plan plan1 = Plan.of("duplicate_name", testPlanPath);
        Plan plan2 = Plan.of("duplicate_name", testPlanPath);
        Task task1 = Task.of("task1", testTaskPath, "plan1");
        
        plans.put("plan1", plan1);
        plans.put("plan2", plan2);
        tasks.put("task1", task1);
        
        planToTasks.put("plan1", Set.of("task1"));
        taskToPlan.put("task1", "plan1");
        
        return AgentGraph.of("DuplicateNamesGraph", plans, tasks, planToTasks, taskToPlan);
    }
    
    private AgentGraph createGraphWithDanglingEdges() {
        Map<String, Plan> plans = new HashMap<>();
        Map<String, Task> tasks = new HashMap<>();
        Map<String, Set<String>> planToTasks = new HashMap<>();
        Map<String, String> taskToPlan = new HashMap<>();
        
        Plan plan1 = Plan.of("plan1", testPlanPath);
        Task task1 = Task.of("task1", testTaskPath, "plan1");
        Task task2 = Task.of("task2", testTaskPath, "nonexistent_plan");
        
        plans.put("plan1", plan1);
        tasks.put("task1", task1);
        tasks.put("task2", task2);
        
        planToTasks.put("plan1", Set.of("task1", "nonexistent_task"));
        taskToPlan.put("task1", "plan1");
        taskToPlan.put("task2", "nonexistent_plan");
        
        return AgentGraph.of("DanglingEdgesGraph", plans, tasks, planToTasks, taskToPlan);
    }
    
    private AgentGraph createGraphWithTaskMultiplePlans() {
        Map<String, Plan> plans = new HashMap<>();
        Map<String, Task> tasks = new HashMap<>();
        Map<String, Set<String>> planToTasks = new HashMap<>();
        Map<String, String> taskToPlan = new HashMap<>();
        
        Plan plan1 = Plan.of("plan1", testPlanPath);
        Plan plan2 = Plan.of("plan2", testPlanPath);
        Task task1 = Task.of("task1", testTaskPath, "plan1"); // Task says plan1
        
        plans.put("plan1", plan1);
        plans.put("plan2", plan2);
        tasks.put("task1", task1);
        
        planToTasks.put("plan1", Set.of("task1"));
        planToTasks.put("plan2", Set.of("task1")); // But graph says plan2
        taskToPlan.put("task1", "plan2"); // And taskToPlan says plan2
        
        return AgentGraph.of("TaskMultiplePlansGraph", plans, tasks, planToTasks, taskToPlan);
    }
    
    private AgentGraph createGraphWithOrphanedNodes() {
        Map<String, Plan> plans = new HashMap<>();
        Map<String, Task> tasks = new HashMap<>();
        Map<String, Set<String>> planToTasks = new HashMap<>();
        Map<String, String> taskToPlan = new HashMap<>();
        
        Plan plan1 = Plan.of("plan1", testPlanPath);
        Plan plan2 = Plan.of("orphaned_plan", testPlanPath); // Orphaned
        Task task1 = Task.of("task1", testTaskPath, "plan1");
        Task task2 = Task.of("orphaned_task", testTaskPath, "plan1"); // Orphaned
        
        plans.put("plan1", plan1);
        plans.put("orphaned_plan", plan2);
        tasks.put("task1", task1);
        tasks.put("orphaned_task", task2);
        
        planToTasks.put("plan1", Set.of("task1"));
        taskToPlan.put("task1", "plan1");
        // Note: orphaned_plan and orphaned_task have no connections
        
        return AgentGraph.of("OrphanedNodesGraph", plans, tasks, planToTasks, taskToPlan);
    }
    
    private AgentGraph createGraphWithInvalidNodeNames() {
        Map<String, Plan> plans = new HashMap<>();
        Map<String, Task> tasks = new HashMap<>();
        Map<String, Set<String>> planToTasks = new HashMap<>();
        Map<String, String> taskToPlan = new HashMap<>();
        
        Plan plan1 = Plan.of("invalid-plan-name", testPlanPath); // Invalid name with hyphen
        Task task1 = Task.of("invalid.task.name", testTaskPath, "plan1"); // Invalid name with dots
        
        plans.put("plan1", plan1);
        tasks.put("task1", task1);
        
        planToTasks.put("plan1", Set.of("task1"));
        taskToPlan.put("task1", "plan1");
        
        return AgentGraph.of("InvalidNodeNamesGraph", plans, tasks, planToTasks, taskToPlan);
    }
    
    private AgentGraph createGraphWithCycles() {
        Map<String, Plan> plans = new HashMap<>();
        Map<String, Task> tasks = new HashMap<>();
        Map<String, Set<String>> planToTasks = new HashMap<>();
        Map<String, String> taskToPlan = new HashMap<>();
        
        Plan plan1 = Plan.of("plan1", testPlanPath);
        Plan plan2 = Plan.of("plan2", testPlanPath);
        Task task1 = Task.of("task1", testTaskPath, "plan2"); // task1's upstream is plan2
        Task task2 = Task.of("task2", testTaskPath, "plan1"); // task2's upstream is plan1
        
        plans.put("plan1", plan1);
        plans.put("plan2", plan2);
        tasks.put("task1", task1);
        tasks.put("task2", task2);
        
        // Create a cycle: plan1 -> task1 -> plan2 -> task2 -> plan1
        planToTasks.put("plan1", Set.of("task1"));
        planToTasks.put("plan2", Set.of("task2"));
        taskToPlan.put("task1", "plan2"); // task1's upstream is plan2
        taskToPlan.put("task2", "plan1"); // task2's upstream is plan1
        
        return AgentGraph.of("CyclesGraph", plans, tasks, planToTasks, taskToPlan);
    }
    
    private AgentGraph createGraphWithPlanNotFeedingIntoTasks() {
        Map<String, Plan> plans = new HashMap<>();
        Map<String, Task> tasks = new HashMap<>();
        Map<String, Set<String>> planToTasks = new HashMap<>();
        Map<String, String> taskToPlan = new HashMap<>();
        
        Plan plan1 = Plan.of("plan1", testPlanPath);
        Plan plan2 = Plan.of("plan2", testPlanPath); // This plan doesn't feed into any tasks
        Task task1 = Task.of("task1", testTaskPath, "plan1");
        
        plans.put("plan1", plan1);
        plans.put("plan2", plan2);
        tasks.put("task1", task1);
        
        planToTasks.put("plan1", Set.of("task1"));
        planToTasks.put("plan2", Set.of()); // plan2 feeds into no tasks
        taskToPlan.put("task1", "plan1");
        
        return AgentGraph.of("PlanNotFeedingIntoTasksGraph", plans, tasks, planToTasks, taskToPlan);
    }
    
    private AgentGraph createGraphWithTaskNoUpstreamPlan() {
        Map<String, Plan> plans = new HashMap<>();
        Map<String, Task> tasks = new HashMap<>();
        Map<String, Set<String>> planToTasks = new HashMap<>();
        Map<String, String> taskToPlan = new HashMap<>();
        
        Plan plan1 = Plan.of("plan1", testPlanPath);
        Task task1 = Task.of("task1", testTaskPath, "plan1");
        Task task2 = Task.of("task2", testTaskPath, "plan1"); // This task has no upstream plan
        
        plans.put("plan1", plan1);
        tasks.put("task1", task1);
        tasks.put("task2", task2);
        
        planToTasks.put("plan1", Set.of("task1"));
        taskToPlan.put("task1", "plan1");
        // task2 is not in taskToPlan, so it has no upstream plan
        
        return AgentGraph.of("TaskNoUpstreamPlanGraph", plans, tasks, planToTasks, taskToPlan);
    }
    
    private Path createValidDirectoryStructure() throws Exception {
        Path specDir = tempDir.resolve("valid_spec");
        Files.createDirectories(specDir);
        
        // Create plans directory
        Path plansDir = specDir.resolve("plans");
        Files.createDirectories(plansDir);
        createPlanDirectory(plansDir, "plan1");
        createPlanDirectory(plansDir, "plan2");
        
        // Create tasks directory
        Path tasksDir = specDir.resolve("tasks");
        Files.createDirectories(tasksDir);
        createTaskDirectory(tasksDir, "task1");
        createTaskDirectory(tasksDir, "task2");
        
        return specDir;
    }
    
    private Path createDirectoryStructureWithMissingPythonFiles() throws Exception {
        Path specDir = tempDir.resolve("missing_files_spec");
        Files.createDirectories(specDir);
        
        // Create plans directory without Python files
        Path plansDir = specDir.resolve("plans");
        Files.createDirectories(plansDir);
        Files.createDirectories(plansDir.resolve("plan1"));
        
        // Create tasks directory without Python files
        Path tasksDir = specDir.resolve("tasks");
        Files.createDirectories(tasksDir);
        Files.createDirectories(tasksDir.resolve("task1"));
        
        return specDir;
    }
    
    private void createPlanDirectory(Path plansDir, String planName) throws Exception {
        Path planDir = plansDir.resolve(planName);
        Files.createDirectories(planDir);
        
        // Create plan.py
        Path planPy = planDir.resolve("plan.py");
        Files.write(planPy, "def plan(upstream_results): return {}".getBytes());
        
        // Create requirements.txt
        Path requirements = planDir.resolve("requirements.txt");
        Files.write(requirements, "requests>=2.25.0".getBytes());
    }
    
    private void createTaskDirectory(Path tasksDir, String taskName) throws Exception {
        Path taskDir = tasksDir.resolve(taskName);
        Files.createDirectories(taskDir);
        
        // Create task.py
        Path taskPy = taskDir.resolve("task.py");
        Files.write(taskPy, "def execute(upstream_plan): return {}".getBytes());
        
        // Create requirements.txt
        Path requirements = taskDir.resolve("requirements.txt");
        Files.write(requirements, "pandas>=1.3.0".getBytes());
    }
    

} 