package ai.eigloo.agentic.graphbuilder.validation;

import ai.eigloo.agentic.graph.model.AgentGraph;
import ai.eigloo.agentic.graph.model.Plan;
import ai.eigloo.agentic.graph.model.Task;
import ai.eigloo.agentic.graph.exception.GraphValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Validates that the Python code bundles (plans and tasks) referenced in an agent graph
 * have the required directory structure and files.
 * 
 * <p>This validator ensures that each plan and task has the necessary Python files
 * and directory structure before the graph can be executed.</p>
 */
public class PythonCodeBundleValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(PythonCodeBundleValidator.class);
    
    private static final String PLANS_DIR = "plans";
    private static final String TASKS_DIR = "tasks";
    private static final String PLAN_PY_FILE = "plan.py";
    private static final String TASK_PY_FILE = "task.py";
    private static final String REQUIREMENTS_FILE = "requirements.txt";
    
    /**
     * Validates that all plans and tasks in the agent graph have the required
     * Python code bundle structure.
     * 
     * @param agentGraph The agent graph to validate
     * @param specificationDirectory The root directory containing the specification
     * @throws GraphValidationException if validation fails
     */
    public static void validate(AgentGraph agentGraph, Path specificationDirectory) throws GraphValidationException {
        logger.debug("Validating Python code bundles for {} plans and {} tasks", 
            agentGraph.planCount(), agentGraph.taskCount());
        
        // Validate all plans
        for (Plan plan : agentGraph.plans().values()) {
            validatePlanDirectory(plan.planSource(), plan.name());
        }
        
        // Validate all tasks
        for (Task task : agentGraph.tasks().values()) {
            validateTaskDirectory(task.taskSource(), task.name());
        }
        
        logger.debug("Python code bundle validation completed successfully");
    }
    
    /**
     * Validates that a plan directory has the required structure and files.
     * 
     * @param planDir The plan directory to validate
     * @param planName The name of the plan (for error messages)
     * @throws GraphValidationException if validation fails
     */
    private static void validatePlanDirectory(Path planDir, String planName) throws GraphValidationException {
        if (!Files.exists(planDir)) {
            throw GraphValidationException.invalidGraph(
                GraphValidationException.ViolationType.MISSING_DIRECTORY,
                planName,
                String.format("Plan directory does not exist: %s", planDir));
        }
        
        Path planPyFile = planDir.resolve(PLAN_PY_FILE);
        if (!Files.exists(planPyFile)) {
            throw GraphValidationException.invalidGraph(
                GraphValidationException.ViolationType.MISSING_PYTHON_FILE,
                planName,
                String.format("Plan Python file not found: %s", planPyFile));
        }
        
        // requirements.txt is optional for plans
        Path requirementsFile = planDir.resolve(REQUIREMENTS_FILE);
        if (Files.exists(requirementsFile)) {
            logger.debug("Found requirements.txt for plan: {}", planName);
        } else {
            logger.debug("No requirements.txt found for plan: {} (optional)", planName);
        }
    }
    
    /**
     * Validates that a task directory has the required structure and files.
     * 
     * @param taskDir The task directory to validate
     * @param taskName The name of the task (for error messages)
     * @throws GraphValidationException if validation fails
     */
    private static void validateTaskDirectory(Path taskDir, String taskName) throws GraphValidationException {
        if (!Files.exists(taskDir)) {
            throw GraphValidationException.invalidGraph(
                GraphValidationException.ViolationType.MISSING_DIRECTORY,
                taskName,
                String.format("Task directory does not exist: %s", taskDir));
        }
        
        Path taskPyFile = taskDir.resolve(TASK_PY_FILE);
        if (!Files.exists(taskPyFile)) {
            throw GraphValidationException.invalidGraph(
                GraphValidationException.ViolationType.MISSING_PYTHON_FILE,
                taskName,
                String.format("Task Python file not found: %s", taskPyFile));
        }
        
        // requirements.txt is optional for tasks
        Path requirementsFile = taskDir.resolve(REQUIREMENTS_FILE);
        if (Files.exists(requirementsFile)) {
            logger.debug("Found requirements.txt for task: {}", taskName);
        } else {
            logger.debug("No requirements.txt found for task: {} (optional)", taskName);
        }
    }
} 