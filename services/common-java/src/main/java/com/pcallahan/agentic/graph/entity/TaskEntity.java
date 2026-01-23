package com.pcallahan.agentic.graph.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a persisted task within an agent graph.
 */
@Entity
@Table(name = "tasks", indexes = {
    @Index(name = "idx_task_graph_id", columnList = "graph_id"),
    @Index(name = "idx_task_name", columnList = "name"),
    @Index(name = "idx_task_graph_name", columnList = "graph_id, name"),
    @Index(name = "idx_task_upstream_plan", columnList = "upstream_plan_id")
})
public class TaskEntity {

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
    @Column(name = "task_source", length = 500)
    private String taskSource;

    // Relationship to AgentGraphEntity
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graph_id", nullable = false)
    private AgentGraphEntity agentGraph;

    // Relationship to upstream plan
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "upstream_plan_id")
    private PlanEntity upstreamPlan;

    // Relationship to downstream plan (Task → Plan)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "downstream_plan_id")
    private PlanEntity downstreamPlan;

    // Relationship to ExecutorFiles
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    private List<ExecutorFileEntity> files = new ArrayList<>();

    // Default constructor for JPA
    public TaskEntity() {}

    // Constructor for creating new entities
    public TaskEntity(String id, String name, String label, String taskSource, 
                     AgentGraphEntity agentGraph, PlanEntity upstreamPlan) {
        this.id = id;
        this.name = name;
        this.label = label;
        this.taskSource = taskSource;
        this.agentGraph = agentGraph;
        this.upstreamPlan = upstreamPlan;
        this.downstreamPlan = null; // Will be set via relationships
    }

    // Full constructor for creating new entities with both plans
    public TaskEntity(String id, String name, String label, String taskSource, 
                     AgentGraphEntity agentGraph, PlanEntity upstreamPlan, PlanEntity downstreamPlan) {
        this.id = id;
        this.name = name;
        this.label = label;
        this.taskSource = taskSource;
        this.agentGraph = agentGraph;
        this.upstreamPlan = upstreamPlan;
        this.downstreamPlan = downstreamPlan;
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

    public String getTaskSourcePath() {
        return taskSource;
    }

    public void setTaskSourcePath(String taskSource) {
        this.taskSource = taskSource;
    }

    public AgentGraphEntity getAgentGraph() {
        return agentGraph;
    }

    public void setAgentGraph(AgentGraphEntity agentGraph) {
        this.agentGraph = agentGraph;
    }

    public PlanEntity getUpstreamPlan() {
        return upstreamPlan;
    }

    public void setUpstreamPlan(PlanEntity upstreamPlan) {
        this.upstreamPlan = upstreamPlan;
    }

    public PlanEntity getDownstreamPlan() {
        return downstreamPlan;
    }

    public void setDownstreamPlan(PlanEntity downstreamPlan) {
        this.downstreamPlan = downstreamPlan;
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
            file.setTask(this);
        }
    }

    public void removeFile(ExecutorFileEntity file) {
        if (file != null) {
            files.remove(file);
            file.setTask(null);
        }
    }

    @Override
    public String toString() {
        return "TaskEntity{" +
               "id='" + id + '\'' +
               ", name='" + name + '\'' +
               ", label='" + label + '\'' +
               ", taskSource='" + taskSource + '\'' +
               ", filesCount=" + (files != null ? files.size() : 0) +
               '}';
    }
}