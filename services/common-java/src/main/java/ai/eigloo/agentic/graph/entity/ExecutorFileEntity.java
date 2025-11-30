package ai.eigloo.agentic.graph.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * JPA entity representing a file associated with a plan or task in the agent graph.
 * 
 * <p>ExecutorFileEntity stores the content and metadata for files that are part of
 * plan or task implementations, such as Python source files and requirements.txt files.
 * Each file tracks its creation and update timestamps along with version information.</p>
 */
@Entity
@Table(name = "executor_files", indexes = {
    @Index(name = "idx_executor_file_plan_id", columnList = "plan_id"),
    @Index(name = "idx_executor_file_task_id", columnList = "task_id"),
    @Index(name = "idx_executor_file_name", columnList = "file_name")
})
public class ExecutorFileEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @NotBlank
    @Size(max = 255)
    @Column(name = "file_name", nullable = false, length = 255)
    private String name;

    @Lob
    @Column(name = "contents")
    private String contents;

    @CreationTimestamp
    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDateTime creationDate;

    @Size(max = 50)
    @Column(name = "version", length = 50)
    private String version;

    @UpdateTimestamp
    @Column(name = "update_date", nullable = false)
    private LocalDateTime updateDate;

    // Relationships - one file can belong to either a plan or a task, but not both
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private PlanEntity plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private TaskEntity task;

    // Default constructor for JPA
    public ExecutorFileEntity() {}

    // Constructor for creating new entities
    public ExecutorFileEntity(String id, String name, String contents, String version) {
        this.id = id;
        this.name = name;
        this.contents = contents;
        this.version = version;
    }

    // Constructor with plan relationship
    public ExecutorFileEntity(String id, String name, String contents, String version, PlanEntity plan) {
        this(id, name, contents, version);
        this.plan = plan;
    }

    // Constructor with task relationship
    public ExecutorFileEntity(String id, String name, String contents, String version, TaskEntity task) {
        this(id, name, contents, version);
        this.task = task;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContents() {
        return contents;
    }

    public void setContents(String contents) {
        this.contents = contents;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public LocalDateTime getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(LocalDateTime updateDate) {
        this.updateDate = updateDate;
    }

    public PlanEntity getPlan() {
        return plan;
    }

    public void setPlan(PlanEntity plan) {
        this.plan = plan;
    }

    public TaskEntity getTask() {
        return task;
    }

    public void setTask(TaskEntity task) {
        this.task = task;
    }

    @Override
    public String toString() {
        return "ExecutorFileEntity{" +
               "id='" + id + '\'' +
               ", name='" + name + '\'' +
               ", version='" + version + '\'' +
               ", creationDate=" + creationDate +
               ", updateDate=" + updateDate +
               ", contentLength=" + (contents != null ? contents.length() : 0) +
               '}';
    }
}