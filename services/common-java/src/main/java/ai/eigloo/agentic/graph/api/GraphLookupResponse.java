package ai.eigloo.agentic.graph.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Serializable graph shape used by services that must resolve node metadata
 * without direct database access.
 */
public class GraphLookupResponse {

    private String id;
    private String tenantId;
    private String status;
    private List<GraphLookupPlan> plans = new ArrayList<>();
    private List<GraphLookupTask> tasks = new ArrayList<>();
    private List<GraphLookupEdge> edges = new ArrayList<>();

    public GraphLookupResponse() {
    }

    public GraphLookupResponse(
            String id,
            String tenantId,
            String status,
            List<GraphLookupPlan> plans,
            List<GraphLookupTask> tasks) {
        this(id, tenantId, status, plans, tasks, null);
    }

    public GraphLookupResponse(
            String id,
            String tenantId,
            String status,
            List<GraphLookupPlan> plans,
            List<GraphLookupTask> tasks,
            List<GraphLookupEdge> edges) {
        this.id = id;
        this.tenantId = tenantId;
        this.status = status;
        this.plans = plans != null ? plans : new ArrayList<>();
        this.tasks = tasks != null ? tasks : new ArrayList<>();
        this.edges = edges != null ? edges : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<GraphLookupPlan> getPlans() {
        return plans;
    }

    public void setPlans(List<GraphLookupPlan> plans) {
        this.plans = plans != null ? plans : new ArrayList<>();
    }

    public List<GraphLookupTask> getTasks() {
        return tasks;
    }

    public void setTasks(List<GraphLookupTask> tasks) {
        this.tasks = tasks != null ? tasks : new ArrayList<>();
    }

    public List<GraphLookupEdge> getEdges() {
        return edges;
    }

    public void setEdges(List<GraphLookupEdge> edges) {
        this.edges = edges != null ? edges : new ArrayList<>();
    }
}
