package ai.eigloo.agentic.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for delete tenant requests.
 */
public class DeleteTenantRequest {
    
    @NotBlank(message = "Tenant ID is required")
    @Size(min = 1, max = 50, message = "Tenant ID must be between 1 and 50 characters")
    @JsonProperty("tenant_id")
    private String tenantId;
    
    @Size(max = 512, message = "Deletion notes must not exceed 512 characters")
    @JsonProperty("deletion_notes")
    private String deletionNotes;
    
    // Default constructor
    public DeleteTenantRequest() {
    }
    
    // Constructor with all fields
    public DeleteTenantRequest(String tenantId, String deletionNotes) {
        this.tenantId = tenantId;
        this.deletionNotes = deletionNotes;
    }
    
    // Getters and Setters
    public String getTenantId() {
        return tenantId;
    }
    
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
    
    public String getDeletionNotes() {
        return deletionNotes;
    }
    
    public void setDeletionNotes(String deletionNotes) {
        this.deletionNotes = deletionNotes;
    }
    
    @Override
    public String toString() {
        return "DeleteTenantRequest{" +
                "tenantId='" + tenantId + '\'' +
                ", deletionNotes='" + deletionNotes + '\'' +
                '}';
    }
} 