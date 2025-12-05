package ai.eigloo.agentic.graphbuilder.exception;

/**
 * Exception thrown when Docker image building operations fail.
 */
public class DockerBuildException extends RuntimeException {
    
    private final String correlationId;
    private final String imageName;
    
    public DockerBuildException(String message) {
        super(message);
        this.correlationId = null;
        this.imageName = null;
    }
    
    public DockerBuildException(String message, Throwable cause) {
        super(message, cause);
        this.correlationId = null;
        this.imageName = null;
    }
    
    public DockerBuildException(String message, String correlationId, String imageName) {
        super(message);
        this.correlationId = correlationId;
        this.imageName = imageName;
    }
    
    public DockerBuildException(String message, Throwable cause, String correlationId, String imageName) {
        super(message, cause);
        this.correlationId = correlationId;
        this.imageName = imageName;
    }
    
    public String getCorrelationId() {
        return correlationId;
    }
    
    public String getImageName() {
        return imageName;
    }
}