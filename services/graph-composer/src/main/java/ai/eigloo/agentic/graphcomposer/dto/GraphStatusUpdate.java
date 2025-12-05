package ai.eigloo.agentic.graphcomposer.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating the status of an Agent Graph.
 */
public class GraphStatusUpdate {
    
    @NotNull(message = "Status cannot be null")
    private GraphStatus status;

    public GraphStatusUpdate() {
    }

    public GraphStatusUpdate(GraphStatus status) {
        this.status = status;
    }

    public GraphStatus getStatus() {
        return status;
    }

    public void setStatus(GraphStatus status) {
        this.status = status;
    }
}