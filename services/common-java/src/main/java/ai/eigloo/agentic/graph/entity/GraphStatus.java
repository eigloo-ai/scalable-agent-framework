package ai.eigloo.agentic.graph.entity;

/**
 * Enumeration representing the status of an agent graph.
 */
public enum GraphStatus {
    /**
     * Graph has been created but not yet executed.
     */
    NEW,

    /**
     * Graph has been loaded and is ready to execute.
     */
    ACTIVE,

    /**
     * Graph has been retired and cannot be executed.
     */
    ARCHIVED;

    /**
     * Returns true when a graph can transition from the current status to the target status.
     */
    public boolean canTransitionTo(GraphStatus target) {
        if (target == null) {
            return false;
        }
        if (this == target) {
            return true;
        }
        return switch (this) {
            case NEW -> target == ACTIVE || target == ARCHIVED;
            case ACTIVE -> target == ARCHIVED;
            case ARCHIVED -> false;
        };
    }
}
