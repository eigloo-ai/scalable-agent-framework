package ai.eigloo.agentic.graphcomposer.service;

import ai.eigloo.agentic.graphcomposer.dto.AgentGraphDto;
import ai.eigloo.agentic.graphcomposer.dto.NodeNameValidationRequest;
import ai.eigloo.agentic.graphcomposer.dto.ValidationResult;

/**
 * Service interface for real-time graph constraint validation.
 * Defines the business logic for validating graphs against Agent Graph Specification.
 */
public interface ValidationService {

    /**
     * Validate a complete graph structure against all constraints.
     *
     * @param graph the graph to validate
     * @return validation result with any errors or warnings
     */
    ValidationResult validateGraph(AgentGraphDto graph);

    /**
     * Validate a node name for uniqueness and format.
     *
     * @param request the node name validation request
     * @return validation result
     */
    ValidationResult validateNodeName(NodeNameValidationRequest request);

    /**
     * Validate that plans only connect to tasks and tasks only connect to plans.
     *
     * @param graph the graph to validate
     * @return validation result for connection constraints
     */
    ValidationResult validateConnectionConstraints(AgentGraphDto graph);

    /**
     * Validate that tasks have exactly one upstream plan.
     *
     * @param graph the graph to validate
     * @return validation result for task upstream constraints
     */
    ValidationResult validateTaskUpstreamConstraints(AgentGraphDto graph);

    /**
     * Validate that plans can have multiple upstream tasks.
     *
     * @param graph the graph to validate
     * @return validation result for plan upstream constraints
     */
    ValidationResult validatePlanUpstreamConstraints(AgentGraphDto graph);

    /**
     * Check for orphaned nodes and connectivity requirements.
     *
     * @param graph the graph to validate
     * @return validation result for connectivity
     */
    ValidationResult validateConnectivity(AgentGraphDto graph);

    /**
     * Validate that node names are unique across the graph.
     *
     * @param graph the graph to validate
     * @return validation result for name uniqueness
     */
    ValidationResult validateNodeNameUniqueness(AgentGraphDto graph);

    /**
     * Validate that node names are valid Python identifiers.
     *
     * @param nodeName the node name to validate
     * @return validation result for Python identifier format
     */
    ValidationResult validatePythonIdentifier(String nodeName);
}