package com.pcallahan.agentic.graphcomposer.service;

import com.pcallahan.agentic.graphcomposer.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Implementation of ValidationService for real-time graph constraint validation.
 * Provides business logic for validating graphs against Agent Graph Specification.
 */
@Service
public class ValidationServiceImpl implements ValidationService {

    private static final Logger logger = LoggerFactory.getLogger(ValidationServiceImpl.class);
    
    // Python identifier pattern: starts with letter or underscore, followed by letters, digits, or underscores
    private static final Pattern PYTHON_IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");
    
    // Python keywords that cannot be used as identifiers
    private static final Set<String> PYTHON_KEYWORDS = Set.of(
            "False", "None", "True", "and", "as", "assert", "async", "await", "break", "class",
            "continue", "def", "del", "elif", "else", "except", "finally", "for", "from",
            "global", "if", "import", "in", "is", "lambda", "nonlocal", "not", "or", "pass",
            "raise", "return", "try", "while", "with", "yield"
    );

    @Override
    public ValidationResult validateGraph(AgentGraphDto graph) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        if (graph == null) {
            logger.debug("Validating complete graph: null");
            errors.add("Graph cannot be null");
            return new ValidationResult(false, errors, warnings);
        }
        
        logger.debug("Validating complete graph: {}", graph.getName());
        
        // Validate basic graph properties
        if (graph.getName() == null || graph.getName().trim().isEmpty()) {
            errors.add("Graph name cannot be empty");
        }
        
        if (graph.getTenantId() == null || graph.getTenantId().trim().isEmpty()) {
            errors.add("Tenant ID cannot be empty");
        }
        
        // Validate node name uniqueness
        ValidationResult nameValidation = validateNodeNameUniqueness(graph);
        errors.addAll(nameValidation.getErrors());
        warnings.addAll(nameValidation.getWarnings());
        
        // Validate connection constraints
        ValidationResult connectionValidation = validateConnectionConstraints(graph);
        errors.addAll(connectionValidation.getErrors());
        warnings.addAll(connectionValidation.getWarnings());
        
        // Validate task upstream constraints
        ValidationResult taskValidation = validateTaskUpstreamConstraints(graph);
        errors.addAll(taskValidation.getErrors());
        warnings.addAll(taskValidation.getWarnings());
        
        // Validate plan upstream constraints
        ValidationResult planValidation = validatePlanUpstreamConstraints(graph);
        errors.addAll(planValidation.getErrors());
        warnings.addAll(planValidation.getWarnings());
        
        // Validate connectivity
        ValidationResult connectivityValidation = validateConnectivity(graph);
        errors.addAll(connectivityValidation.getErrors());
        warnings.addAll(connectivityValidation.getWarnings());
        
        boolean isValid = errors.isEmpty();
        logger.debug("Graph validation completed. Valid: {}, Errors: {}, Warnings: {}", 
                    isValid, errors.size(), warnings.size());
        
