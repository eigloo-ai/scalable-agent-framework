package ai.eigloo.agentic.graph.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * First-class runtime execution record for a graph lifetime.
 */
@Entity
@Table(name = "graph_runs", indexes = {
        @Index(name = "idx_graph_runs_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_graph_runs_graph_id", columnList = "graph_id"),
        @Index(name = "idx_graph_runs_status", columnList = "status"),
        @Index(name = "idx_graph_runs_created_at", columnList = "created_at"),
        @Index(name = "idx_graph_runs_completed_at", columnList = "completed_at")
})
public class GraphRunEntity {

    @Id
    @Column(name = "lifetime_id", length = 36, nullable = false)
    private String lifetimeId;

    @Column(name = "tenant_id", length = 50, nullable = false)
    private String tenantId;

    @Column(name = "graph_id", length = 36, nullable = false)
    private String graphId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private GraphRunStatus status;

    @Column(name = "entry_plan_names", length = 1000)
    private String entryPlanNames;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "db_created_at", nullable = false, updatable = false)
    private Instant dbCreatedAt;

    @Column(name = "db_updated_at", nullable = false)
    private Instant dbUpdatedAt;

    public GraphRunEntity() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.dbCreatedAt = now;
        this.dbUpdatedAt = now;
    }

    public String getLifetimeId() {
        return lifetimeId;
    }

    public void setLifetimeId(String lifetimeId) {
        this.lifetimeId = lifetimeId;
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

    public GraphRunStatus getStatus() {
        return status;
    }

    public void setStatus(GraphRunStatus status) {
        this.status = status;
    }

    public String getEntryPlanNames() {
        return entryPlanNames;
    }

    public void setEntryPlanNames(String entryPlanNames) {
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

    public Instant getDbCreatedAt() {
        return dbCreatedAt;
    }

    public void setDbCreatedAt(Instant dbCreatedAt) {
        this.dbCreatedAt = dbCreatedAt;
    }

    public Instant getDbUpdatedAt() {
        return dbUpdatedAt;
    }

    public void setDbUpdatedAt(Instant dbUpdatedAt) {
        this.dbUpdatedAt = dbUpdatedAt;
    }

    @PreUpdate
    public void preUpdate() {
        this.dbUpdatedAt = Instant.now();
    }
}
