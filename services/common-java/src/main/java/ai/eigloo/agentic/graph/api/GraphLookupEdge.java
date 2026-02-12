package ai.eigloo.agentic.graph.api;

/**
 * Directed edge for graph lookup payloads.
 */
public class GraphLookupEdge {

    private String from;
    private GraphLookupNodeType fromType;
    private String to;
    private GraphLookupNodeType toType;

    public GraphLookupEdge() {
    }

    public GraphLookupEdge(
            String from,
            GraphLookupNodeType fromType,
            String to,
            GraphLookupNodeType toType) {
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

    public GraphLookupNodeType getFromType() {
        return fromType;
    }

    public void setFromType(GraphLookupNodeType fromType) {
        this.fromType = fromType;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public GraphLookupNodeType getToType() {
        return toType;
    }

    public void setToType(GraphLookupNodeType toType) {
        this.toType = toType;
    }
}
