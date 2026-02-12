package ai.eigloo.agentic.graphcomposer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Data Transfer Object for Plan operations.
 * Represents a plan node in the Agent Graph with associated files.
 */
public class PlanDto {
    
    @NotBlank(message = "Plan name cannot be blank")
    @Size(max = 255, message = "Plan name cannot exceed 255 characters")
    private String name;
    
    @Size(max = 500, message = "Plan label cannot exceed 500 characters")
    private String label;

    private List<ExecutorFileDto> files;

    public PlanDto() {
    }

    public PlanDto(String name, String label, List<ExecutorFileDto> files) {
        this.name = name;
        this.label = label;
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

    public List<ExecutorFileDto> getFiles() {
        return files;
    }

    public void setFiles(List<ExecutorFileDto> files) {
        this.files = files;
    }
}
