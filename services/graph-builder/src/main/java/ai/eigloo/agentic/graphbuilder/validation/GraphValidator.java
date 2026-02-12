package ai.eigloo.agentic.graphbuilder.validation;

import ai.eigloo.agentic.graph.model.AgentGraph;
import ai.eigloo.agentic.graph.model.GraphEdge;
import ai.eigloo.agentic.graph.model.GraphNodeType;
import ai.eigloo.agentic.graph.model.Plan;
import ai.eigloo.agentic.graph.model.Task;
import ai.eigloo.agentic.graph.exception.GraphValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Set;

/**
 * Utility class for validating agent graph integrity.
 * 
 * <p>This class provides comprehensive validation of agent graphs to ensure
 * they meet all integrity constraints and business rules. Validation includes:</p>
 * <ul>
 *   <li>Duplicate name detection across plans and tasks</li>
 *   <li>Dangling edge reference detection</li>
 *   <li>Task single-upstream-plan constraint enforcement</li>
 *   <li>Graph connectivity validation</li>
 *   <li>Directory structure validation</li>
 *   <li>Python file existence validation</li>
 * </ul>
 */
public class GraphValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(GraphValidator.class);
    
    private static final String PLANS_DIR = "plans";
    private static final String TASKS_DIR = "tasks";
    private static final String PLAN_PY_FILE = "plan.py";
    private static final String TASK_PY_FILE = "task.py";
    private static final String REQUIREMENTS_FILE = "requirements.txt";
    
    /**
     * Validates the integrity of an agent graph.
     * 
     * @param graph The agent graph to validate
     * @throws GraphValidationException if validation fails
     */
    public static void validate(AgentGraph graph) throws GraphValidationException {
        logger.debug("Validating agent graph with {} plans and {} tasks", 
            graph.planCount(), graph.taskCount());
        
        // Check for duplicate names first
        validateUniqueNames(graph);
        
        // Validate node names are valid identifiers
        validateNodeNames(graph);
        
        // Check for dangling edges
        validateEdgeReferences(graph);
        
        // Check task single-upstream-plan constraint
        validateTaskUpstreamConstraint(graph);
        
        // Validate that every plan feeds into 1 or more tasks
        validatePlanFeedsIntoTasks(graph);
        
        // Validate that every task has an upstream/parent plan
        validateTaskHasUpstreamPlan(graph);
        
        // Check for orphaned nodes
        validateConnectivity(graph);
        
        logger.debug("Graph validation completed successfully");
    }
    
    /**
     * Validates the directory structure for a graph specification.
     * 
     * @param specDir The specification directory
     * @param graph The agent graph
     * @throws GraphValidationException if validation fails
     */
    public static void validateDirectoryStructure(Path specDir, AgentGraph graph) throws GraphValidationException {
        logger.debug("Validating directory structure for graph specification: {}", specDir);
        
        // Check that plans/ and tasks/ directories exist
        Path plansDir = specDir.resolve(PLANS_DIR);
        Path tasksDir = specDir.resolve(TASKS_DIR);
        
        if (!Files.exists(plansDir)) {
            throw GraphValidationException.invalidGraph(
                GraphValidationException.ViolationType.MISSING_DIRECTORY,
                null,
                "Plans directory does not exist: " + plansDir);
        }
        
        if (!Files.exists(tasksDir)) {
            throw GraphValidationException.invalidGraph(
                GraphValidationException.ViolationType.MISSING_DIRECTORY,
                null,
                "Tasks directory does not exist: " + tasksDir);
        }
        
        // Validate each plan has required files
        for (Plan plan : graph.plans().values()) {
            validatePlanDirectory(plan, plansDir);
        }
        
        // Validate each task has required files
        for (Task task : graph.tasks().values()) {
            validateTaskDirectory(task, tasksDir);
        }
        
        logger.debug("Directory structure validation completed successfully");
    }
    
    private static void validateUniqueNames(AgentGraph graph) throws GraphValidationException {
        Set<String> allNames = new HashSet<>();
        
        // Check plan names
        for (String planName : graph.getAllPlanNames()) {
            if (!allNames.add(planName)) {
                throw GraphValidationException.invalidGraph(
                    GraphValidationException.ViolationType.DUPLICATE_NAME,
                    planName,
                    "Duplicate plan name: " + planName);
            }
        }
        
        // Check task names
        for (String taskName : graph.getAllTaskNames()) {
            if (!allNames.add(taskName)) {
                throw GraphValidationException.invalidGraph(
                    GraphValidationException.ViolationType.DUPLICATE_NAME,
                    taskName,
                    "Duplicate task name: " + taskName);
            }
        }
    }
    
    private static void validateEdgeReferences(AgentGraph graph) throws GraphValidationException {
        for (GraphEdge edge : graph.edges()) {
            if (edge.fromType() == GraphNodeType.PLAN && !graph.plans().containsKey(edge.from())) {
                throw GraphValidationException.invalidGraph(
                        GraphValidationException.ViolationType.DANGLING_EDGE,
                        edge.from(),
                        String.format("Edge references non-existent plan: %s", edge.from()));
            }
            if (edge.fromType() == GraphNodeType.TASK && !graph.tasks().containsKey(edge.from())) {
                throw GraphValidationException.invalidGraph(
                        GraphValidationException.ViolationType.DANGLING_EDGE,
                        edge.from(),
                        String.format("Edge references non-existent task: %s", edge.from()));
            }
            if (edge.toType() == GraphNodeType.PLAN && !graph.plans().containsKey(edge.to())) {
                throw GraphValidationException.invalidGraph(
                        GraphValidationException.ViolationType.DANGLING_EDGE,
                        edge.to(),
                        String.format("Edge references non-existent plan: %s", edge.to()));
            }
            if (edge.toType() == GraphNodeType.TASK && !graph.tasks().containsKey(edge.to())) {
                throw GraphValidationException.invalidGraph(
                        GraphValidationException.ViolationType.DANGLING_EDGE,
                        edge.to(),
                        String.format("Edge references non-existent task: %s", edge.to()));
            }
        }
    }
    
    private static void validateTaskUpstreamConstraint(AgentGraph graph) throws GraphValidationException {
        HashMap<String, String> taskToUpstreamPlan = new HashMap<>();
        for (GraphEdge edge : graph.edges()) {
            if (edge.fromType() == GraphNodeType.PLAN && edge.toType() == GraphNodeType.TASK) {
                String previous = taskToUpstreamPlan.put(edge.to(), edge.from());
                if (previous != null && !previous.equals(edge.from())) {
                    throw GraphValidationException.invalidGraph(
                        GraphValidationException.ViolationType.INVALID_STRUCTURE,
                        edge.to(),
                        String.format("Task %s has multiple upstream plans (%s, %s)", edge.to(), previous, edge.from()));
                }
            }
        }

        for (Task task : graph.tasks().values()) {
            if (!taskToUpstreamPlan.containsKey(task.name())) {
                throw GraphValidationException.invalidGraph(
                    GraphValidationException.ViolationType.INVALID_STRUCTURE,
                    task.name(),
                    String.format("Task %s has no upstream plan", task.name()));
            }
        }
    }
    
    private static void validateConnectivity(AgentGraph graph) throws GraphValidationException {
        // Find all connected nodes
        Set<String> connectedNodes = new HashSet<>();
        
        for (GraphEdge edge : graph.edges()) {
            connectedNodes.add(edge.from());
            connectedNodes.add(edge.to());
        }
        
        // Check for orphaned nodes
        for (String planName : graph.getAllPlanNames()) {
            if (!connectedNodes.contains(planName)) {
                throw GraphValidationException.invalidGraph(
                    GraphValidationException.ViolationType.ORPHANED_NODE,
                    planName,
                    String.format("Plan %s has no connections", planName));
            }
        }
        
        for (String taskName : graph.getAllTaskNames()) {
            if (!connectedNodes.contains(taskName)) {
                throw GraphValidationException.invalidGraph(
                    GraphValidationException.ViolationType.ORPHANED_NODE,
                    taskName,
                    String.format("Task %s has no connections", taskName));
            }
        }
    }
    
    private static void validatePlanFeedsIntoTasks(AgentGraph graph) throws GraphValidationException {
        // Validate that every plan feeds into 1 or more tasks
        for (String planName : graph.getAllPlanNames()) {
            Set<String> downstreamTasks = graph.getDownstreamTasks(planName);
            if (downstreamTasks.isEmpty()) {
                throw GraphValidationException.invalidGraph(
                    GraphValidationException.ViolationType.INVALID_STRUCTURE,
                    planName,
                    String.format("Plan %s does not feed into any tasks", planName));
            }
        }
    }
    
    private static void validateTaskHasUpstreamPlan(AgentGraph graph) throws GraphValidationException {
        // Validate that every task has an upstream/parent plan
        for (String taskName : graph.getAllTaskNames()) {
            String upstreamPlan = graph.getUpstreamPlan(taskName);
            if (upstreamPlan == null) {
                throw GraphValidationException.invalidGraph(
                    GraphValidationException.ViolationType.INVALID_STRUCTURE,
                    taskName,
                    String.format("Task %s has no upstream plan", taskName));
            }
        }
    }
    

    
    private static void validateNodeNames(AgentGraph graph) throws GraphValidationException {
        // Validate that all node names are valid identifiers
        for (String planName : graph.getAllPlanNames()) {
            if (!isValidIdentifier(planName)) {
                throw GraphValidationException.invalidGraph(
                    GraphValidationException.ViolationType.INVALID_NAME,
                    planName,
                    String.format("Invalid plan name: %s (must be a valid identifier)", planName));
            }
        }
        
        for (String taskName : graph.getAllTaskNames()) {
            if (!isValidIdentifier(taskName)) {
                throw GraphValidationException.invalidGraph(
                    GraphValidationException.ViolationType.INVALID_NAME,
                    taskName,
                    String.format("Invalid task name: %s (must be a valid identifier)", taskName));
            }
        }
    }
    
    private static boolean isValidIdentifier(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        // Must start with letter or underscore
        if (!Character.isLetter(name.charAt(0)) && name.charAt(0) != '_') {
            return false;
        }
        
        // Must contain only letters, digits, and underscores
        for (int i = 1; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_') {
                return false;
            }
        }
        
        return true;
    }
    
    private static void validatePlanDirectory(Plan plan, Path plansDir) throws GraphValidationException {
        Path planDir = plansDir.resolve(plan.name());
        
        if (!Files.exists(planDir)) {
            throw GraphValidationException.invalidGraph(
                GraphValidationException.ViolationType.MISSING_DIRECTORY,
                plan.name(),
                "Plan directory does not exist: " + planDir);
        }
        
        Path planPyFile = planDir.resolve(PLAN_PY_FILE);
        if (!Files.exists(planPyFile)) {
            throw GraphValidationException.invalidGraph(
                GraphValidationException.ViolationType.MISSING_PYTHON_FILE,
                plan.name(),
                "Plan Python file does not exist: " + planPyFile);
        }
        
        Path requirementsFile = planDir.resolve(REQUIREMENTS_FILE);
        if (!Files.exists(requirementsFile)) {
            throw GraphValidationException.invalidGraph(
                GraphValidationException.ViolationType.MISSING_PYTHON_FILE,
                plan.name(),
                "Plan requirements file does not exist: " + requirementsFile);
        }
    }
    
    private static void validateTaskDirectory(Task task, Path tasksDir) throws GraphValidationException {
        Path taskDir = tasksDir.resolve(task.name());
        
        if (!Files.exists(taskDir)) {
            throw GraphValidationException.invalidGraph(
                GraphValidationException.ViolationType.MISSING_DIRECTORY,
                task.name(),
                "Task directory does not exist: " + taskDir);
        }
        
        Path taskPyFile = taskDir.resolve(TASK_PY_FILE);
        if (!Files.exists(taskPyFile)) {
            throw GraphValidationException.invalidGraph(
                GraphValidationException.ViolationType.MISSING_PYTHON_FILE,
                task.name(),
                "Task Python file does not exist: " + taskPyFile);
        }
        
        Path requirementsFile = taskDir.resolve(REQUIREMENTS_FILE);
        if (!Files.exists(requirementsFile)) {
            throw GraphValidationException.invalidGraph(
                GraphValidationException.ViolationType.MISSING_PYTHON_FILE,
                task.name(),
                "Task requirements file does not exist: " + requirementsFile);
        }
    }
} 
