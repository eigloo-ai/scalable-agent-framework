package ai.eigloo.agentic.graph.exception;

/**
 * Exception thrown when graph persistence operations fail.
 */
public class GraphPersistenceException extends RuntimeException {
    private final String correlationId;
    private final String graphName;

    public GraphPersistenceException(String message) {
        super(message);
        this.correlationId = null;
        this.graphName = null;
    }

    public GraphPersistenceException(String message, Throwable cause) {
        super(message, cause);
        this.correlationId = null;
        this.graphName = null;
    }

    public GraphPersistenceException(String message, String correlationId, String graphName) {
        super(message);
        this.correlationId = correlationId;
        this.graphName = graphName;
    }

    public GraphPersistenceException(String message, Throwable cause, String correlationId, String graphName) {
        super(message, cause);
        this.correlationId = correlationId;
        this.graphName = graphName;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getGraphName() {
        return graphName;
    }
}


