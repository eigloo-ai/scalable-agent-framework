package ai.eigloo.agentic.graphbuilder.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing Docker images built for tasks and plans during graph bundle processing.
 * Tracks the Docker images created for each executor component in the agent graph.
 */
@Entity
@Table(name = "docker_images", indexes = {
    @Index(name = "idx_docker_image_process_id", columnList = "process_id"),
    @Index(name = "idx_docker_image_executor_type", columnList = "executor_type"),
    @Index(name = "idx_docker_image_executor_name", columnList = "executor_name"),
    @Index(name = "idx_docker_image_build_time", columnList = "build_time")
})
public class DockerImageEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @NotBlank
    @Size(max = 36)
    @Column(name = "process_id", nullable = false, length = 36)
    private String processId;

    @NotBlank
    @Size(max = 255)
    @Column(name = "image_name", nullable = false, length = 255)
    private String imageName;

    @Size(max = 100)
    @Column(name = "image_tag", length = 100)
    private String imageTag;

    @NotBlank
    @Size(max = 20)
    @Column(name = "executor_type", nullable = false, length = 20)
    private String executorType; // TASK or PLAN

    @NotBlank
    @Size(max = 100)
    @Column(name = "executor_name", nullable = false, length = 100)
    private String executorName;

    @NotNull
    @CreationTimestamp
    @Column(name = "build_time", nullable = false)
    private LocalDateTime buildTime;

    // Relationship to GraphBundleEntity
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "process_id", referencedColumnName = "process_id", insertable = false, updatable = false)
    private GraphBundleEntity graphBundle;

    // Default constructor for JPA
    public DockerImageEntity() {}

    // Constructor for creating new Docker image records
    public DockerImageEntity(String id, String processId, String imageName, String executorType, String executorName) {
        this.id = id;
        this.processId = processId;
        this.imageName = imageName;
        this.executorType = executorType;
        this.executorName = executorName;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getImageTag() {
        return imageTag;
    }

    public void setImageTag(String imageTag) {
        this.imageTag = imageTag;
    }

    public String getExecutorType() {
        return executorType;
    }

    public void setExecutorType(String executorType) {
        this.executorType = executorType;
    }

    public String getExecutorName() {
        return executorName;
    }

    public void setExecutorName(String executorName) {
        this.executorName = executorName;
    }

    public LocalDateTime getBuildTime() {
        return buildTime;
    }

    public void setBuildTime(LocalDateTime buildTime) {
        this.buildTime = buildTime;
    }

    public GraphBundleEntity getGraphBundle() {
        return graphBundle;
    }

    public void setGraphBundle(GraphBundleEntity graphBundle) {
        this.graphBundle = graphBundle;
    }

    @Override
    public String toString() {
        return "DockerImageEntity{" +
                "id='" + id + '\'' +
                ", processId='" + processId + '\'' +
                ", imageName='" + imageName + '\'' +
                ", imageTag='" + imageTag + '\'' +
                ", executorType='" + executorType + '\'' +
                ", executorName='" + executorName + '\'' +
                ", buildTime=" + buildTime +
                '}';
    }
}