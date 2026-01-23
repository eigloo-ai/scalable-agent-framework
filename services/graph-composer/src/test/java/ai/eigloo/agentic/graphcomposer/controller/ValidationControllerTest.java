package ai.eigloo.agentic.graphcomposer.controller;

import ai.eigloo.agentic.graphcomposer.dto.AgentGraphDto;
import ai.eigloo.agentic.graphcomposer.dto.GraphStatus;
import ai.eigloo.agentic.graphcomposer.dto.NodeNameValidationRequest;
import ai.eigloo.agentic.graphcomposer.dto.PlanDto;
import ai.eigloo.agentic.graphcomposer.dto.TaskDto;
import ai.eigloo.agentic.graphcomposer.dto.ValidationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import ai.eigloo.agentic.graphcomposer.dto.*;
import ai.eigloo.agentic.graphcomposer.exception.GraphComposerExceptionHandler;
import ai.eigloo.agentic.graphcomposer.service.ValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ValidationController using standalone MockMvc setup.
 */
@ExtendWith(MockitoExtension.class)
class ValidationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ValidationService validationService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ValidationController(validationService))
                .setControllerAdvice(new GraphComposerExceptionHandler())
                .build();
    }

    @Test
    void validateGraph_WithValidGraph_ShouldReturnValidResult() throws Exception {
        // Given
        AgentGraphDto graph = createValidGraph();
        ValidationResult validResult = new ValidationResult(true, Collections.emptyList(), Collections.emptyList());
        
        when(validationService.validateGraph(any(AgentGraphDto.class))).thenReturn(validResult);

        // When & Then
        mockMvc.perform(post("/api/v1/validation/graph")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(graph)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.errors").isEmpty())
                .andExpect(jsonPath("$.warnings").isEmpty());

        verify(validationService).validateGraph(any(AgentGraphDto.class));
    }

    @Test
    void validateGraph_WithInvalidGraph_ShouldReturnErrorsAndWarnings() throws Exception {
        // Given
        AgentGraphDto graph = createInvalidGraph();
        ValidationResult invalidResult = new ValidationResult(
            false,
            Arrays.asList("Task 'task1' has no upstream plan", "Plan 'plan1' connects to another plan"),
            Arrays.asList("Graph has orphaned nodes")
        );
        
        when(validationService.validateGraph(any(AgentGraphDto.class))).thenReturn(invalidResult);

        // When & Then
        mockMvc.perform(post("/api/v1/validation/graph")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(graph)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[0]").value("Task 'task1' has no upstream plan"))
                .andExpect(jsonPath("$.errors[1]").value("Plan 'plan1' connects to another plan"))
                .andExpect(jsonPath("$.warnings.length()").value(1))
                .andExpect(jsonPath("$.warnings[0]").value("Graph has orphaned nodes"));

        verify(validationService).validateGraph(any(AgentGraphDto.class));
    }

    @Test
    void validateGraph_WithNullGraph_ShouldReturn400() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/validation/graph")
                .contentType(MediaType.APPLICATION_JSON)
                .content("null"))
                .andExpect(status().isBadRequest());

        verify(validationService, never()).validateGraph(any());
    }

    @Test
    void validateNodeName_WithValidName_ShouldReturnValidResult() throws Exception {
        // Given
        NodeNameValidationRequest request = new NodeNameValidationRequest(
            "valid_node_name", 
            "graph1", 
            null,
            new HashSet<>(Arrays.asList("existing_node"))
        );
        ValidationResult validResult = new ValidationResult(true, Collections.emptyList(), Collections.emptyList());
        
        when(validationService.validateNodeName(any(NodeNameValidationRequest.class))).thenReturn(validResult);

        // When & Then
        mockMvc.perform(post("/api/v1/validation/node-name")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.errors").isEmpty())
                .andExpect(jsonPath("$.warnings").isEmpty());

        verify(validationService).validateNodeName(any(NodeNameValidationRequest.class));
    }

    @Test
    void validateNodeName_WithInvalidName_ShouldReturnErrors() throws Exception {
        // Given
        NodeNameValidationRequest request = new NodeNameValidationRequest(
            "invalid-node-name", 
            "graph1", 
            null,
            new HashSet<>(Arrays.asList("existing_node"))
        );
        ValidationResult invalidResult = new ValidationResult(
            false,
            Arrays.asList("Node name 'invalid-node-name' is not a valid Python identifier"),
            Collections.emptyList()
        );
        
        when(validationService.validateNodeName(any(NodeNameValidationRequest.class))).thenReturn(invalidResult);

        // When & Then
        mockMvc.perform(post("/api/v1/validation/node-name")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0]").value("Node name 'invalid-node-name' is not a valid Python identifier"));

        verify(validationService).validateNodeName(any(NodeNameValidationRequest.class));
    }

    @Test
    void validateNodeName_WithDuplicateName_ShouldReturnErrors() throws Exception {
        // Given
        NodeNameValidationRequest request = new NodeNameValidationRequest(
            "existing_node", 
            "graph1", 
            null,
            new HashSet<>(Arrays.asList("existing_node"))
        );
        ValidationResult invalidResult = new ValidationResult(
            false,
            Arrays.asList("Node name 'existing_node' already exists in the graph"),
            Collections.emptyList()
        );
        
        when(validationService.validateNodeName(any(NodeNameValidationRequest.class))).thenReturn(invalidResult);

        // When & Then
        mockMvc.perform(post("/api/v1/validation/node-name")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0]").value("Node name 'existing_node' already exists in the graph"));

        verify(validationService).validateNodeName(any(NodeNameValidationRequest.class));
    }

    @Test
    void validateNodeName_WithBlankName_ShouldReturn400() throws Exception {
        // Given
        NodeNameValidationRequest request = new NodeNameValidationRequest(
            "", 
            "graph1", 
            null,
            new HashSet<>()
        );

        // When & Then
        mockMvc.perform(post("/api/v1/validation/node-name")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(validationService, never()).validateNodeName(any());
    }

    @Test
    void validateNodeName_WithBlankGraphId_ShouldReturn400() throws Exception {
        // Given
        NodeNameValidationRequest request = new NodeNameValidationRequest(
            "valid_name", 
            "", 
            null,
            new HashSet<>()
        );

        // When & Then
        mockMvc.perform(post("/api/v1/validation/node-name")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(validationService, never()).validateNodeName(any());
    }

    @Test
    void validateNodeName_WithTooLongName_ShouldReturn400() throws Exception {
        // Given
        StringBuilder longName = new StringBuilder();
        for (int i = 0; i < 256; i++) {
            longName.append("a");
        }
        NodeNameValidationRequest request = new NodeNameValidationRequest(
            longName.toString(), 
            "graph1", 
            null,
            new HashSet<>()
        );

        // When & Then
        mockMvc.perform(post("/api/v1/validation/node-name")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(validationService, never()).validateNodeName(any());
    }

    @Test
    void validateNodeName_ForRename_ShouldExcludeCurrentNode() throws Exception {
        // Given
        NodeNameValidationRequest request = new NodeNameValidationRequest(
            "renamed_node", 
            "graph1", 
            "node1", // Exclude current node from uniqueness check
            new HashSet<>(Arrays.asList("node1", "other_node"))
        );
        ValidationResult validResult = new ValidationResult(true, Collections.emptyList(), Collections.emptyList());
        
        when(validationService.validateNodeName(any(NodeNameValidationRequest.class))).thenReturn(validResult);

        // When & Then
        mockMvc.perform(post("/api/v1/validation/node-name")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.valid").value(true));

        verify(validationService).validateNodeName(any(NodeNameValidationRequest.class));
    }

    @Test
    void validateGraph_WithComplexValidationErrors_ShouldReturnAllErrors() throws Exception {
        // Given
        AgentGraphDto graph = createComplexInvalidGraph();
        ValidationResult complexResult = new ValidationResult(
            false,
            Arrays.asList(
                "Task 'task1' has no upstream plan",
                "Task 'task2' has multiple upstream plans",
                "Plan 'plan1' connects to another plan 'plan2'",
                "Node name 'invalid-name' is not a valid Python identifier",
                "Duplicate node name 'duplicate' found",
                "Orphaned node 'orphan' has no connections"
            ),
            Arrays.asList(
                "Graph structure is complex and may be hard to understand",
                "Consider simplifying the workflow"
            )
        );
        
        when(validationService.validateGraph(any(AgentGraphDto.class))).thenReturn(complexResult);

        // When & Then
        mockMvc.perform(post("/api/v1/validation/graph")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(graph)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.errors.length()").value(6))
                .andExpect(jsonPath("$.warnings.length()").value(2))
                .andExpect(jsonPath("$.errors[0]").value("Task 'task1' has no upstream plan"))
                .andExpect(jsonPath("$.errors[5]").value("Orphaned node 'orphan' has no connections"))
                .andExpect(jsonPath("$.warnings[0]").value("Graph structure is complex and may be hard to understand"));

        verify(validationService).validateGraph(any(AgentGraphDto.class));
    }

    private AgentGraphDto createValidGraph() {
        AgentGraphDto graph = new AgentGraphDto();
        graph.setId("graph1");
        graph.setName("Valid Graph");
        graph.setTenantId("test-tenant");
        graph.setStatus(GraphStatus.NEW);
        
        // Add valid plans and tasks
        PlanDto plan = new PlanDto();
        plan.setName("valid_plan");
        plan.setLabel("Valid Plan");
        graph.setPlans(Arrays.asList(plan));
        
        TaskDto task = new TaskDto();
        task.setName("valid_task");
        task.setLabel("Valid Task");
        graph.setTasks(Arrays.asList(task));
        
        return graph;
    }

    private AgentGraphDto createInvalidGraph() {
        AgentGraphDto graph = new AgentGraphDto();
        graph.setId("graph1");
        graph.setName("Invalid Graph");
        graph.setTenantId("test-tenant");
        graph.setStatus(GraphStatus.NEW);
        
        // Add invalid structure
        PlanDto plan = new PlanDto();
        plan.setName("invalid-plan-name");
        plan.setLabel("Invalid Plan Name");
        graph.setPlans(Arrays.asList(plan));
        
        TaskDto task = new TaskDto();
        task.setName("orphaned_task");
        task.setLabel("Orphaned Task");
        graph.setTasks(Arrays.asList(task));
        
        return graph;
    }

    private AgentGraphDto createComplexInvalidGraph() {
        AgentGraphDto graph = new AgentGraphDto();
        graph.setId("graph1");
        graph.setName("Complex Invalid Graph");
        graph.setTenantId("test-tenant");
        graph.setStatus(GraphStatus.NEW);
        
        // Add multiple invalid elements
        PlanDto plan1 = new PlanDto();
        plan1.setName("invalid-name");
        plan1.setLabel("Invalid Name Plan");
        
        PlanDto plan2 = new PlanDto();
        plan2.setName("duplicate");
        plan2.setLabel("Duplicate Plan");
        
        graph.setPlans(Arrays.asList(plan1, plan2));
        
        TaskDto task1 = new TaskDto();
        task1.setName("orphaned_task");
        task1.setLabel("Orphaned Task");
        
        TaskDto task2 = new TaskDto();
        task2.setName("duplicate");
        task2.setLabel("Duplicate Task");
        
        TaskDto task3 = new TaskDto();
        task3.setName("orphan");
        task3.setLabel("Orphan Task");
        
        graph.setTasks(Arrays.asList(task1, task2, task3));
        
        return graph;
    }
}