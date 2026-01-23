package ai.eigloo.agentic.graphcomposer.exception;

import java.util.List;

/**
 * Exception thrown when graph validation fails.
 */
public class GraphValidationException extends RuntimeException {
    
    private final List<String> validationErrors;
    private final List<String> validationWarnings;
    
    public GraphValidationException(String message, List<String> validationErrors, List<String> validationWarnings) {
        super(message);
        this.validationErrors = validationErrors;
        this.validationWarnings = validationWarnings;
    }
    
    public List<String> getValidationErrors() {
        return validationErrors;
    }
    
    public List<String> getValidationWarnings() {
        return validationWarnings;
    }
}