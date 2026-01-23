package ai.eigloo.agentic.graphbuilder.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entity representing an uploaded graph bundle with processing status and metadata.
 * Tracks the lifecycle of uploaded agent graph bundles from upload to completion.
 */
@Entity
@Table(name = "graph_bundles", indexes = {
    @Index(name = "idx_graph_bundle_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_graph_bundle_status", columnList = "status"),
    @Index(name = "idx_graph_bundle_process_id", columnList = "process_id", unique = true),
    @Index(name = "idx_graph_bundle_upload_time", columnList = "upload_time")
})
public class GraphBundleEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @NotBlank
    @Size(max = 100)
    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Size(max = 255)
    @Column(name = "graph_name", length = 255)
    private String graphName;

    @NotBlank
    @Size(max = 255)
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @NotBlank
    @Size(max = 50)
    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @NotNull
    @CreationTimestamp
    @Column(name = "upload_time", nullable = false)
    private LocalDateTime uploadTime;

    @UpdateTimestamp
    @Column(name = "completion_time")
    private LocalDateTime completionTime;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @NotBlank
    @Size(max = 36)
    @Column(name = "process_id", nullable = false, unique = true, length = 36)
    private String processId;

    // Relationship to ProcessingStepEntity
    @OneToMany(mappedBy = "graphBundle", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProcessingStepEntity> processingSteps;

    // Relationship to DockerImageEntity
    @OneToMany(mappedBy = "graphBundle", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DockerImageEntity> dockerImages;

    // Default constructor for JPA
    public GraphBundleEntity() {}

    // Constructor for creating new entities
    public GraphBundleEntity(String id, String tenantId, String fileName, String status, String processId) {
        this.id = id;
        this.tenantId = tenantId;
        this.fileName = fileName;
        this.status = status;
        this.processId = processId;
        this.uploadTime = LocalDateTime.now(); // Set uploadTime explicitly for tests
    }

    // Getters and setters
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

    public String getGraphName() {
        return graphName;
    }

    public void setGraphName(String graphName) {
        this.graphName = graphName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(LocalDateTime uploadTime) {
        this.uploadTime = uploadTime;
    }

    public LocalDateTime getCompletionTime() {
        return completionTime;
    }

    public void setCompletionTime(LocalDateTime completionTime) {
        this.completionTime = completionTime;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public List<ProcessingStepEntity> getProcessingSteps() {
        return processingSteps;
    }

    public void setProcessingSteps(List<ProcessingStepEntity> processingSteps) {
        this.processingSteps = processingSteps;
    }

    public List<DockerImageEntity> getDockerImages() {
        return dockerImages;
    }

    public void setDockerImages(List<DockerImageEntity> dockerImages) {
        this.dockerImages = dockerImages;
    }

    @Override
    public String toString() {
        return "GraphBundleEntity{" +
                "id='" + id + '\'' +
                ", tenantId='" + tenantId + '\'' +
                ", graphName='" + graphName + '\'' +
                ", fileName='" + fileName + '\'' +
                ", status='" + status + '\'' +
                ", uploadTime=" + uploadTime +
                ", completionTime=" + completionTime +
                ", processId='" + processId + '\'' +
                '}';
    }
}