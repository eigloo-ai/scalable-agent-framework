package ai.eigloo.agentic.graphbuilder.exception;

/**
 * Exception thrown when bundle processing operations fail.
 */
public class BundleProcessingException extends RuntimeException {
    
    private final String correlationId;
    private final String step;
    
    public BundleProcessingException(String message) {
        super(message);
        this.correlationId = null;
        this.step = null;
    }
    
    public BundleProcessingException(String message, Throwable cause) {
        super(message, cause);
        this.correlationId = null;
        this.step = null;
    }
    
    public BundleProcessingException(String message, String correlationId, String step) {
        super(message);
        this.correlationId = correlationId;
        this.step = step;
    }
    
    public BundleProcessingException(String message, Throwable cause, String correlationId, String step) {
        super(message, cause);
        this.correlationId = correlationId;
        this.step = step;
    }
    
    public String getCorrelationId() {
        return correlationId;
    }
    
    public String getStep() {
        return step;
    }
}