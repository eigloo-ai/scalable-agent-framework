package ai.eigloo.agentic.admin.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA entity representing tenant information in the database.
 * Stores tenant provisioning and lifecycle data.
 */
@Entity
@Table(name = "tenant_info", indexes = {
    @Index(name = "idx_tenant_info_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_tenant_info_creation_time", columnList = "creation_time")
})
public class TenantInfoEntity {
    
    @Id
    @Column(name = "id", length = 36)
    private String id;
    
    @Column(name = "tenant_id", length = 50, nullable = false, unique = true)
    private String tenantId;
    
    @Column(name = "tenant_name", length = 50, nullable = false)
    private String tenantName;
    
    @Column(name = "creation_time", nullable = false, updatable = false)
    private Instant creationTime;
    
    @Column(name = "disable_time")
    private Instant disableTime;
    
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;
    
    @Column(name = "provision_notes", length = 512)
    private String provisionNotes;
    
    @Column(name = "disable_notes", length = 512)
    private String disableNotes;
    
    // Default constructor
    public TenantInfoEntity() {
        this.creationTime = Instant.now();
        this.enabled = true;
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
    
    public String getTenantName() {
        return tenantName;
    }
    
    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }
    
    public Instant getCreationTime() {
        return creationTime;
    }
    
    public void setCreationTime(Instant creationTime) {
        this.creationTime = creationTime;
    }
    
    public Instant getDisableTime() {
        return disableTime;
    }
    
    public void setDisableTime(Instant disableTime) {
        this.disableTime = disableTime;
    }
    
    public Boolean getEnabled() {
        return enabled;
    }
    
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getProvisionNotes() {
        return provisionNotes;
    }
    
    public void setProvisionNotes(String provisionNotes) {
        this.provisionNotes = provisionNotes;
    }
    
    public String getDisableNotes() {
        return disableNotes;
    }
    
    public void setDisableNotes(String disableNotes) {
        this.disableNotes = disableNotes;
    }
    
    @PrePersist
    public void prePersist() {
        if (this.creationTime == null) {
            this.creationTime = Instant.now();
        }
        if (this.enabled == null) {
            this.enabled = true;
        }
    }
    
    @PreUpdate
    public void preUpdate() {
        // No automatic updates needed for this entity
    }
} 