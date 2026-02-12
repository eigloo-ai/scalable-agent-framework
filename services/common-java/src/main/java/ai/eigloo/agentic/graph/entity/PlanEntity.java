package ai.eigloo.agentic.graph.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a persisted plan within an agent graph.
 */
@Entity
@Table(name = "plans", indexes = {
    @Index(name = "idx_plan_graph_id", columnList = "graph_id"),
    @Index(name = "idx_plan_name", columnList = "name"),
    @Index(name = "idx_plan_graph_name", columnList = "graph_id, name")
})
public class PlanEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @NotBlank
    @Size(max = 255)
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Size(max = 255)
    @Column(name = "label", length = 255)
    private String label;

    @Size(max = 500)
    @Column(name = "plan_source_path", length = 500)
    private String planSourcePath;

    // Relationship to AgentGraphEntity
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graph_id", nullable = false)
    private AgentGraphEntity agentGraph;

    // Relationship to ExecutorFiles
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    private List<ExecutorFileEntity> files = new ArrayList<>();

    // Default constructor for JPA
    public PlanEntity() {}

    // Constructor for creating new entities
    public PlanEntity(String id, String name, String label, String planSourcePath, AgentGraphEntity agentGraph) {
        this.id = id;
        this.name = name;
        this.label = label;
        this.planSourcePath = planSourcePath;
        this.agentGraph = agentGraph;
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

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getPlanSourcePath() {
        return planSourcePath;
    }

    public void setPlanSourcePath(String planSourcePath) {
        this.planSourcePath = planSourcePath;
    }

    public AgentGraphEntity getAgentGraph() {
        return agentGraph;
    }

    public void setAgentGraph(AgentGraphEntity agentGraph) {
        this.agentGraph = agentGraph;
    }

    public List<ExecutorFileEntity> getFiles() {
        return files;
    }

    public void setFiles(List<ExecutorFileEntity> files) {
        this.files = files != null ? files : new ArrayList<>();
    }

    // Helper methods for managing files
    public void addFile(ExecutorFileEntity file) {
        if (file != null) {
            files.add(file);
            file.setPlan(this);
        }
    }

    public void removeFile(ExecutorFileEntity file) {
        if (file != null) {
            files.remove(file);
            file.setPlan(null);
        }
    }

    @Override
    public String toString() {
        return "PlanEntity{" +
               "id='" + id + '\'' +
               ", name='" + name + '\'' +
               ", label='" + label + '\'' +
               ", planSourcePath='" + planSourcePath + '\'' +
               ", filesCount=" + (files != null ? files.size() : 0) +
               '}';
    }
}
