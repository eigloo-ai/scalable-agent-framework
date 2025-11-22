package ai.eigloo.agentic.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for provision tenant requests.
 */
public class ProvisionTenantRequest {
    
    @NotBlank(message = "Tenant ID is required")
    @Size(min = 1, max = 50, message = "Tenant ID must be between 1 and 50 characters")
    @JsonProperty("tenant_id")
    private String tenantId;
    
    @NotBlank(message = "Tenant name is required")
    @Size(min = 1, max = 50, message = "Tenant name must be between 1 and 50 characters")
    @JsonProperty("tenant_name")
    private String tenantName;
    
    @Size(max = 512, message = "Provision notes must not exceed 512 characters")
    @JsonProperty("provision_notes")
    private String provisionNotes;
    
    // Default constructor
    public ProvisionTenantRequest() {
    }
    
    // Constructor with all fields
    public ProvisionTenantRequest(String tenantId, String tenantName, String provisionNotes) {
        this.tenantId = tenantId;
        this.tenantName = tenantName;
        this.provisionNotes = provisionNotes;
    }
    
    // Getters and Setters
    public String getTenantId() {
        return tenantId;
    }
    
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
    
    public String getTenantName() {
        return tenantName;
    }
    
    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }
    
    public String getProvisionNotes() {
        return provisionNotes;
    }
    
    public void setProvisionNotes(String provisionNotes) {
        this.provisionNotes = provisionNotes;
    }
    
    @Override
    public String toString() {
        return "ProvisionTenantRequest{" +
                "tenantId='" + tenantId + '\'' +
                ", tenantName='" + tenantName + '\'' +
                ", provisionNotes='" + provisionNotes + '\'' +
                '}';
    }
} 