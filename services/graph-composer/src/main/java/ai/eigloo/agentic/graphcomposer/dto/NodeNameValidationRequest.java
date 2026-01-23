package ai.eigloo.agentic.graphcomposer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * Request DTO for validating node names.
 */
public class NodeNameValidationRequest {
    
    @NotBlank(message = "Node name cannot be blank")
    @Size(max = 255, message = "Node name cannot exceed 255 characters")
    private String nodeName;
    
    @NotBlank(message = "Graph ID cannot be blank")
    private String graphId;
    
    private String excludeNodeId; // For rename operations
    
    private Set<String> existingNodeNames; // For uniqueness validation

    public NodeNameValidationRequest() {
    }

    public NodeNameValidationRequest(String nodeName, String graphId, String excludeNodeId) {
        this.nodeName = nodeName;
        this.graphId = graphId;
        this.excludeNodeId = excludeNodeId;
    }

    public NodeNameValidationRequest(String nodeName, String graphId, String excludeNodeId, Set<String> existingNodeNames) {
        this.nodeName = nodeName;
        this.graphId = graphId;
        this.excludeNodeId = excludeNodeId;
        this.existingNodeNames = existingNodeNames;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getGraphId() {
        return graphId;
    }

    public void setGraphId(String graphId) {
        this.graphId = graphId;
    }

    public String getExcludeNodeId() {
        return excludeNodeId;
    }

    public void setExcludeNodeId(String excludeNodeId) {
        this.excludeNodeId = excludeNodeId;
    }

    public Set<String> getExistingNodeNames() {
        return existingNodeNames;
    }

    public void setExistingNodeNames(Set<String> existingNodeNames) {
        this.existingNodeNames = existingNodeNames;
    }
}