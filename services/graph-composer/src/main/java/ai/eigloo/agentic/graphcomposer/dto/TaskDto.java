package ai.eigloo.agentic.graphcomposer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Data Transfer Object for Task operations.
 * Represents a task node in the Agent Graph with associated files.
 */
public class TaskDto {
    
    @NotBlank(message = "Task name cannot be blank")
    @Size(max = 255, message = "Task name cannot exceed 255 characters")
    private String name;
    
    @Size(max = 500, message = "Task label cannot exceed 500 characters")
    private String label;
    
    private String upstreamPlanId;
    
    private List<ExecutorFileDto> files;

    public TaskDto() {
    }

    public TaskDto(String name, String label, String upstreamPlanId, List<ExecutorFileDto> files) {
        this.name = name;
        this.label = label;
        this.upstreamPlanId = upstreamPlanId;
        this.files = files;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getUpstreamPlanId() {
        return upstreamPlanId;
    }

    public void setUpstreamPlanId(String upstreamPlanId) {
        this.upstreamPlanId = upstreamPlanId;
    }

    public List<ExecutorFileDto> getFiles() {
        return files;
    }

    public void setFiles(List<ExecutorFileDto> files) {
        this.files = files;
    }
}