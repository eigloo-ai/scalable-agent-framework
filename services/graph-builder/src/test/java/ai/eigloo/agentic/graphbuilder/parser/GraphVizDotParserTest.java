package ai.eigloo.agentic.graphbuilder.parser;

import ai.eigloo.agentic.graph.model.AgentGraph;
import ai.eigloo.agentic.graph.model.Task;
import ai.eigloo.agentic.graph.exception.GraphParsingException;
import ai.eigloo.agentic.graph.exception.GraphValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class GraphVizDotParserTest {
    
    @TempDir
    Path tempDir;
    
    private GraphVizDotParser parser;
    
    @BeforeEach
    void setUp() {
        parser = new GraphVizDotParser();
    }
    
    @Test
    void testSupportsMethod() {
        // Given & When & Then
        assertThat(parser.supports(Paths.get("test.dot"))).isTrue();
        assertThat(parser.supports(Paths.get("test.gv"))).isFalse();
        assertThat(parser.supports(Paths.get("test.json"))).isFalse();
        assertThat(parser.supports(Paths.get("test.yaml"))).isFalse();
        assertThat(parser.supports(null)).isFalse();
    }
    
    @Test
    void testGetName() {
        // Given & When & Then
        assertThat(parser.getName()).isEqualTo("GraphViz DOT Parser");
    }
    
    @Test
    void testGetSupportedExtensions() {
        // Given & When
        String[] extensions = parser.getSupportedExtensions();
        
        // Then
        assertThat(extensions).containsExactlyInAnyOrder(".dot");
    }
    
    @Test
    void testParseValidGraph() throws Exception {
        // Given
        Path specDir = createValidGraphSpecification();
        
        // When
        AgentGraph graph = parser.parse(specDir);
        
        // Then
        assertThat(graph).isNotNull();
        assertThat(graph.planCount()).isEqualTo(2);
        assertThat(graph.taskCount()).isEqualTo(3);
        
        // Verify plan names and labels
        var plan1 = graph.getPlan("plan_data_collection");
        assertThat(plan1).isNotNull();
        assertThat(plan1.name()).isEqualTo("plan_data_collection");
        assertThat(plan1.label()).isEqualTo("Data Collection Plan");
        
        var plan2 = graph.getPlan("plan_analysis");
        assertThat(plan2).isNotNull();
        assertThat(plan2.name()).isEqualTo("plan_analysis");
        assertThat(plan2.label()).isEqualTo("Analysis Plan");
        
        // Verify task names and labels
        var task1 = graph.getTask("task_fetch_data");
        assertThat(task1).isNotNull();
        assertThat(task1.name()).isEqualTo("task_fetch_data");
        assertThat(task1.label()).isEqualTo("Fetch Data");
        
        var task2 = graph.getTask("task_process_data");
        assertThat(task2).isNotNull();
        assertThat(task2.name()).isEqualTo("task_process_data");
        assertThat(task2.label()).isEqualTo("Process Data");
        
        var task3 = graph.getTask("task_generate_report");
        assertThat(task3).isNotNull();
        assertThat(task3.name()).isEqualTo("task_generate_report");
        assertThat(task3.label()).isEqualTo("Generate Report");
        
        // Verify relationships
        assertThat(graph.getDownstreamTasks("plan_data_collection"))
            .containsExactlyInAnyOrder("task_fetch_data", "task_process_data");
        assertThat(graph.getDownstreamTasks("plan_analysis"))
            .containsExactlyInAnyOrder("task_generate_report");
        
        assertThat(graph.getUpstreamPlan("task_fetch_data")).isEqualTo("plan_data_collection");
        assertThat(graph.getUpstreamPlan("task_process_data")).isEqualTo("plan_data_collection");
        assertThat(graph.getUpstreamPlan("task_generate_report")).isEqualTo("plan_analysis");
    }
    
    @Test
    void testParseWithArbitraryNodeNames() throws Exception {
        // Given
        Path specDir = createArbitraryNodeNamesSpecification();
        
        // When
        AgentGraph graph = parser.parse(specDir);
        
        // Then
        assertThat(graph).isNotNull();
        assertThat(graph.planCount()).isEqualTo(1);
        assertThat(graph.taskCount()).isEqualTo(1);
        
        // Verify plan with arbitrary name
        var plan = graph.getPlan("dataCollectionPlan");
        assertThat(plan).isNotNull();
        assertThat(plan.name()).isEqualTo("dataCollectionPlan");
        assertThat(plan.label()).isEqualTo("Data Collection Plan");
        
        // Verify task with arbitrary name
        var task = graph.getTask("fetchDataTask");
        assertThat(task).isNotNull();
        assertThat(task.name()).isEqualTo("fetchDataTask");
        assertThat(task.label()).isEqualTo("Fetch Data Task");
        
        // Verify relationships
        assertThat(graph.getDownstreamTasks("dataCollectionPlan"))
            .containsExactlyInAnyOrder("fetchDataTask");
        
        assertThat(graph.getUpstreamPlan("fetchDataTask")).isEqualTo("dataCollectionPlan");
    }
    
    @Test
    void testIncomingPlanEdgesAreCorrectlyResolved() throws Exception {
        // Given
        Path specDir = createValidGraphSpecification();
        
        // When
        AgentGraph graph = parser.parse(specDir);
        
        // Then
        // Verify that each task has the correct incoming PLAN->TASK edge
        Task fetchDataTask = graph.getTask("task_fetch_data");
        assertThat(fetchDataTask).isNotNull();
        assertThat(graph.getUpstreamPlan(fetchDataTask.name())).isEqualTo("plan_data_collection");
        
        Task processDataTask = graph.getTask("task_process_data");
        assertThat(processDataTask).isNotNull();
        assertThat(graph.getUpstreamPlan(processDataTask.name())).isEqualTo("plan_data_collection");
        
        Task generateReportTask = graph.getTask("task_generate_report");
        assertThat(generateReportTask).isNotNull();
        assertThat(graph.getUpstreamPlan(generateReportTask.name())).isEqualTo("plan_analysis");
    }
    
    @Test
    void testTaskWithoutUpstreamPlanThrowsException() throws Exception {
        // Given
        Path specDir = createSpecificationWithOrphanedTask();
        
        // When & Then
        assertThatThrownBy(() -> parser.parse(specDir))
            .isInstanceOf(GraphValidationException.class)
            .hasMessageContaining("Task task_orphaned has no upstream plan");
    }
    
    @Test
    void testComplexGraphWithMultipleUpstreamPlans() throws Exception {
        // Given
        Path specDir = createComplexGraphSpecification();
        
        // When
        AgentGraph graph = parser.parse(specDir);
        
        // Then
        assertThat(graph.planCount()).isEqualTo(3);
        assertThat(graph.taskCount()).isEqualTo(4);
        
        // Verify upstream plans for each task through edges
        assertThat(graph.getUpstreamPlan("task_fetch_data")).isEqualTo("plan_data_collection");
        assertThat(graph.getUpstreamPlan("task_process_data")).isEqualTo("plan_data_collection");
        assertThat(graph.getUpstreamPlan("task_analyze_data")).isEqualTo("plan_analysis");
        assertThat(graph.getUpstreamPlan("task_generate_report")).isEqualTo("plan_reporting");
    }
    
    @Test
    void testTaskToPlanEdgesAreReflectedInPlanUpstreamTasks() throws Exception {
        // Given
        Path specDir = createGraphWithTaskToPlanEdges();
        
        // When
        AgentGraph graph = parser.parse(specDir);
        
        // Then
        assertThat(graph.planCount()).isEqualTo(3);
        assertThat(graph.taskCount()).isEqualTo(4);
        
        // Verify that plans have correct upstream tasks via TASK->PLAN edges
        var plan1 = graph.getPlan("plan_data_processing");
        assertThat(plan1).isNotNull();
        assertThat(graph.getUpstreamTasks(plan1.name())).containsExactlyInAnyOrder("task_fetch_data", "task_validate_data");
        
        var plan2 = graph.getPlan("plan_reporting");
        assertThat(plan2).isNotNull();
        assertThat(graph.getUpstreamTasks(plan2.name())).containsExactlyInAnyOrder("task_process_data");
        
        // Verify that tasks have correct upstream plans via PLAN->TASK edges
        assertThat(graph.getUpstreamPlan("task_fetch_data")).isEqualTo("plan_data_collection");
        assertThat(graph.getUpstreamPlan("task_validate_data")).isEqualTo("plan_data_collection");
        assertThat(graph.getUpstreamPlan("task_process_data")).isEqualTo("plan_data_processing");
        assertThat(graph.getUpstreamPlan("task_generate_report")).isEqualTo("plan_reporting");
    }
    
    @Test
    void testNameAndLabelFieldsAreSetCorrectly() throws Exception {
        // Given
        Path specDir = createValidGraphSpecification();
        
        // When
        AgentGraph graph = parser.parse(specDir);
        
        // Then
        // Verify Plan name and label fields
        var plan1 = graph.getPlan("plan_data_collection");
        assertThat(plan1).isNotNull();
        assertThat(plan1.name()).isEqualTo("plan_data_collection"); // Node name
        assertThat(plan1.label()).isEqualTo("Data Collection Plan"); // Label attribute
        
        var plan2 = graph.getPlan("plan_analysis");
        assertThat(plan2).isNotNull();
        assertThat(plan2.name()).isEqualTo("plan_analysis"); // Node name
        assertThat(plan2.label()).isEqualTo("Analysis Plan"); // Label attribute
        
        // Verify Task name and label fields
        var task1 = graph.getTask("task_fetch_data");
        assertThat(task1).isNotNull();
        assertThat(task1.name()).isEqualTo("task_fetch_data"); // Node name
        assertThat(task1.label()).isEqualTo("Fetch Data"); // Label attribute
        
        var task2 = graph.getTask("task_process_data");
        assertThat(task2).isNotNull();
        assertThat(task2.name()).isEqualTo("task_process_data"); // Node name
        assertThat(task2.label()).isEqualTo("Process Data"); // Label attribute
        
        var task3 = graph.getTask("task_generate_report");
        assertThat(task3).isNotNull();
        assertThat(task3.name()).isEqualTo("task_generate_report"); // Node name
        assertThat(task3.label()).isEqualTo("Generate Report"); // Label attribute
    }
    
    @Test
    void testNameAndLabelFieldsWithMissingLabels() throws Exception {
        // Given
        Path specDir = createGraphSpecificationWithoutLabels();
        
        // When
        AgentGraph graph = parser.parse(specDir);
        
        // Then
        // Verify Plan name and label fields (label should fallback to name)
        var plan1 = graph.getPlan("plan_data_collection");
        assertThat(plan1).isNotNull();
        assertThat(plan1.name()).isEqualTo("plan_data_collection"); // Node name
        assertThat(plan1.label()).isEqualTo("plan_data_collection"); // Fallback to name when no label
        
        var plan2 = graph.getPlan("plan_analysis");
        assertThat(plan2).isNotNull();
        assertThat(plan2.name()).isEqualTo("plan_analysis"); // Node name
        assertThat(plan2.label()).isEqualTo("plan_analysis"); // Fallback to name when no label
        
        // Verify Task name and label fields (label should fallback to name)
        var task1 = graph.getTask("task_fetch_data");
        assertThat(task1).isNotNull();
        assertThat(task1.name()).isEqualTo("task_fetch_data"); // Node name
        assertThat(task1.label()).isEqualTo("task_fetch_data"); // Fallback to name when no label
        
        var task2 = graph.getTask("task_process_data");
        assertThat(task2).isNotNull();
        assertThat(task2.name()).isEqualTo("task_process_data"); // Node name
        assertThat(task2.label()).isEqualTo("task_process_data"); // Fallback to name when no label
    }
    
    @Test
    void testParseInvalidDotSyntax() throws Exception {
        // Given
        Path specDir = createInvalidDotSpecification();
        
        // When & Then
        assertThatThrownBy(() -> parser.parse(specDir))
            .isInstanceOf(GraphParsingException.class)
            .hasMessageContaining("Error parsing DOT file");
    }
    
    @Test
    void testParseMissingDirectories() throws Exception {
        // Given
        Path specDir = createSpecificationWithoutDirectories();
        
        // When - parser should succeed since it no longer validates file system
        AgentGraph graph = parser.parse(specDir);
        
        // Then - should parse successfully without validation
        assertThat(graph).isNotNull();
        assertThat(graph.planCount()).isEqualTo(1);
        assertThat(graph.taskCount()).isEqualTo(1);
    }
    
    @Test
    void testParseMissingPythonFiles() throws Exception {
        // Given
        Path specDir = createSpecificationWithMissingPythonFiles();
        
        // When - parser should succeed since it no longer validates file system
        AgentGraph graph = parser.parse(specDir);
        
        // Then - should parse successfully without validation
        assertThat(graph).isNotNull();
        assertThat(graph.planCount()).isEqualTo(1);
        assertThat(graph.taskCount()).isEqualTo(1);
    }
    
    @Test
    void testGraphNameExtraction() throws Exception {
        // Given
        Path specDir = tempDir.resolve("graph_name_test");
        Files.createDirectories(specDir);
        
        // Create a DOT file with a specific graph name
        Path dotFile = specDir.resolve("agent_graph.dot");
        String dotContent = """
            digraph CustomGraphName {
                rankdir=TB;
                node [shape=box, style=filled];
                
                plan1 [label="Plan 1", type="plan"];
                task1 [label="Task 1", type="task"];
                
                plan1 -> task1;
            }
            """;
        Files.write(dotFile, dotContent.getBytes());
        
        // When
        AgentGraph graph = parser.parse(specDir);
        
        // Then
        assertThat(graph.name()).isEqualTo("CustomGraphName");
        assertThat(graph.planCount()).isEqualTo(1);
        assertThat(graph.taskCount()).isEqualTo(1);
    }
    
    @Test
    void testMissingGraphNameThrowsException() throws Exception {
        // Given
        Path specDir = tempDir.resolve("missing_graph_name");
        Files.createDirectories(specDir);
        
        // Create a DOT file without a graph name
        Path dotFile = specDir.resolve("agent_graph.dot");
        String dotContent = """
            digraph {
                rankdir=TB;
                node [shape=box, style=filled];
                
                plan1 [label="Plan 1", type="plan"];
                task1 [label="Task 1", type="task"];
                
                plan1 -> task1;
            }
            """;
        Files.write(dotFile, dotContent.getBytes());
        
        // When & Then
        assertThatThrownBy(() -> parser.parse(specDir))
            .isInstanceOf(GraphParsingException.class)
            .hasMessageContaining("Graph name is missing or empty")
            .hasMessageContaining("agent_graph.dot");
    }
    
    private Path createValidGraphSpecification() throws Exception {
        // Create the specification directory structure
        Path specDir = tempDir.resolve("valid_graph");
        Files.createDirectories(specDir);
        
        // Create the DOT file
        Path dotFile = specDir.resolve("agent_graph.dot");
        String dotContent = """
            digraph AgentGraph {
                rankdir=TB;
                node [shape=box, style=filled];
                
                plan_data_collection [label="Data Collection Plan", type="plan"];
                plan_analysis [label="Analysis Plan", type="plan"];
                
                task_fetch_data [label="Fetch Data", type="task"];
                task_process_data [label="Process Data", type="task"];
                task_generate_report [label="Generate Report", type="task"];
                
                plan_data_collection -> task_fetch_data;
                plan_data_collection -> task_process_data;
                plan_analysis -> task_generate_report;
            }
            """;
        Files.write(dotFile, dotContent.getBytes());
        
        // Create plans directory
        Path plansDir = specDir.resolve("plans");
        Files.createDirectories(plansDir);
        
        // Create plan directories with actual node names
        createPlanDirectory(plansDir, "plan_data_collection");
        createPlanDirectory(plansDir, "plan_analysis");
        
        // Create tasks directory
        Path tasksDir = specDir.resolve("tasks");
        Files.createDirectories(tasksDir);
        
        // Create task directories with actual node names
        createTaskDirectory(tasksDir, "task_fetch_data");
        createTaskDirectory(tasksDir, "task_process_data");
        createTaskDirectory(tasksDir, "task_generate_report");
        
        return specDir;
    }
    
    private Path createSpecificationWithOrphanedTask() throws Exception {
        // Create the specification directory structure
        Path specDir = tempDir.resolve("orphaned_task");
        Files.createDirectories(specDir);
        
        // Create the DOT file with an orphaned task (no incoming edges)
        Path dotFile = specDir.resolve("agent_graph.dot");
        String dotContent = """
            digraph AgentGraph {
                rankdir=TB;
                node [shape=box, style=filled];
                
                plan_data_collection [label="Data Collection Plan", type="plan"];
                task_fetch_data [label="Fetch Data", type="task"];
                task_orphaned [label="Orphaned Task", type="task"];
                
                plan_data_collection -> task_fetch_data;
                // task_orphaned has no incoming edges
            }
            """;
        Files.write(dotFile, dotContent.getBytes());
        
        // Create plans directory
        Path plansDir = specDir.resolve("plans");
        Files.createDirectories(plansDir);
        createPlanDirectory(plansDir, "plan_data_collection");
        
        // Create tasks directory
        Path tasksDir = specDir.resolve("tasks");
        Files.createDirectories(tasksDir);
        createTaskDirectory(tasksDir, "task_fetch_data");
        createTaskDirectory(tasksDir, "task_orphaned");
        
        return specDir;
    }
    
    private Path createComplexGraphSpecification() throws Exception {
        // Create the specification directory structure
        Path specDir = tempDir.resolve("complex_graph");
        Files.createDirectories(specDir);
        
        // Create the DOT file with multiple plans and tasks
        Path dotFile = specDir.resolve("agent_graph.dot");
        String dotContent = """
            digraph AgentGraph {
                rankdir=TB;
                node [shape=box, style=filled];
                
                plan_data_collection [label="Data Collection Plan", type="plan"];
                plan_analysis [label="Analysis Plan", type="plan"];
                plan_reporting [label="Reporting Plan", type="plan"];
                
                task_fetch_data [label="Fetch Data", type="task"];
                task_process_data [label="Process Data", type="task"];
                task_analyze_data [label="Analyze Data", type="task"];
                task_generate_report [label="Generate Report", type="task"];
                
                plan_data_collection -> task_fetch_data;
                plan_data_collection -> task_process_data;
                plan_analysis -> task_analyze_data;
                plan_reporting -> task_generate_report;
            }
            """;
        Files.write(dotFile, dotContent.getBytes());
        
        // Create plans directory
        Path plansDir = specDir.resolve("plans");
        Files.createDirectories(plansDir);
        createPlanDirectory(plansDir, "plan_data_collection");
        createPlanDirectory(plansDir, "plan_analysis");
        createPlanDirectory(plansDir, "plan_reporting");
        
        // Create tasks directory
        Path tasksDir = specDir.resolve("tasks");
        Files.createDirectories(tasksDir);
        createTaskDirectory(tasksDir, "task_fetch_data");
        createTaskDirectory(tasksDir, "task_process_data");
        createTaskDirectory(tasksDir, "task_analyze_data");
        createTaskDirectory(tasksDir, "task_generate_report");
        
        return specDir;
    }
    
    private Path createInvalidDotSpecification() throws Exception {
        Path specDir = tempDir.resolve("invalid_graph");
        Files.createDirectories(specDir);
        
        // Create malformed DOT file
        Path dotFile = specDir.resolve("agent_graph.dot");
        String dotContent = """
            digraph AgentGraph {
                // Missing closing brace
                plan_data_collection [label="Data Collection Plan"];
                task_fetch_data [label="Fetch Data"];
                plan_data_collection -> task_fetch_data
            // Missing closing brace
            """;
        Files.write(dotFile, dotContent.getBytes());
        
        return specDir;
    }
    
    private Path createSpecificationWithoutDirectories() throws Exception {
        Path specDir = tempDir.resolve("no_directories");
        Files.createDirectories(specDir);
        
        // Create DOT file but no plans/ or tasks/ directories
        Path dotFile = specDir.resolve("agent_graph.dot");
        String dotContent = """
            digraph AgentGraph {
                plan_data_collection [label="Data Collection Plan", type="plan"];
                task_fetch_data [label="Fetch Data", type="task"];
                plan_data_collection -> task_fetch_data;
            }
            """;
        Files.write(dotFile, dotContent.getBytes());
        
        return specDir;
    }
    
    private Path createSpecificationWithMissingPythonFiles() throws Exception {
        Path specDir = tempDir.resolve("missing_python_files");
        Files.createDirectories(specDir);
        
        // Create DOT file
        Path dotFile = specDir.resolve("agent_graph.dot");
        String dotContent = """
            digraph AgentGraph {
                plan_data_collection [label="Data Collection Plan", type="plan"];
                task_fetch_data [label="Fetch Data", type="task"];
                plan_data_collection -> task_fetch_data;
            }
            """;
        Files.write(dotFile, dotContent.getBytes());
        
        // Create directories but without Python files
        Path plansDir = specDir.resolve("plans");
        Files.createDirectories(plansDir);
        Files.createDirectories(plansDir.resolve("data_collection"));
        
        Path tasksDir = specDir.resolve("tasks");
        Files.createDirectories(tasksDir);
        Files.createDirectories(tasksDir.resolve("fetch_data"));
        
        return specDir;
    }
    
    private void createPlanDirectory(Path plansDir, String planName) throws Exception {
        Path planDir = plansDir.resolve(planName);
        Files.createDirectories(planDir);
        
        // Create plan.py
        Path planPy = planDir.resolve("plan.py");
        String planContent = """
            def plan(upstream_results):
                return {"data": "test"}
            """;
        Files.write(planPy, planContent.getBytes());
        
        // Create requirements.txt
        Path requirements = planDir.resolve("requirements.txt");
        Files.write(requirements, "requests>=2.25.0\n".getBytes());
    }
    
    private void createTaskDirectory(Path tasksDir, String taskName) throws Exception {
        Path taskDir = tasksDir.resolve(taskName);
        Files.createDirectories(taskDir);
        
        // Create task.py
        Path taskPy = taskDir.resolve("task.py");
        String taskContent = """
            def execute(upstream_plan):
                return {"data": "test"}
            """;
        Files.write(taskPy, taskContent.getBytes());
        
        // Create requirements.txt
        Path requirements = taskDir.resolve("requirements.txt");
        Files.write(requirements, "pandas>=1.3.0\n".getBytes());
    }
    
    private Path createGraphWithTaskToPlanEdges() throws Exception {
        // Create the specification directory structure
        Path specDir = tempDir.resolve("task_to_plan_graph");
        Files.createDirectories(specDir);
        
        // Create the DOT file with task-to-plan edges
        Path dotFile = specDir.resolve("agent_graph.dot");
        String dotContent = """
            digraph AgentGraph {
                rankdir=TB;
                node [shape=box, style=filled];
                
                plan_data_collection [label="Data Collection Plan", type="plan"];
                plan_data_processing [label="Data Processing Plan", type="plan"];
                plan_reporting [label="Reporting Plan", type="plan"];
                
                task_fetch_data [label="Fetch Data", type="task"];
                task_validate_data [label="Validate Data", type="task"];
                task_process_data [label="Process Data", type="task"];
                task_generate_report [label="Generate Report", type="task"];
                
                plan_data_collection -> task_fetch_data;
                plan_data_collection -> task_validate_data;
                plan_data_processing -> task_process_data;
                plan_reporting -> task_generate_report;
                
                task_fetch_data -> plan_data_processing;
                task_validate_data -> plan_data_processing;
                task_process_data -> plan_reporting;
            }
            """;
        Files.write(dotFile, dotContent.getBytes());
        
        // Create plans directory
        Path plansDir = specDir.resolve("plans");
        Files.createDirectories(plansDir);
        createPlanDirectory(plansDir, "plan_data_collection");
        createPlanDirectory(plansDir, "plan_data_processing");
        createPlanDirectory(plansDir, "plan_reporting");
        
        // Create tasks directory
        Path tasksDir = specDir.resolve("tasks");
        Files.createDirectories(tasksDir);
        createTaskDirectory(tasksDir, "task_fetch_data");
        createTaskDirectory(tasksDir, "task_validate_data");
        createTaskDirectory(tasksDir, "task_process_data");
        createTaskDirectory(tasksDir, "task_generate_report");
        
        return specDir;
    }
    
    private Path createGraphSpecificationWithoutLabels() throws Exception {
        // Create the specification directory structure
        Path specDir = tempDir.resolve("no_labels_graph");
        Files.createDirectories(specDir);
        
        // Create the DOT file without labels
        Path dotFile = specDir.resolve("agent_graph.dot");
        String dotContent = """
            digraph AgentGraph {
                rankdir=TB;
                node [shape=box, style=filled];
                
                plan_data_collection [type="plan"];
                plan_analysis [type="plan"];
                
                task_fetch_data [type="task"];
                task_process_data [type="task"];
                
                plan_data_collection -> task_fetch_data;
                plan_analysis -> task_process_data;
            }
            """;
        Files.write(dotFile, dotContent.getBytes());
        
        // Create plans directory
        Path plansDir = specDir.resolve("plans");
        Files.createDirectories(plansDir);
        createPlanDirectory(plansDir, "plan_data_collection");
        createPlanDirectory(plansDir, "plan_analysis");
        
        // Create tasks directory
        Path tasksDir = specDir.resolve("tasks");
        Files.createDirectories(tasksDir);
        createTaskDirectory(tasksDir, "task_fetch_data");
        createTaskDirectory(tasksDir, "task_process_data");
        
        return specDir;
    }

    private Path createArbitraryNodeNamesSpecification() throws Exception {
        // Create the specification directory structure
        Path specDir = tempDir.resolve("arbitrary_node_names");
        Files.createDirectories(specDir);

        // Create the DOT file with arbitrary node names
        Path dotFile = specDir.resolve("agent_graph.dot");
        String dotContent = """
            digraph AgentGraph {
                rankdir=TB;
                node [shape=box, style=filled];
                
                dataCollectionPlan [label="Data Collection Plan", type="plan"];
                fetchDataTask [label="Fetch Data Task", type="task"];
                
                dataCollectionPlan -> fetchDataTask;
            }
            """;
        Files.write(dotFile, dotContent.getBytes());

        // Create plans directory
        Path plansDir = specDir.resolve("plans");
        Files.createDirectories(plansDir);
        createPlanDirectory(plansDir, "dataCollectionPlan");

        // Create tasks directory
        Path tasksDir = specDir.resolve("tasks");
        Files.createDirectories(tasksDir);
        createTaskDirectory(tasksDir, "fetchDataTask");

        return specDir;
    }
}
