package ai.eigloo.agentic.graph.model;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a complete agent graph specification.
 *
 * @param tenantId tenant identifier
 * @param name graph name
 * @param plans graph plans by name
 * @param tasks graph tasks by name
 * @param edges canonical directed edges
 */
public record AgentGraph(
        String tenantId,
        String name,
        Map<String, Plan> plans,
        Map<String, Task> tasks,
        List<GraphEdge> edges
) {

    public AgentGraph {
        if (name == null) {
            throw new IllegalArgumentException("Graph name cannot be null");
        }
        if (plans == null) {
            throw new IllegalArgumentException("Plans map cannot be null");
        }
        if (tasks == null) {
            throw new IllegalArgumentException("Tasks map cannot be null");
        }
        if (edges == null) {
            throw new IllegalArgumentException("Edges cannot be null");
        }

        plans = Map.copyOf(plans);
        tasks = Map.copyOf(tasks);
        edges = List.copyOf(edges);
    }

    /**
     * Backward-compatible constructor without tenantId.
     */
    public AgentGraph(
            String name,
            Map<String, Plan> plans,
            Map<String, Task> tasks,
            List<GraphEdge> edges
    ) {
        this(null, name, plans, tasks, edges);
    }

    public static AgentGraph of(
            String name,
            Map<String, Plan> plans,
            Map<String, Task> tasks,
            List<GraphEdge> edges
    ) {
        return new AgentGraph(name, plans, tasks, edges);
    }

    public static AgentGraph of(
            String tenantId,
            String name,
            Map<String, Plan> plans,
            Map<String, Task> tasks,
            List<GraphEdge> edges
    ) {
        return new AgentGraph(tenantId, name, plans, tasks, edges);
    }

    public static AgentGraph empty(String name) {
        return new AgentGraph(name, Map.of(), Map.of(), List.of());
    }

    public static AgentGraph empty(String tenantId, String name) {
        return new AgentGraph(tenantId, name, Map.of(), Map.of(), List.of());
    }

    public static AgentGraph empty() {
        return empty("EmptyGraph");
    }

    public Plan getPlan(String planName) {
        return plans.get(planName);
    }

    public Task getTask(String taskName) {
        return tasks.get(taskName);
    }

    public Set<String> getDownstreamTasks(String planName) {
        LinkedHashSet<String> downstream = new LinkedHashSet<>();
        for (GraphEdge edge : edges) {
            if (edge.fromType() == GraphNodeType.PLAN
                    && edge.toType() == GraphNodeType.TASK
                    && edge.from().equals(planName)) {
                downstream.add(edge.to());
            }
        }
        return Set.copyOf(downstream);
    }

    public String getUpstreamPlan(String taskName) {
        String upstreamPlan = null;
        for (GraphEdge edge : edges) {
            if (edge.fromType() == GraphNodeType.PLAN
                    && edge.toType() == GraphNodeType.TASK
                    && edge.to().equals(taskName)) {
                if (upstreamPlan != null && !upstreamPlan.equals(edge.from())) {
                    throw new IllegalStateException("Task '" + taskName + "' has multiple upstream plans");
                }
                upstreamPlan = edge.from();
            }
        }
        return upstreamPlan;
    }

    public Set<String> getUpstreamTasks(String planName) {
        LinkedHashSet<String> upstreamTasks = new LinkedHashSet<>();
        for (GraphEdge edge : edges) {
            if (edge.fromType() == GraphNodeType.TASK
                    && edge.toType() == GraphNodeType.PLAN
                    && edge.to().equals(planName)) {
                upstreamTasks.add(edge.from());
            }
        }
        return Set.copyOf(upstreamTasks);
    }

    public String getDownstreamPlan(String taskName) {
        String downstreamPlan = null;
        for (GraphEdge edge : edges) {
            if (edge.fromType() == GraphNodeType.TASK
                    && edge.toType() == GraphNodeType.PLAN
                    && edge.from().equals(taskName)) {
                if (downstreamPlan != null && !downstreamPlan.equals(edge.to())) {
                    throw new IllegalStateException("Task '" + taskName + "' has multiple downstream plans");
                }
                downstreamPlan = edge.to();
            }
        }
        return downstreamPlan;
    }

    public Set<String> getAllPlanNames() {
        return plans.keySet();
    }

    public Set<String> getAllTaskNames() {
        return tasks.keySet();
    }

    public boolean isEmpty() {
        return plans.isEmpty() && tasks.isEmpty();
    }

    public int planCount() {
        return plans.size();
    }

    public int taskCount() {
        return tasks.size();
    }

    public int totalNodeCount() {
        return plans.size() + tasks.size();
    }
}