        return new ValidationResult(isValid, errors, warnings);
    }

    @Override
    public ValidationResult validateNodeName(NodeNameValidationRequest request) {
        logger.debug("Validating node name: {}", request.getNodeName());
        
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        if (request.getNodeName() == null || request.getNodeName().trim().isEmpty()) {
            errors.add("Node name cannot be empty");
            return new ValidationResult(false, errors, warnings);
        }
        
        String nodeName = request.getNodeName().trim();
        
        // Validate Python identifier format
        ValidationResult identifierValidation = validatePythonIdentifier(nodeName);
        errors.addAll(identifierValidation.getErrors());
        warnings.addAll(identifierValidation.getWarnings());
        
        // Check uniqueness within the graph if provided
        if (request.getExistingNodeNames() != null && !request.getExistingNodeNames().isEmpty()) {
            if (request.getExistingNodeNames().contains(nodeName)) {
                errors.add("Node name '" + nodeName + "' already exists in the graph");
            }
        }
        
        boolean isValid = errors.isEmpty();
        logger.debug("Node name validation completed. Valid: {}, Errors: {}", isValid, errors.size());
        
        return new ValidationResult(isValid, errors, warnings);
    }

    @Override
    public ValidationResult validateConnectionConstraints(AgentGraphDto graph) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        if (graph.getPlanToTasks() != null) {
            // Validate that plans only connect to tasks
            for (Map.Entry<String, Set<String>> entry : graph.getPlanToTasks().entrySet()) {
                String planName = entry.getKey();
                Set<String> connectedNodes = entry.getValue();
                
                // Check if plan exists
                boolean planExists = graph.getPlans() != null && 
                                   graph.getPlans().stream().anyMatch(p -> p.getName().equals(planName));
                
                if (!planExists) {
                    errors.add("Plan '" + planName + "' referenced in connections but not found in graph");
                    continue;
                }
                
                // Check that all connected nodes are tasks
                for (String connectedNode : connectedNodes) {
                    boolean isTask = graph.getTasks() != null && 
                                   graph.getTasks().stream().anyMatch(t -> t.getName().equals(connectedNode));
                    
                    if (!isTask) {
                        errors.add("Plan '" + planName + "' is connected to '" + connectedNode + "' which is not a task");
                    }
                }
            }
            
            // Validate that tasks have valid upstream plans using taskToPlan map
            if (graph.getTasks() != null && graph.getTaskToPlan() != null) {
                for (TaskDto task : graph.getTasks()) {
                    String taskName = task.getName();
                    String upstreamPlan = graph.getTaskToPlan().get(taskName);
                    
                    if (upstreamPlan != null && !upstreamPlan.isEmpty()) {
                        // Check if the upstream plan exists
                        boolean planExists = graph.getPlans() != null && 
                                           graph.getPlans().stream().anyMatch(p -> p.getName().equals(upstreamPlan));
                        
                        if (!planExists) {
                            errors.add("Task '" + taskName + "' is connected to '" + upstreamPlan + "' which is not a plan");
                        }
                    }
                }
            }
        }
        
        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    @Override
    public ValidationResult validateTaskUpstreamConstraints(AgentGraphDto graph) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        if (graph.getTasks() != null) {
            for (TaskDto task : graph.getTasks()) {
                String taskName = task.getName();
                
                // Check if task has exactly one upstream plan using taskToPlan map
                final String upstreamPlan = graph.getTaskToPlan() != null ? 
                    graph.getTaskToPlan().get(taskName) : null;
                
                if (upstreamPlan == null || upstreamPlan.isEmpty()) {
                    errors.add("Task '" + taskName + "' must have exactly one upstream plan");
                } else {
                    // Verify the upstream plan exists
                    boolean planExists = graph.getPlans() != null && 
                                       graph.getPlans().stream().anyMatch(p -> p.getName().equals(upstreamPlan));
                    
                    if (!planExists) {
                        errors.add("Task '" + taskName + "' references upstream plan '" + upstreamPlan + "' which does not exist");
                    }
                }
            }
        }
        
        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    @Override
    public ValidationResult validatePlanUpstreamConstraints(AgentGraphDto graph) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Plans can have multiple upstream tasks, so this is generally permissive
        // We mainly check that referenced upstream tasks exist
        
        if (graph.getPlans() != null && graph.getPlanToTasks() != null) {
            for (PlanDto plan : graph.getPlans()) {
                String planName = plan.getName();
                
                if (plan.getUpstreamTaskIds() != null) {
                    for (String upstreamTaskId : plan.getUpstreamTaskIds()) {
                        // Verify the upstream task exists
                        boolean taskExists = graph.getTasks() != null && 
                                           graph.getTasks().stream().anyMatch(t -> t.getName().equals(upstreamTaskId));
                        
                        if (!taskExists) {
                            errors.add("Plan '" + planName + "' references upstream task '" + upstreamTaskId + "' which does not exist");
                        }
                    }
                }
            }
        }
        
        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    @Override
    public ValidationResult validateConnectivity(AgentGraphDto graph) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        Set<String> allNodes = new HashSet<>();
        Set<String> connectedNodes = new HashSet<>();
        
        // Collect all node names
        if (graph.getPlans() != null) {
            allNodes.addAll(graph.getPlans().stream().map(PlanDto::getName).collect(Collectors.toSet()));
        }
        if (graph.getTasks() != null) {
            allNodes.addAll(graph.getTasks().stream().map(TaskDto::getName).collect(Collectors.toSet()));
        }
        
        // Collect connected nodes
        if (graph.getPlanToTasks() != null) {
            connectedNodes.addAll(graph.getPlanToTasks().keySet());
            graph.getPlanToTasks().values().forEach(connectedNodes::addAll);
        }
        if (graph.getTaskToPlan() != null) {
            connectedNodes.addAll(graph.getTaskToPlan().keySet());
            connectedNodes.addAll(graph.getTaskToPlan().values());
        }
        
        // Find orphaned nodes
        Set<String> orphanedNodes = new HashSet<>(allNodes);
        orphanedNodes.removeAll(connectedNodes);
        
        if (!orphanedNodes.isEmpty()) {
            for (String orphanedNode : orphanedNodes) {
                warnings.add("Node '" + orphanedNode + "' is not connected to any other nodes");
            }
        }
        
        // Check for empty graph
        if (allNodes.isEmpty()) {
            warnings.add("Graph contains no nodes");
        } else if (allNodes.size() == 1) {
            warnings.add("Graph contains only one node");
        }
        
        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    @Override
    public ValidationResult validateNodeNameUniqueness(AgentGraphDto graph) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        Set<String> allNodeNames = new HashSet<>();
        Set<String> duplicateNames = new HashSet<>();
        
        // Check plan names
        if (graph.getPlans() != null) {
            for (PlanDto plan : graph.getPlans()) {
                String name = plan.getName();
                if (name != null) {
                    if (!allNodeNames.add(name)) {
                        duplicateNames.add(name);
                    }
                }
            }
        }
        
        // Check task names
        if (graph.getTasks() != null) {
            for (TaskDto task : graph.getTasks()) {
                String name = task.getName();
                if (name != null) {
                    if (!allNodeNames.add(name)) {
                        duplicateNames.add(name);
                    }
                }
            }
        }
        
        // Report duplicates
        for (String duplicateName : duplicateNames) {
            errors.add("Node name '" + duplicateName + "' is used multiple times in the graph");
        }
        
        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    @Override
    public ValidationResult validatePythonIdentifier(String nodeName) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        if (nodeName == null || nodeName.trim().isEmpty()) {
            errors.add("Node name cannot be empty");
            return new ValidationResult(false, errors, warnings);
        }
        
        String trimmedName = nodeName.trim();
        
        // Check if it matches Python identifier pattern
        if (!PYTHON_IDENTIFIER_PATTERN.matcher(trimmedName).matches()) {
            errors.add("Node name '" + trimmedName + "' is not a valid Python identifier. " +
                      "It must start with a letter or underscore and contain only letters, digits, and underscores.");
        }
        
        // Check if it's a Python keyword
        if (PYTHON_KEYWORDS.contains(trimmedName)) {
            errors.add("Node name '" + trimmedName + "' is a Python keyword and cannot be used as an identifier");
        }
        
        // Warn about naming conventions
        if (trimmedName.startsWith("_")) {
            warnings.add("Node name '" + trimmedName + "' starts with underscore, which is typically reserved for internal use");
        }
        
        if (trimmedName.toUpperCase().equals(trimmedName) && trimmedName.length() > 1) {
            warnings.add("Node name '" + trimmedName + "' is all uppercase, which is typically reserved for constants");
        }
        
        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }
}