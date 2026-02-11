package ai.eigloo.agentic.dataplane.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

/**
 * JPA entity representing a TaskResult in the database.
 * Stores TaskResult data separately from TaskExecution for foreign key relationships.
 */
@Entity
@Table(name = "task_results", indexes = {
    @Index(name = "idx_task_results_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_task_results_created_at", columnList = "db_created_at")
})
public class TaskResultEntity {
    
    @Id
    @Column(name = "id", length = 36)
    private String id;
    
    @Column(name = "tenant_id", length = 50, nullable = false)
    private String tenantId;
    
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_data", columnDefinition = "jsonb")
    private Map<String, Object> resultData;
    
    // Auto-managed timestamps
    @Column(name = "db_created_at", nullable = false, updatable = false)
    private Instant dbCreatedAt;
    
    @Column(name = "db_updated_at", nullable = false)
    private Instant dbUpdatedAt;
    
    // Default constructor
    public TaskResultEntity() {
        this.dbCreatedAt = Instant.now();
        this.dbUpdatedAt = Instant.now();
    }
    
    // Getters and Setters
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
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public Map<String, Object> getResultData() {
        return resultData;
    }
    
    public void setResultData(Map<String, Object> resultData) {
        this.resultData = resultData;
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
