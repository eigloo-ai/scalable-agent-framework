package ai.eigloo.agentic.graphbuilder.dto;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for bundle upload requests containing tenant ID and file.
 */
public class BundleUploadRequest {
    
    @NotBlank(message = "Tenant ID is required")
    private String tenantId;
    
    @NotNull(message = "File is required")
    private MultipartFile file;

    public BundleUploadRequest() {}

    public BundleUploadRequest(String tenantId, MultipartFile file) {
        this.tenantId = tenantId;
        this.file = file;
    }

    // Getters and setters
    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }
}