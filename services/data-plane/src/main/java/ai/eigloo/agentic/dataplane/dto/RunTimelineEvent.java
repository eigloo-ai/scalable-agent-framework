package ai.eigloo.agentic.dataplane.dto;

import java.time.Instant;
import java.util.List;

/**
 * Timeline event for a plan/task execution persisted for a graph run.
 */
public class RunTimelineEvent {

    private String eventType;
    private String nodeName;
    private String executionId;
    private String status;
    private Instant createdAt;
    private Instant persistedAt;
    private String parentNodeName;
    private String parentExecutionId;
    private List<String> nextTaskNames;
    private String errorMessage;

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getPersistedAt() {
        return persistedAt;
    }

    public void setPersistedAt(Instant persistedAt) {
        this.persistedAt = persistedAt;
    }

    public String getParentNodeName() {
        return parentNodeName;
    }

    public void setParentNodeName(String parentNodeName) {
        this.parentNodeName = parentNodeName;
    }

    public String getParentExecutionId() {
        return parentExecutionId;
    }

    public void setParentExecutionId(String parentExecutionId) {
        this.parentExecutionId = parentExecutionId;
    }

    public List<String> getNextTaskNames() {
        return nextTaskNames;
    }

    public void setNextTaskNames(List<String> nextTaskNames) {
        this.nextTaskNames = nextTaskNames;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
