package ai.eigloo.agentic.graph.entity;

/**
 * Runtime status for a graph execution instance, keyed by lifetime ID.
 */
public enum GraphRunStatus {
    /**
     * Execution request accepted, but bootstrap messages not fully published yet.
     */
    QUEUED,

    /**
     * Execution has started and messages are flowing through the runtime.
     */
    RUNNING,

    /**
     * Execution finished successfully.
     */
    SUCCEEDED,

    /**
     * Execution failed.
     */
    FAILED,

    /**
     * Execution was canceled.
     */
    CANCELED;

    /**
     * Returns true when this status is terminal.
     */
    public boolean isTerminal() {
        return this == SUCCEEDED || this == FAILED || this == CANCELED;
    }

    /**
     * Returns true when a run can transition from the current status to the target status.
     */
    public boolean canTransitionTo(GraphRunStatus target) {
        if (target == null) {
            return false;
        }
        if (this == target) {
            return true;
        }
        return switch (this) {
            case QUEUED -> target == RUNNING || target == FAILED || target == CANCELED;
            case RUNNING -> target == SUCCEEDED || target == FAILED || target == CANCELED;
            case SUCCEEDED, FAILED, CANCELED -> false;
        };
    }
}
