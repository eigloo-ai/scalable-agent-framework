package ai.eigloo.agentic.dataplane.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * API response containing graph run status and execution timeline.
 */
public class RunTimelineResponse {

    private String tenantId;
    private String graphId;
    private String lifetimeId;
    private String status;
    private List<String> entryPlanNames = new ArrayList<>();
    private String errorMessage;
    private Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;
    private int planExecutions;
    private int taskExecutions;
    private List<RunTimelineEvent> events = new ArrayList<>();

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

    public int getPlanExecutions() {
        return planExecutions;
    }

    public void setPlanExecutions(int planExecutions) {
        this.planExecutions = planExecutions;
    }

    public int getTaskExecutions() {
        return taskExecutions;
    }

    public void setTaskExecutions(int taskExecutions) {
        this.taskExecutions = taskExecutions;
    }

    public List<RunTimelineEvent> getEvents() {
        return events;
    }

    public void setEvents(List<RunTimelineEvent> events) {
        this.events = events;
    }
}
