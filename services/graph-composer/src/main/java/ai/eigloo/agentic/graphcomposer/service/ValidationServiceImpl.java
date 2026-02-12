package ai.eigloo.agentic.graphcomposer.service;

import ai.eigloo.agentic.graphcomposer.dto.AgentGraphDto;
import ai.eigloo.agentic.graphcomposer.dto.ExecutorFileDto;
import ai.eigloo.agentic.graphcomposer.dto.GraphEdgeDto;
import ai.eigloo.agentic.graphcomposer.dto.GraphNodeType;
import ai.eigloo.agentic.graphcomposer.dto.NodeNameValidationRequest;
import ai.eigloo.agentic.graphcomposer.dto.PlanDto;
import ai.eigloo.agentic.graphcomposer.dto.TaskDto;
import ai.eigloo.agentic.graphcomposer.dto.ValidationResult;
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
    private static final Pattern PLAN_FUNCTION_PATTERN = Pattern.compile("(?m)^\\s*(?:async\\s+)?def\\s+plan\\s*\\(");
    private static final Pattern TASK_FUNCTION_PATTERN = Pattern.compile("(?m)^\\s*(?:async\\s+)?def\\s+task\\s*\\(");
    
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

        // Validate executor contract requirements
        ValidationResult contractValidation = validateExecutorContracts(graph);
        errors.addAll(contractValidation.getErrors());
        warnings.addAll(contractValidation.getWarnings());
        
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

        Set<String> planNames = graph.getPlans() != null
                ? graph.getPlans().stream().map(PlanDto::getName).collect(Collectors.toSet())
                : Set.of();
        Set<String> taskNames = graph.getTasks() != null
                ? graph.getTasks().stream().map(TaskDto::getName).collect(Collectors.toSet())
                : Set.of();

        List<GraphEdgeDto> edges = deriveCanonicalEdges(graph);
        for (GraphEdgeDto edge : edges) {
            if (edge.getFromType() == null || edge.getToType() == null) {
                errors.add("Edge must include fromType and toType");
                continue;
            }
            if (edge.getFrom() == null || edge.getFrom().isBlank() || edge.getTo() == null || edge.getTo().isBlank()) {
                errors.add("Edge endpoints cannot be blank");
                continue;
            }
            if (edge.getFromType() == edge.getToType()) {
                errors.add("Edge '" + edge.getFrom() + "' -> '" + edge.getTo() + "' must connect PLAN to TASK or TASK to PLAN");
                continue;
            }

            if (edge.getFromType() == GraphNodeType.PLAN && !planNames.contains(edge.getFrom())) {
                errors.add("Plan '" + edge.getFrom() + "' referenced in edges but not found in graph");
            }
            if (edge.getFromType() == GraphNodeType.TASK && !taskNames.contains(edge.getFrom())) {
                errors.add("Task '" + edge.getFrom() + "' referenced in edges but not found in graph");
            }
            if (edge.getToType() == GraphNodeType.PLAN && !planNames.contains(edge.getTo())) {
                errors.add("Plan '" + edge.getTo() + "' referenced in edges but not found in graph");
            }
            if (edge.getToType() == GraphNodeType.TASK && !taskNames.contains(edge.getTo())) {
                errors.add("Task '" + edge.getTo() + "' referenced in edges but not found in graph");
            }
        }
        
        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    private ValidationResult validateExecutorContracts(AgentGraphDto graph) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (graph.getPlans() != null) {
            for (PlanDto plan : graph.getPlans()) {
                String planName = nodeDisplayName(plan != null ? plan.getName() : null, "plan");
                ExecutorFileDto planFile = findFileByName(plan != null ? plan.getFiles() : null, "plan.py");
                if (planFile == null) {
                    errors.add("Plan '" + planName + "' must include file 'plan.py'");
                    continue;
                }

                String contents = planFile.getContents() == null ? "" : planFile.getContents();
                if (!PLAN_FUNCTION_PATTERN.matcher(contents).find()) {
                    errors.add("Plan '" + planName + "' file 'plan.py' must define function 'def plan(...)'");
                }
            }
        }

        if (graph.getTasks() != null) {
            for (TaskDto task : graph.getTasks()) {
                String taskName = nodeDisplayName(task != null ? task.getName() : null, "task");
                ExecutorFileDto taskFile = findFileByName(task != null ? task.getFiles() : null, "task.py");
                if (taskFile == null) {
                    errors.add("Task '" + taskName + "' must include file 'task.py'");
                    continue;
                }

                String contents = taskFile.getContents() == null ? "" : taskFile.getContents();
                if (!TASK_FUNCTION_PATTERN.matcher(contents).find()) {
                    errors.add("Task '" + taskName + "' file 'task.py' must define function 'def task(...)'");
                }
            }
        }

        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    private static ExecutorFileDto findFileByName(List<ExecutorFileDto> files, String expectedName) {
        if (files == null) {
            return null;
        }
        for (ExecutorFileDto file : files) {
            if (file != null && expectedName.equals(file.getName())) {
                return file;
            }
        }
        return null;
    }

    private static String nodeDisplayName(String name, String fallbackPrefix) {
        if (name == null || name.trim().isEmpty()) {
            return "<unnamed " + fallbackPrefix + ">";
        }
        return name.trim();
    }

    @Override
    public ValidationResult validateTaskUpstreamConstraints(AgentGraphDto graph) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        Map<String, Integer> incomingPlanEdgeCounts = new HashMap<>();
        List<GraphEdgeDto> edges = deriveCanonicalEdges(graph);
        for (GraphEdgeDto edge : edges) {
            if (edge.getFromType() == GraphNodeType.PLAN && edge.getToType() == GraphNodeType.TASK) {
                incomingPlanEdgeCounts.merge(edge.getTo(), 1, Integer::sum);
            }
        }

        if (graph.getTasks() != null) {
            for (TaskDto task : graph.getTasks()) {
                String taskName = task.getName();
                int incomingCount = incomingPlanEdgeCounts.getOrDefault(taskName, 0);
                if (incomingCount != 1) {
                    errors.add("Task '" + taskName + "' must have exactly one upstream plan");
                }
            }
        }
        
        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    @Override
    public ValidationResult validatePlanUpstreamConstraints(AgentGraphDto graph) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        Set<String> taskNames = graph.getTasks() != null
                ? graph.getTasks().stream().map(TaskDto::getName).collect(Collectors.toSet())
                : Set.of();

        for (GraphEdgeDto edge : deriveCanonicalEdges(graph)) {
            if (edge.getFromType() == GraphNodeType.TASK
                    && edge.getToType() == GraphNodeType.PLAN
                    && !taskNames.contains(edge.getFrom())) {
                errors.add("Plan '" + edge.getTo() + "' references upstream task '" + edge.getFrom() + "' which does not exist");
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
        
        // Collect connected nodes from canonical edges.
        for (GraphEdgeDto edge : deriveCanonicalEdges(graph)) {
            if (edge.getFrom() != null && !edge.getFrom().isBlank()) {
                connectedNodes.add(edge.getFrom());
            }
            if (edge.getTo() != null && !edge.getTo().isBlank()) {
                connectedNodes.add(edge.getTo());
            }
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

    private List<GraphEdgeDto> deriveCanonicalEdges(AgentGraphDto graph) {
        LinkedHashMap<String, GraphEdgeDto> deduped = new LinkedHashMap<>();

        if (graph.getEdges() != null && !graph.getEdges().isEmpty()) {
            for (GraphEdgeDto edge : graph.getEdges()) {
                addEdge(deduped, edge.getFrom(), edge.getFromType(), edge.getTo(), edge.getToType());
            }
            return new ArrayList<>(deduped.values());
        }

        if (graph.getTasks() != null) {
            for (TaskDto task : graph.getTasks()) {
                if (task.getUpstreamPlanId() != null && !task.getUpstreamPlanId().isBlank()) {
                    addEdge(deduped, task.getUpstreamPlanId(), GraphNodeType.PLAN, task.getName(), GraphNodeType.TASK);
                }
            }
        }

        if (graph.getPlans() != null) {
            for (PlanDto plan : graph.getPlans()) {
                if (plan.getUpstreamTaskIds() == null) {
                    continue;
                }
                for (String taskName : plan.getUpstreamTaskIds()) {
                    addEdge(deduped, taskName, GraphNodeType.TASK, plan.getName(), GraphNodeType.PLAN);
                }
            }
        }

        if (graph.getPlanToTasks() != null) {
            for (Map.Entry<String, Set<String>> entry : graph.getPlanToTasks().entrySet()) {
                for (String taskName : entry.getValue()) {
                    addEdge(deduped, entry.getKey(), GraphNodeType.PLAN, taskName, GraphNodeType.TASK);
                }
            }
        }

        if (graph.getTaskToPlan() != null) {
            // Legacy semantics retained for compatibility: Task -> upstream Plan.
            for (Map.Entry<String, String> entry : graph.getTaskToPlan().entrySet()) {
                addEdge(deduped, entry.getValue(), GraphNodeType.PLAN, entry.getKey(), GraphNodeType.TASK);
            }
        }

        return new ArrayList<>(deduped.values());
    }

    private static void addEdge(
            LinkedHashMap<String, GraphEdgeDto> deduped,
            String from,
            GraphNodeType fromType,
            String to,
            GraphNodeType toType) {
        if (from == null || from.isBlank() || to == null || to.isBlank() || fromType == null || toType == null) {
            return;
        }
        String key = fromType + "|" + from + "->" + toType + "|" + to;
        deduped.putIfAbsent(key, new GraphEdgeDto(from, fromType, to, toType));
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
