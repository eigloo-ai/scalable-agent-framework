package ai.eigloo.agentic.graphcomposer.controller;

import ai.eigloo.agentic.graphcomposer.dto.AgentGraphDto;
import ai.eigloo.agentic.graphcomposer.dto.AgentGraphSummary;
import ai.eigloo.agentic.graphcomposer.dto.CreateGraphRequest;
import ai.eigloo.agentic.graphcomposer.dto.ExecutionResponse;
import ai.eigloo.agentic.graphcomposer.dto.GraphStatusUpdate;
import ai.eigloo.agentic.graphcomposer.dto.*;
import ai.eigloo.agentic.graphcomposer.service.GraphService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * REST Controller for Agent Graph management operations.
 * Provides endpoints for creating, reading, updating, and deleting graphs.
 */
@RestController
@RequestMapping("/api/v1/graphs")
@CrossOrigin(origins = "*", maxAge = 3600)
public class GraphController {

    private final GraphService graphService;

    public GraphController(GraphService graphService) {
        this.graphService = graphService;
    }

    /**
     * List all graphs for a specific tenant.
     *
     * @param tenantId the tenant identifier
     * @return list of graph summaries
     */
    @GetMapping
    public ResponseEntity<List<AgentGraphSummary>> listGraphs(@RequestParam String tenantId) {
        List<AgentGraphSummary> graphs = graphService.listGraphs(tenantId);
        return ResponseEntity.ok(graphs);
    }

    /**
     * Get a specific graph by ID.
     *
     * @param graphId the graph identifier
     * @param tenantId the tenant identifier
     * @return the complete graph data
     */
    @GetMapping("/{graphId}")
    public ResponseEntity<AgentGraphDto> getGraph(@PathVariable String graphId, @RequestParam String tenantId) {
        AgentGraphDto graph = graphService.getGraph(graphId, tenantId);
        return ResponseEntity.ok(graph);
    }

    /**
     * Create a new graph.
     *
     * @param request the graph creation request
     * @return the created graph data
     */
    @PostMapping
    public ResponseEntity<AgentGraphDto> createGraph(@Valid @RequestBody CreateGraphRequest request) {
        AgentGraphDto createdGraph = graphService.createGraph(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdGraph);
    }

    /**
     * Update an existing graph.
     *
     * @param graphId the graph identifier
     * @param graph the updated graph data
     * @return the updated graph data
     */
    @PutMapping("/{graphId}")
    public ResponseEntity<AgentGraphDto> updateGraph(@PathVariable String graphId, @Valid @RequestBody AgentGraphDto graph) {
        AgentGraphDto updatedGraph = graphService.updateGraph(graphId, graph);
        return ResponseEntity.ok(updatedGraph);
    }

    /**
     * Delete a graph.
     *
     * @param graphId the graph identifier
     * @param tenantId the tenant identifier
     * @return empty response
     */
    @DeleteMapping("/{graphId}")
    public ResponseEntity<Void> deleteGraph(@PathVariable String graphId, @RequestParam String tenantId) {
        graphService.deleteGraph(graphId, tenantId);
        return ResponseEntity.noContent().build();
    }



    /**
     * Submit a graph for execution.
     *
     * @param graphId the graph identifier
     * @param tenantId the tenant identifier
     * @return execution response
     */
    @PostMapping("/{graphId}/execute")
    public ResponseEntity<ExecutionResponse> submitForExecution(@PathVariable String graphId, @RequestParam String tenantId) {
        ExecutionResponse response = graphService.submitForExecution(graphId, tenantId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update the status of a graph.
     *
     * @param graphId the graph identifier
     * @param statusUpdate the status update request
     * @return empty response
     */
    @PutMapping("/{graphId}/status")
    public ResponseEntity<Void> updateGraphStatus(@PathVariable String graphId, @Valid @RequestBody GraphStatusUpdate statusUpdate) {
        graphService.updateGraphStatus(graphId, statusUpdate);
        return ResponseEntity.noContent().build();
    }
}