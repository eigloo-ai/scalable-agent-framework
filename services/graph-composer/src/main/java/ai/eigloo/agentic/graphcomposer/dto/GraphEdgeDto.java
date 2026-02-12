package ai.eigloo.agentic.graphcomposer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Directed edge between plan/task nodes.
 */
public class GraphEdgeDto {

    @NotBlank(message = "Edge 'from' node cannot be blank")
    private String from;

    @NotNull(message = "Edge 'fromType' cannot be null")
    private GraphNodeType fromType;

    @NotBlank(message = "Edge 'to' node cannot be blank")
    private String to;

    @NotNull(message = "Edge 'toType' cannot be null")
    private GraphNodeType toType;

    public GraphEdgeDto() {
    }

    public GraphEdgeDto(String from, GraphNodeType fromType, String to, GraphNodeType toType) {
        this.from = from;
        this.fromType = fromType;
        this.to = to;
        this.toType = toType;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public GraphNodeType getFromType() {
        return fromType;
    }

    public void setFromType(GraphNodeType fromType) {
        this.fromType = fromType;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public GraphNodeType getToType() {
        return toType;
    }

    public void setToType(GraphNodeType toType) {
        this.toType = toType;
    }
}
