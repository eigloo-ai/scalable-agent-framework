package ai.eigloo.agentic.graphcomposer.dto;

/**
 * Enumeration representing the status of an Agent Graph.
 * Used to track the lifecycle state of graphs in the system.
 */
public enum GraphStatus {
    /**
     * Graph has been created but not yet submitted for execution
     */
    NEW,

    /**
     * Graph has been loaded and is ready to execute
     */
    ACTIVE,
    
    /**
     * Graph is currently being executed
     */
    RUNNING,
    
    /**
     * Graph execution has been stopped or completed successfully
     */
    STOPPED,
    
    /**
     * Graph execution encountered an error
     */
    ERROR
}
