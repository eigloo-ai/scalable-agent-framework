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
     * Graph is currently being executed.
     */
    RUNNING,
    
    /**
     * Graph execution has been stopped.
     */
    STOPPED,
    
    /**
     * Graph execution encountered an error.
     */
    ERROR
}
