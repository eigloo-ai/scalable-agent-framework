package ai.eigloo.agentic.graphbuilder.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing individual processing steps within a graph bundle processing workflow.
 * Tracks the progress and status of each step in the bundle processing pipeline.
 */
@Entity
@Table(name = "processing_steps", indexes = {
    @Index(name = "idx_processing_step_process_id", columnList = "process_id"),
    @Index(name = "idx_processing_step_status", columnList = "status"),
    @Index(name = "idx_processing_step_start_time", columnList = "start_time")
})
public class ProcessingStepEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @NotBlank
    @Size(max = 36)
    @Column(name = "process_id", nullable = false, length = 36)
    private String processId;

    @NotBlank
    @Size(max = 100)
    @Column(name = "step_name", nullable = false, length = 100)
    private String stepName;

    @NotBlank
    @Size(max = 50)
    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @NotNull
    @CreationTimestamp
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // Relationship to GraphBundleEntity
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "process_id", referencedColumnName = "process_id", insertable = false, updatable = false)
    private GraphBundleEntity graphBundle;

    // Default constructor for JPA
    public ProcessingStepEntity() {}

    // Constructor for creating new processing steps
    public ProcessingStepEntity(String id, String processId, String stepName, String status) {
        this.id = id;
        this.processId = processId;
        this.stepName = stepName;
        this.status = status;
        this.startTime = LocalDateTime.now(); // Set startTime explicitly for tests
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

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public GraphBundleEntity getGraphBundle() {
        return graphBundle;
    }

    public void setGraphBundle(GraphBundleEntity graphBundle) {
        this.graphBundle = graphBundle;
    }

    @Override
    public String toString() {
        return "ProcessingStepEntity{" +
                "id='" + id + '\'' +
                ", processId='" + processId + '\'' +
                ", stepName='" + stepName + '\'' +
                ", status='" + status + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                '}';
    }
}