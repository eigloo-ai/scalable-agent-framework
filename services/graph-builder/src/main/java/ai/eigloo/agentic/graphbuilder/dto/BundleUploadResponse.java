package ai.eigloo.agentic.graphbuilder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO for bundle upload responses containing process ID, status, and optional message.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BundleUploadResponse {
    
    private String processId;
    private String status;
    private String message;

    public BundleUploadResponse() {}

    public BundleUploadResponse(String processId, String status) {
        this.processId = processId;
        this.status = status;
    }

    public BundleUploadResponse(String processId, String status, String message) {
        this.processId = processId;
        this.status = status;
        this.message = message;
    }

    // Getters and setters
    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}