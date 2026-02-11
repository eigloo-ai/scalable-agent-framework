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
    FAILED
}
