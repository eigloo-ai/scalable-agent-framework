package ai.eigloo.agentic.graph.api;

/**
 * Serializable run state projection for control-plane routing checks.
 */
public class GraphRunStateResponse {

    private String tenantId;
    private String graphId;
    private String lifetimeId;
    private String status;

    public GraphRunStateResponse() {
    }

    public GraphRunStateResponse(String tenantId, String graphId, String lifetimeId, String status) {
        this.tenantId = tenantId;
        this.graphId = graphId;
        this.lifetimeId = lifetimeId;
        this.status = status;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getGraphId() {
        return graphId;
    }

    public void setGraphId(String graphId) {
        this.graphId = graphId;
    }

    public String getLifetimeId() {
        return lifetimeId;
    }

    public void setLifetimeId(String lifetimeId) {
        this.lifetimeId = lifetimeId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
