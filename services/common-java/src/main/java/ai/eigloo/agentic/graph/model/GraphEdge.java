package ai.eigloo.agentic.graph.model;

/**
 * Canonical directed edge connecting plan/task nodes.
 */
public record GraphEdge(
        String from,
        GraphNodeType fromType,
        String to,
        GraphNodeType toType
) {
    public GraphEdge {
        if (from == null || from.trim().isEmpty()) {
            throw new IllegalArgumentException("Edge source cannot be null or empty");
        }
        if (to == null || to.trim().isEmpty()) {
            throw new IllegalArgumentException("Edge target cannot be null or empty");
        }
        if (fromType == null || toType == null) {
            throw new IllegalArgumentException("Edge node types cannot be null");
        }
        if (fromType == toType) {
            throw new IllegalArgumentException("Edge must connect PLAN->TASK or TASK->PLAN");
        }
    }
}
