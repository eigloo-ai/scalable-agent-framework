package ai.eigloo.agentic.graphcomposer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Set;

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
    
    private Set<String> upstreamTaskIds;
    
    private List<ExecutorFileDto> files;

    public PlanDto() {
    }

    public PlanDto(String name, String label, Set<String> upstreamTaskIds, List<ExecutorFileDto> files) {
        this.name = name;
        this.label = label;
        this.upstreamTaskIds = upstreamTaskIds;
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

    public Set<String> getUpstreamTaskIds() {
        return upstreamTaskIds;
    }

    public void setUpstreamTaskIds(Set<String> upstreamTaskIds) {
        this.upstreamTaskIds = upstreamTaskIds;
    }

    public List<ExecutorFileDto> getFiles() {
        return files;
    }

    public void setFiles(List<ExecutorFileDto> files) {
        this.files = files;
    }
}