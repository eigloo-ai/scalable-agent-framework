package ai.eigloo.agentic.graphcomposer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new Agent Graph.
 */
public class CreateGraphRequest {
    
    @NotBlank(message = "Graph name cannot be blank")
    @Size(max = 255, message = "Graph name cannot exceed 255 characters")
    private String name;
    
    @NotBlank(message = "Tenant ID cannot be blank")
    @Size(max = 100, message = "Tenant ID cannot exceed 100 characters")
    private String tenantId;

    public CreateGraphRequest() {
    }

    public CreateGraphRequest(String name, String tenantId) {
        this.name = name;
        this.tenantId = tenantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}