package ai.eigloo.agentic.graphcomposer.dto;

/**
 * Response DTO for graph execution operations.
 */
public class ExecutionResponse {
    
    private String executionId;
    private String message;
    private String status;

    public ExecutionResponse() {
    }

    public ExecutionResponse(String executionId, String message, String status) {
        this.executionId = executionId;
        this.message = message;
        this.status = status;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}