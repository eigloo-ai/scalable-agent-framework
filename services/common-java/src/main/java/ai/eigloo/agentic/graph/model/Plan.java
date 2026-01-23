package ai.eigloo.agentic.graph.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
 * <p>Plans can have multiple upstream tasks feeding into them, and can feed into
 * multiple downstream tasks, forming a flexible graph structure.</p>
 * 
 * @param name The unique identifier for this plan
 * @param label The human-readable label for this plan
 * @param planSource Path to the Python subproject directory containing the plan implementation
 * @param upstreamTaskIds Set of task IDs that feed into this plan
 * @param files List of ExecutorFile objects containing the plan implementation files
 */
public record Plan(String name, String label, Path planSource, Set<String> upstreamTaskIds, List<ExecutorFile> files) {
    
    /**
     * Compact constructor with validation.
     * 
     * @param name The plan name
     * @param planSource The plan source directory
     * @param upstreamTaskIds The upstream task IDs
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
        if (upstreamTaskIds == null) {
            upstreamTaskIds = new HashSet<>();
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
     * @return A new Plan with no upstream tasks and no files
     */
    public static Plan of(String name, Path planSource) {
        return new Plan(name, name, planSource, new HashSet<>(), new ArrayList<>());
    }
    
    /**
     * Creates a new Plan with the specified name, source directory, and upstream tasks.
     * 
     * @param name The plan name
     * @param planSource The plan source directory
     * @param upstreamTaskIds The upstream task IDs
     * @return A new Plan with no files
     */
    public static Plan of(String name, Path planSource, Set<String> upstreamTaskIds) {
        return new Plan(name, name, planSource, upstreamTaskIds, new ArrayList<>());
    }
    
    /**
     * Returns a new Plan with an additional upstream task.
     * 
     * @param taskId The task ID to add as upstream
     * @return A new Plan with the additional upstream task
     */
    public Plan withUpstreamTask(String taskId) {
        Set<String> newUpstreamTasks = new HashSet<>(upstreamTaskIds);
        newUpstreamTasks.add(taskId);
        return new Plan(name, label, planSource, newUpstreamTasks, files);
    }
    
    /**
     * Returns a new Plan with the specified upstream tasks.
     * 
     * @param taskIds The task IDs to set as upstream
     * @return A new Plan with the specified upstream tasks
     */
    public Plan withUpstreamTasks(Set<String> taskIds) {
        return new Plan(name, label, planSource, new HashSet<>(taskIds), files);
    }
    
    /**
     * Returns a new Plan with the specified files.
     * 
     * @param newFiles The ExecutorFile list to set
     * @return A new Plan with the specified files
     */
    public Plan withFiles(List<ExecutorFile> newFiles) {
        return new Plan(name, label, planSource, upstreamTaskIds, List.copyOf(newFiles));
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
        return new Plan(name, label, planSource, upstreamTaskIds, List.copyOf(newFiles));
    }
} 