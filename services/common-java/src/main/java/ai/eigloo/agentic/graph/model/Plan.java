package ai.eigloo.agentic.graph.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a plan in the agent graph specification.
 * 
 * <p>A plan is a node in the agent graph that processes results from upstream tasks
 * and produces a plan result that can be consumed by downstream tasks. Each plan
 * has a unique name and is associated with a Python subproject directory containing
 * the plan implementation.</p>
 * 
 * <p>The plan's Python subproject should contain:</p>
 * <ul>
 *   <li>A {@code plan.py} file with a {@code plan(upstream_results: List[TaskResult]) -> PlanResult} function</li>
 *   <li>A {@code requirements.txt} file listing dependencies</li>
 * </ul>
 * 
 * <p>Graph topology is defined by canonical edge records in {@link AgentGraph}.</p>
 * 
 * @param name The unique identifier for this plan
 * @param label The human-readable label for this plan
 * @param planSource Path to the Python subproject directory containing the plan implementation
 * @param files List of ExecutorFile objects containing the plan implementation files
 */
public record Plan(String name, String label, Path planSource, List<ExecutorFile> files) {
    
    /**
     * Compact constructor with validation.
     * 
     * @param name The plan name
     * @param planSource The plan source directory
     * @param files The list of ExecutorFile objects
     * @throws IllegalArgumentException if validation fails
     */
    public Plan {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Plan name cannot be null or empty");
        }
        if (planSource == null) {
            throw new IllegalArgumentException("Plan source cannot be null");
        }
        if (files == null) {
            files = new ArrayList<>();
        } else {
            files = List.copyOf(files);
        }
    }
    
    /**
     * Creates a new Plan with the specified name and source directory.
     * 
     * @param name The plan name
     * @param planSource The plan source directory
     * @return A new Plan with no files
     */
    public static Plan of(String name, Path planSource) {
        return new Plan(name, name, planSource, new ArrayList<>());
    }
    
    /**
     * Returns a new Plan with the specified files.
     * 
     * @param newFiles The ExecutorFile list to set
     * @return A new Plan with the specified files
     */
    public Plan withFiles(List<ExecutorFile> newFiles) {
        return new Plan(name, label, planSource, List.copyOf(newFiles));
    }
    
    /**
     * Returns a new Plan with an additional file.
     * 
     * @param file The ExecutorFile to add
     * @return A new Plan with the additional file
     */
    public Plan withFile(ExecutorFile file) {
        List<ExecutorFile> newFiles = new ArrayList<>(files);
        newFiles.add(file);
        return new Plan(name, label, planSource, List.copyOf(newFiles));
    }
}
