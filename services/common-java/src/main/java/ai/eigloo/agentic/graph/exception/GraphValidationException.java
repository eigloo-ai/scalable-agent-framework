package ai.eigloo.agentic.graph.exception;

/**
 * Exception thrown when agent graph validation fails.
 * 
 * <p>This exception is thrown when the agent graph specification violates
 * integrity constraints such as:</p>
 * <ul>
 *   <li>Duplicate node names across plans and tasks</li>
 *   <li>Dangling edge references to non-existent nodes</li>
 *   <li>Tasks with multiple upstream plans (violating single-plan constraint)</li>
 *   <li>Orphaned nodes with no connections</li>
 *   <li>Invalid node names or identifiers</li>
 * </ul>
 * 
 * <p>The exception includes structured error information to help identify
 * the specific validation failure and affected nodes.</p>
 */
public class GraphValidationException extends RuntimeException {
    
    /**
     * Types of validation violations.
     */
    public enum ViolationType {
        DUPLICATE_NAME("Duplicate node name"),
        DANGLING_EDGE("Edge references non-existent node"),
        TASK_MULTIPLE_PLANS("Task has multiple upstream plans"),
        ORPHANED_NODE("Node has no connections"),
        INVALID_NAME("Invalid node name"),
        MISSING_DIRECTORY("Missing required directory"),
        MISSING_PYTHON_FILE("Missing required Python file"),
        INVALID_STRUCTURE("Invalid graph structure");
        
        private final String description;
        
        ViolationType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private final ViolationType violationType;
    private final String nodeId;
    
    /**
     * Creates a new GraphValidationException with a formatted message.
     * 
     * @param message The error message format
     * @param args The message arguments
     * @return A new GraphValidationException
     */
    public static GraphValidationException invalidGraph(String message, Object... args) {
        return new GraphValidationException(String.format(message, args));
    }
    
    /**
     * Creates a new GraphValidationException with violation details.
     * 
     * @param violationType The type of validation violation
     * @param nodeId The ID of the affected node
     * @param message The error message
     * @return A new GraphValidationException
     */
    public static GraphValidationException invalidGraph(ViolationType violationType, String nodeId, String message) {
        return new GraphValidationException(violationType, nodeId, message);
    }
    
    /**
     * Creates a new GraphValidationException with violation details and cause.
     * 
     * @param violationType The type of validation violation
     * @param nodeId The ID of the affected node
     * @param message The error message
     * @param cause The underlying cause
     * @return A new GraphValidationException
     */
    public static GraphValidationException invalidGraph(ViolationType violationType, String nodeId, String message, Throwable cause) {
        return new GraphValidationException(violationType, nodeId, message, cause);
    }
    
    /**
     * Creates a new GraphValidationException.
     * 
     * @param message The error message
     */
    public GraphValidationException(String message) {
        super(message);
        this.violationType = null;
        this.nodeId = null;
    }
    
    /**
     * Creates a new GraphValidationException with cause.
     * 
     * @param message The error message
     * @param cause The underlying cause
     */
    public GraphValidationException(String message, Throwable cause) {
        super(message, cause);
        this.violationType = null;
        this.nodeId = null;
    }
    
    /**
     * Creates a new GraphValidationException with violation details.
     * 
     * @param violationType The type of validation violation
     * @param nodeId The ID of the affected node
     * @param message The error message
     */
    public GraphValidationException(ViolationType violationType, String nodeId, String message) {
        super(message);
        this.violationType = violationType;
        this.nodeId = nodeId;
    }
    
    /**
     * Creates a new GraphValidationException with violation details and cause.
     * 
     * @param violationType The type of validation violation
     * @param nodeId The ID of the affected node
     * @param message The error message
     * @param cause The underlying cause
     */
    public GraphValidationException(ViolationType violationType, String nodeId, String message, Throwable cause) {
        super(message, cause);
        this.violationType = violationType;
        this.nodeId = nodeId;
    }
    
    /**
     * Gets the type of validation violation.
     * 
     * @return The violation type, or null if not specified
     */
    public ViolationType getViolationType() {
        return violationType;
    }
    
    /**
     * Gets the ID of the affected node.
     * 
     * @return The node ID, or null if not specified
     */
    public String getNodeId() {
        return nodeId;
    }
} 