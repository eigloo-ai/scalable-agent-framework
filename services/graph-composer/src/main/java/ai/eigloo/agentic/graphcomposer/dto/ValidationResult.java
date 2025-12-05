package ai.eigloo.agentic.graphcomposer.dto;

import java.util.List;

/**
 * Response DTO for validation operations.
 * Contains validation results and any error messages.
 */
public class ValidationResult {
    
    private boolean valid;
    private List<String> errors;
    private List<String> warnings;

    public ValidationResult() {
    }

    public ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
        this.valid = valid;
        this.errors = errors;
        this.warnings = warnings;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
}