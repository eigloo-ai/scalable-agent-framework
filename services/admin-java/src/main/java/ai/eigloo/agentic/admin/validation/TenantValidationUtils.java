package ai.eigloo.agentic.admin.validation;

import ai.eigloo.agentic.admin.dto.DeleteTenantRequest;
import ai.eigloo.agentic.admin.dto.DisableTenantRequest;
import ai.eigloo.agentic.admin.dto.ProvisionTenantRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Utility class for validating tenant-related input data.
 * Provides comprehensive validation for tenant ID format, length constraints, and required fields.
 */
public class TenantValidationUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(TenantValidationUtils.class);
    
    // Tenant ID format: alphanumeric, hyphens, underscores only, 1-50 characters
    private static final Pattern TENANT_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,50}$");
    
    // Tenant name format: alphanumeric, spaces, hyphens, underscores, 1-50 characters
    private static final Pattern TENANT_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s_-]{1,50}$");
    
    // Notes format: any characters except control characters, 0-512 characters
    private static final Pattern NOTES_PATTERN = Pattern.compile("^[^\\x00-\\x1F\\x7F]{0,512}$");
    
    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    
    /**
     * Validates tenant ID format and constraints.
     * 
     * @param tenantId the tenant ID to validate
     * @return ValidationResult with validation status and error messages
     */
    public static ValidationResult validateTenantId(String tenantId) {
        if (tenantId == null) {
            return ValidationResult.error("Tenant ID cannot be null");
        }
        
        if (tenantId.trim().isEmpty()) {
            return ValidationResult.error("Tenant ID cannot be empty");
        }
        
        if (tenantId.length() > 50) {
            return ValidationResult.error("Tenant ID must not exceed 50 characters");
        }
        
        if (!TENANT_ID_PATTERN.matcher(tenantId).matches()) {
            return ValidationResult.error("Tenant ID must contain only alphanumeric characters, hyphens, and underscores");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Validates tenant name format and constraints.
     * 
     * @param tenantName the tenant name to validate
     * @return ValidationResult with validation status and error messages
     */
    public static ValidationResult validateTenantName(String tenantName) {
        if (tenantName == null) {
            return ValidationResult.error("Tenant name cannot be null");
        }
        
        if (tenantName.trim().isEmpty()) {
            return ValidationResult.error("Tenant name cannot be empty");
        }
        
        if (tenantName.length() > 50) {
            return ValidationResult.error("Tenant name must not exceed 50 characters");
        }
        
        if (!TENANT_NAME_PATTERN.matcher(tenantName).matches()) {
            return ValidationResult.error("Tenant name must contain only alphanumeric characters, spaces, hyphens, and underscores");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Validates notes format and constraints.
     * 
     * @param notes the notes to validate
     * @return ValidationResult with validation status and error messages
     */
    public static ValidationResult validateNotes(String notes) {
        if (notes == null) {
            return ValidationResult.success(); // Notes are optional
        }
        
        if (notes.length() > 512) {
            return ValidationResult.error("Notes must not exceed 512 characters");
        }
        
        // Check for control characters (0x00-0x1F and 0x7F)
        for (int i = 0; i < notes.length(); i++) {
            char c = notes.charAt(i);
            if ((c >= 0x00 && c <= 0x1F) || c == 0x7F) {
                return ValidationResult.error("Notes contain invalid control characters");
            }
        }
        
        return ValidationResult.success();
    }
    

    
    /**
     * Validates a ProvisionTenantRequest using both custom validation and Bean Validation.
     * 
     * @param request the request to validate
     * @return ValidationResult with validation status and error messages
     */
    public static ValidationResult validateProvisionTenantRequest(ProvisionTenantRequest request) {
        if (request == null) {
            return ValidationResult.error("Request cannot be null");
        }
        
        // Custom validation
        ValidationResult tenantIdResult = validateTenantId(request.getTenantId());
        if (!tenantIdResult.isValid()) {
            return tenantIdResult;
        }
        
        ValidationResult tenantNameResult = validateTenantName(request.getTenantName());
        if (!tenantNameResult.isValid()) {
            return tenantNameResult;
        }
        
        ValidationResult notesResult = validateNotes(request.getProvisionNotes());
        if (!notesResult.isValid()) {
            return notesResult;
        }
        
        // Bean Validation
        Set<ConstraintViolation<ProvisionTenantRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder("Validation failed: ");
            for (ConstraintViolation<ProvisionTenantRequest> violation : violations) {
                errorMessage.append(violation.getPropertyPath()).append(": ").append(violation.getMessage()).append("; ");
            }
            return ValidationResult.error(errorMessage.toString().trim());
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Validates a DisableTenantRequest using both custom validation and Bean Validation.
     * 
     * @param request the request to validate
     * @return ValidationResult with validation status and error messages
     */
    public static ValidationResult validateDisableTenantRequest(DisableTenantRequest request) {
        if (request == null) {
            return ValidationResult.error("Request cannot be null");
        }
        
        // Custom validation
        ValidationResult tenantIdResult = validateTenantId(request.getTenantId());
        if (!tenantIdResult.isValid()) {
            return tenantIdResult;
        }
        
        ValidationResult notesResult = validateNotes(request.getDisableNotes());
        if (!notesResult.isValid()) {
            return notesResult;
        }
        
        // Bean Validation
        Set<ConstraintViolation<DisableTenantRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder("Validation failed: ");
            for (ConstraintViolation<DisableTenantRequest> violation : violations) {
                errorMessage.append(violation.getPropertyPath()).append(": ").append(violation.getMessage()).append("; ");
            }
            return ValidationResult.error(errorMessage.toString().trim());
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Validates a DeleteTenantRequest using both custom validation and Bean Validation.
     * 
     * @param request the request to validate
     * @return ValidationResult with validation status and error messages
     */
    public static ValidationResult validateDeleteTenantRequest(DeleteTenantRequest request) {
        if (request == null) {
            return ValidationResult.error("Request cannot be null");
        }
        
        // Custom validation
        ValidationResult tenantIdResult = validateTenantId(request.getTenantId());
        if (!tenantIdResult.isValid()) {
            return tenantIdResult;
        }
        
        ValidationResult notesResult = validateNotes(request.getDeletionNotes());
        if (!notesResult.isValid()) {
            return notesResult;
        }
        
        // Bean Validation
        Set<ConstraintViolation<DeleteTenantRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder("Validation failed: ");
            for (ConstraintViolation<DeleteTenantRequest> violation : violations) {
                errorMessage.append(violation.getPropertyPath()).append(": ").append(violation.getMessage()).append("; ");
            }
            return ValidationResult.error(errorMessage.toString().trim());
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Result of validation operations.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        
        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult error(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
} 