package ai.eigloo.agentic.dataplane.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

/**
 * JPA entity representing a PlanExecution in the database.
 * Mirrors the Python SQLAlchemy PlanExecution model.
 */
@Entity
@Table(name = "plan_executions", indexes = {
    @Index(name = "idx_plan_executions_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_plan_executions_lifetime_id", columnList = "lifetime_id"),
    @Index(name = "idx_plan_executions_graph_id", columnList = "graph_id"),
    @Index(name = "idx_plan_executions_status", columnList = "status"),
    @Index(name = "idx_plan_executions_created_at", columnList = "created_at"),
    @Index(name = "idx_plan_executions_parent_task_exec_ids", columnList = "parent_task_exec_ids"),
    @Index(name = "idx_plan_executions_parent_task_names", columnList = "parent_task_names")
})
public class PlanExecutionEntity {
    
    @Id
    @Column(name = "exec_id", length = 36)
    private String execId;
    
    @Column(name = "name", length = 100, nullable = false)
    private String name;
    
    @Column(name = "graph_id", length = 36, nullable = false)
    private String graphId;
    
    @Column(name = "lifetime_id", length = 36, nullable = false)
    private String lifetimeId;
    
    @Column(name = "tenant_id", length = 50, nullable = false)
    private String tenantId;
    
    @Column(name = "attempt", nullable = false)
    private Integer attempt;
    
    @Column(name = "iteration_idx", nullable = false)
    private Integer iterationIdx;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, nullable = false)
    private ExecutionStatus status;
    
    @Column(name = "edge_taken", length = 100)
    private String edgeTaken;
    
    // Plan-specific fields
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parent_task_exec_ids", columnDefinition = "jsonb")
    private List<String> parentTaskExecIds;
    
    @Column(name = "parent_task_names", length = 500)
    private String parentTaskNames;
    
    // Result fields
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_next_task_names", columnDefinition = "jsonb")
    private List<String> resultNextTaskNames;
    
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    
    // Foreign key list to TaskResult objects
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "upstream_task_result_ids", columnDefinition = "jsonb")
    private List<String> upstreamTaskResultIds;
    
    // Auto-managed timestamps
    @Column(name = "db_created_at", nullable = false, updatable = false)
    private Instant dbCreatedAt;
    
    @Column(name = "db_updated_at", nullable = false)
    private Instant dbUpdatedAt;
    
    public enum ExecutionStatus {
        EXECUTION_STATUS_UNSPECIFIED,
        EXECUTION_STATUS_PENDING,
        EXECUTION_STATUS_RUNNING,
        EXECUTION_STATUS_SUCCEEDED,
        EXECUTION_STATUS_FAILED
    }
    
    // Default constructor
    public PlanExecutionEntity() {
        this.dbCreatedAt = Instant.now();
        this.dbUpdatedAt = Instant.now();
    }
    
    // Getters and Setters
    public String getExecId() {
        return execId;
    }
    
    public void setExecId(String execId) {
        this.execId = execId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
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
    
    public String getTenantId() {
        return tenantId;
    }
    
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
    
    public Integer getAttempt() {
        return attempt;
    }
    
    public void setAttempt(Integer attempt) {
        this.attempt = attempt;
    }
    
    public Integer getIterationIdx() {
        return iterationIdx;
    }
    
    public void setIterationIdx(Integer iterationIdx) {
        this.iterationIdx = iterationIdx;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public ExecutionStatus getStatus() {
        return status;
    }
    
    public void setStatus(ExecutionStatus status) {
        this.status = status;
    }
    
    public String getEdgeTaken() {
        return edgeTaken;
    }
    
    public void setEdgeTaken(String edgeTaken) {
        this.edgeTaken = edgeTaken;
    }
    
    public List<String> getParentTaskExecIds() {
        return parentTaskExecIds;
    }
    
    public void setParentTaskExecIds(List<String> parentTaskExecIds) {
        this.parentTaskExecIds = parentTaskExecIds;
    }
    
    public String getParentTaskNames() {
        return parentTaskNames;
    }
    
    public void setParentTaskNames(String parentTaskNames) {
        this.parentTaskNames = parentTaskNames;
    }
    
    public List<String> getResultNextTaskNames() {
        return resultNextTaskNames;
    }
    
    public void setResultNextTaskNames(List<String> resultNextTaskNames) {
        this.resultNextTaskNames = resultNextTaskNames;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public List<String> getUpstreamTaskResultIds() {
        return upstreamTaskResultIds;
    }
    
    public void setUpstreamTaskResultIds(List<String> upstreamTaskResultIds) {
        this.upstreamTaskResultIds = upstreamTaskResultIds;
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
