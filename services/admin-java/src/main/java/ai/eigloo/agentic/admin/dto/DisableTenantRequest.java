package ai.eigloo.agentic.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for disable tenant requests.
 */
public class DisableTenantRequest {
    
    @NotBlank(message = "Tenant ID is required")
    @Size(min = 1, max = 50, message = "Tenant ID must be between 1 and 50 characters")
    @JsonProperty("tenant_id")
    private String tenantId;
    
    @Size(max = 512, message = "Disable notes must not exceed 512 characters")
    @JsonProperty("disable_notes")
    private String disableNotes;
    
    // Default constructor
    public DisableTenantRequest() {
    }
    
    // Constructor with all fields
    public DisableTenantRequest(String tenantId, String disableNotes) {
        this.tenantId = tenantId;
        this.disableNotes = disableNotes;
    }
    
    // Getters and Setters
    public String getTenantId() {
        return tenantId;
    }
    
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
    
    public String getDisableNotes() {
        return disableNotes;
    }
    
    public void setDisableNotes(String disableNotes) {
        this.disableNotes = disableNotes;
    }
    
    @Override
    public String toString() {
        return "DisableTenantRequest{" +
                "tenantId='" + tenantId + '\'' +
                ", disableNotes='" + disableNotes + '\'' +
                '}';
    }
} 