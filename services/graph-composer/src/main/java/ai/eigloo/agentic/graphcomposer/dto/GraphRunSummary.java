package ai.eigloo.agentic.graphcomposer.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Summary projection for tenant graph execution runs.
 */
public class GraphRunSummary {

    private String tenantId;
    private String graphId;
    private String lifetimeId;
    private String status;
    private List<String> entryPlanNames = new ArrayList<>();
    private String errorMessage;
    private Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;

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

    public List<String> getEntryPlanNames() {
        return entryPlanNames;
    }

    public void setEntryPlanNames(List<String> entryPlanNames) {
        this.entryPlanNames = entryPlanNames;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
