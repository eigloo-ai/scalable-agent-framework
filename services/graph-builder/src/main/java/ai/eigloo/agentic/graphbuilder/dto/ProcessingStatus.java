package ai.eigloo.agentic.graphbuilder.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO representing the processing status of a graph bundle upload.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProcessingStatus {
    private String processId;
    private String status;
    private List<ProcessingStep> steps;
    private String errorMessage;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;

    public ProcessingStatus() {}

    public ProcessingStatus(String processId, String status) {
        this.processId = processId;
        this.status = status;
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

    public List<ProcessingStep> getSteps() {
        return steps;
    }

    public void setSteps(List<ProcessingStep> steps) {
        this.steps = steps;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
}