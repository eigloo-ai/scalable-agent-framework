package ai.eigloo.agentic.graphbuilder;

import ai.eigloo.agentic.graphbuilder.parser.GraphParser;
import ai.eigloo.agentic.graph.model.AgentGraph;
import ai.eigloo.agentic.graph.exception.GraphParsingException;
import ai.eigloo.agentic.graph.exception.GraphValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class GraphBuilderServiceTest {
    
    @TempDir
    Path tempDir;
    
    private GraphBuilderService service;
    
    @BeforeEach
    void setUp() {
        service = new GraphBuilderService();
    }
    
    @Test
    void testBuildValidGraph() throws Exception {
        // Given
        Path specDir = createValidGraphSpecification();
        
        // When
        AgentGraph graph = service.buildGraph(specDir);
        
        // Then
        assertThat(graph).isNotNull();
        assertThat(graph.planCount()).isEqualTo(2);
        assertThat(graph.taskCount()).isEqualTo(3);
        assertThat(graph.getAllPlanNames()).containsExactlyInAnyOrder("plan_data_collection", "plan_analysis");
        assertThat(graph.getAllTaskNames()).containsExactlyInAnyOrder("task_fetch_data", "task_process_data", "task_generate_report");
    }
    
    @Test
    void testBuildValidGraphFromDotFile() throws Exception {
        // Given
        Path specDir = TestResourceUtils.getResourcePath("valid_graphs/simple_graph");
        
        // When
        AgentGraph graph = service.buildGraph(specDir);
        
        // Then
        assertThat(graph).isNotNull();
        assertThat(graph.planCount()).isEqualTo(2);
        assertThat(graph.taskCount()).isEqualTo(3);
        assertThat(graph.getAllPlanNames()).containsExactlyInAnyOrder("plan_data_collection", "plan_analysis");
        assertThat(graph.getAllTaskNames()).containsExactlyInAnyOrder("task_fetch_data", "task_process_data", "task_generate_report");
    }
    
    @Test
    void testBuildCycleGraphFromDotFile() throws Exception {
        // Given
        Path specDir = TestResourceUtils.getResourcePath("valid_graphs/cycle_graph");
        
        // When
        AgentGraph graph = service.buildGraph(specDir);
        
        // Then
        assertThat(graph).isNotNull();
        assertThat(graph.planCount()).isEqualTo(2);
        assertThat(graph.taskCount()).isEqualTo(2);
        assertThat(graph.getAllPlanNames()).containsExactlyInAnyOrder("plan1", "plan2");
        assertThat(graph.getAllTaskNames()).containsExactlyInAnyOrder("task1", "task2");
    }
    
    @Test
    void testBuildMultiPlanGraphFromDotFile() throws Exception {
        // Given
        Path specDir = TestResourceUtils.getResourcePath("valid_graphs/multi_plan_graph");
        
        // When
        AgentGraph graph = service.buildGraph(specDir);
        
        // Then
        assertThat(graph).isNotNull();
        assertThat(graph.planCount()).isEqualTo(4);
        assertThat(graph.taskCount()).isEqualTo(6);
        assertThat(graph.getAllPlanNames()).containsExactlyInAnyOrder(
            "plan_data_ingestion", "plan_data_processing", "plan_analysis", "plan_reporting");
        assertThat(graph.getAllTaskNames()).containsExactlyInAnyOrder(
            "task_fetch_data", "task_validate_data", "task_transform_data", "task_analyze_data", 
            "task_generate_report", "task_send_notification");
    }
    
    @Test
    void testBuildParallelTasksGraphFromDotFile() throws Exception {
        // Given
        Path specDir = TestResourceUtils.getResourcePath("valid_graphs/parallel_tasks_graph");
        
        // When
        AgentGraph graph = service.buildGraph(specDir);
        
        // Then
        assertThat(graph).isNotNull();
        assertThat(graph.planCount()).isEqualTo(3);
        assertThat(graph.taskCount()).isEqualTo(5);
        assertThat(graph.getAllPlanNames()).containsExactlyInAnyOrder(
            "plan_data_collection", "plan_parallel_processing", "plan_aggregation");
        assertThat(graph.getAllTaskNames()).containsExactlyInAnyOrder(
            "task_fetch_user_data", "task_fetch_product_data", "task_process_user_data", 
            "task_process_product_data", "task_aggregate_results");
    }
    
    @Test
    void testBuildInvalidGraph() throws Exception {
        // Given
        Path specDir = createInvalidGraphSpecification();
        
        // When & Then
        assertThatThrownBy(() -> service.buildGraph(specDir))
            .isInstanceOf(GraphValidationException.class)
            .hasMessageContaining("Plan directory does not exist");
    }
    
    @Test
    void testGetSupportedFormats() {
        // When
        var formats = service.getSupportedFormats();
        
        // Then
        assertThat(formats).containsExactlyInAnyOrder(".dot");
    }
    
    @Test
    void testGetAvailableParsers() {
        // When
        var parsers = service.getAvailableParsers();
        
        // Then
        assertThat(parsers).hasSize(1);
        assertThat(parsers.get(0).getName()).isEqualTo("GraphViz DOT Parser");
        assertThat(parsers.get(0).getSupportedExtensions()).containsExactlyInAnyOrder(".dot");
    }
    
    @Test
    void testBuildGraphWithNullDirectory() {
        // When & Then
        assertThatThrownBy(() -> service.buildGraph(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Specification directory cannot be null");
    }
    
    @Test
    void testBuildGraphWithNonExistentDirectory() {
        // Given
        Path nonExistentDir = Paths.get("/non/existent/directory");
        
        // When & Then
        assertThatThrownBy(() -> service.buildGraph(nonExistentDir))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Specification directory does not exist");
    }
    
    @Test
    void testBuildGraphWithFileInsteadOfDirectory() throws Exception {
        // Given
        Path filePath = tempDir.resolve("test.txt");
        Files.write(filePath, "test content".getBytes());
        
        // When & Then
        assertThatThrownBy(() -> service.buildGraph(filePath))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Specification path is not a directory");
    }
    
    @Test
    void testBuildGraphWithNoSpecificationFile() throws Exception {
        // Given
        Path emptyDir = tempDir.resolve("empty");
        Files.createDirectories(emptyDir);
        
        // When & Then
        assertThatThrownBy(() -> service.buildGraph(emptyDir))
            .isInstanceOf(GraphParsingException.class)
            .hasMessageContaining("Error searching for specification file");
    }
    
    @Test
    void testAddParser() {
        // Given
        var initialParsers = service.getAvailableParsers();
        
        // When
        service.addParser(new MockParser());
        
        // Then
        var updatedParsers = service.getAvailableParsers();
        assertThat(updatedParsers).hasSize(initialParsers.size() + 1);
        assertThat(updatedParsers).anyMatch(p -> p.getName().equals("Mock Parser"));
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
                
                task_fetch_data -> plan_analysis;
                task_process_data -> plan_analysis;
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
    
    private Path createInvalidGraphSpecification() throws Exception {
        Path specDir = tempDir.resolve("invalid_graph");
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
    

    
    // Mock parser for testing
    private static class MockParser implements GraphParser {
        @Override
        public AgentGraph parse(java.nio.file.Path specificationDirectory) {
            throw new UnsupportedOperationException("Mock parser not implemented");
        }
        
        @Override
        public boolean supports(java.nio.file.Path specificationFile) {
            return specificationFile != null && specificationFile.toString().endsWith(".mock");
        }
        
        @Override
        public String getName() {
            return "Mock Parser";
        }
        
        @Override
        public String[] getSupportedExtensions() {
            return new String[]{".mock"};
        }
    }
} 