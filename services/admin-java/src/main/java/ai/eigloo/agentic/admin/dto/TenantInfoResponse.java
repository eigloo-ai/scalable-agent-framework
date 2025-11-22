package ai.eigloo.agentic.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import ai.eigloo.agentic.admin.entity.TenantInfoEntity;
import java.time.Instant;

/**
 * DTO for tenant info responses.
 */
public class TenantInfoResponse {
    
    private String id;
    
    @JsonProperty("tenant_id")
    private String tenantId;
    
    @JsonProperty("tenant_name")
    private String tenantName;
    
    @JsonProperty("creation_time")
    private Instant creationTime;
    
    @JsonProperty("disable_time")
    private Instant disableTime;
    
    private Boolean enabled;
    
    @JsonProperty("provision_notes")
    private String provisionNotes;
    
    @JsonProperty("disable_notes")
    private String disableNotes;
    
    // Default constructor
    public TenantInfoResponse() {
    }
    
    // Constructor with all fields
    public TenantInfoResponse(String id, String tenantId, String tenantName, Instant creationTime,
                            Instant disableTime, Boolean enabled, String provisionNotes, String disableNotes) {
        this.id = id;
        this.tenantId = tenantId;
        this.tenantName = tenantName;
        this.creationTime = creationTime;
        this.disableTime = disableTime;
        this.enabled = enabled;
        this.provisionNotes = provisionNotes;
        this.disableNotes = disableNotes;
    }
    
    /**
     * Create TenantInfoResponse from TenantInfoEntity.
     * 
     * @param entity the entity to convert
     * @return the response DTO
     */
    public static TenantInfoResponse fromEntity(TenantInfoEntity entity) {
        return new TenantInfoResponse(
            entity.getId(),
            entity.getTenantId(),
            entity.getTenantName(),
            entity.getCreationTime(),
            entity.getDisableTime(),
            entity.getEnabled(),
            entity.getProvisionNotes(),
            entity.getDisableNotes()
        );
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
    
    @Override
    public String toString() {
        return "TenantInfoResponse{" +
                "id='" + id + '\'' +
                ", tenantId='" + tenantId + '\'' +
                ", tenantName='" + tenantName + '\'' +
                ", creationTime=" + creationTime +
                ", disableTime=" + disableTime +
                ", enabled=" + enabled +
                ", provisionNotes='" + provisionNotes + '\'' +
                ", disableNotes='" + disableNotes + '\'' +
                '}';
    }
} 