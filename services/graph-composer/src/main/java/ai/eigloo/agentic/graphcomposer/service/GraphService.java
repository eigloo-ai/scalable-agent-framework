package ai.eigloo.agentic.graphcomposer.service;

import ai.eigloo.agentic.graphcomposer.dto.AgentGraphDto;
import ai.eigloo.agentic.graphcomposer.dto.AgentGraphSummary;
import ai.eigloo.agentic.graphcomposer.dto.CreateGraphRequest;
import ai.eigloo.agentic.graphcomposer.dto.ExecutionResponse;
import ai.eigloo.agentic.graphcomposer.dto.GraphStatusUpdate;
import ai.eigloo.agentic.graphcomposer.dto.*;

import java.util.List;

/**
 * Service interface for Agent Graph management operations.
 * Defines the business logic for graph CRUD operations with tenant isolation.
 */
public interface GraphService {

    /**
     * List all graphs for a specific tenant.
     *
     * @param tenantId the tenant identifier
     * @return list of graph summaries
     */
    List<AgentGraphSummary> listGraphs(String tenantId);

    /**
     * Get a specific graph by ID with tenant validation.
     *
     * @param graphId the graph identifier
     * @param tenantId the tenant identifier
     * @return the complete graph data
     * @throws GraphNotFoundException if graph is not found
     * @throws TenantAccessException if tenant doesn't have access
     */
    AgentGraphDto getGraph(String graphId, String tenantId);

    /**
     * Create a new graph for a tenant.
     *
     * @param request the graph creation request
     * @return the created graph data
     */
    AgentGraphDto createGraph(CreateGraphRequest request);

    /**
     * Update an existing graph with tenant validation.
     *
     * @param graphId the graph identifier
     * @param graph the updated graph data
     * @return the updated graph data
     * @throws GraphNotFoundException if graph is not found
     * @throws TenantAccessException if tenant doesn't have access
     */
    AgentGraphDto updateGraph(String graphId, AgentGraphDto graph);

    /**
     * Delete a graph with tenant validation.
     *
     * @param graphId the graph identifier
     * @param tenantId the tenant identifier
     * @throws GraphNotFoundException if graph is not found
     * @throws TenantAccessException if tenant doesn't have access
     */
    void deleteGraph(String graphId, String tenantId);



    /**
     * Submit a graph for execution.
     *
     * @param graphId the graph identifier
     * @param tenantId the tenant identifier
     * @return execution response
     * @throws GraphNotFoundException if graph is not found
     * @throws TenantAccessException if tenant doesn't have access
     */
    ExecutionResponse submitForExecution(String graphId, String tenantId);

    /**
     * Update the status of a graph.
     *
     * @param graphId the graph identifier
     * @param statusUpdate the status update request
     * @throws GraphNotFoundException if graph is not found
     */
    void updateGraphStatus(String graphId, GraphStatusUpdate statusUpdate);

    // Exception classes
    class GraphNotFoundException extends RuntimeException {
        public GraphNotFoundException(String message) {
            super(message);
        }
    }



    class TenantAccessException extends RuntimeException {
        public TenantAccessException(String message) {
            super(message);
        }
    }
}