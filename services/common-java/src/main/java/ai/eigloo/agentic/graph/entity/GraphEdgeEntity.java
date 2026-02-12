package ai.eigloo.agentic.graph.entity;

import ai.eigloo.agentic.graph.model.GraphNodeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Canonical directed edge persisted for an agent graph.
 */
@Entity
@Table(name = "graph_edges", indexes = {
        @Index(name = "idx_graph_edges_graph_id", columnList = "graph_id"),
        @Index(name = "idx_graph_edges_from", columnList = "graph_id, from_node_type, from_node_name"),
        @Index(name = "idx_graph_edges_to", columnList = "graph_id, to_node_type, to_node_name")
}, uniqueConstraints = {
        @UniqueConstraint(
                name = "uq_graph_edges_unique",
                columnNames = {"graph_id", "from_node_name", "from_node_type", "to_node_name", "to_node_type"})
})
public class GraphEdgeEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graph_id", nullable = false)
    private AgentGraphEntity agentGraph;

    @NotBlank
    @Size(max = 255)
    @Column(name = "from_node_name", nullable = false, length = 255)
    private String fromNodeName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "from_node_type", nullable = false, length = 20)
    private GraphNodeType fromNodeType;

    @NotBlank
    @Size(max = 255)
    @Column(name = "to_node_name", nullable = false, length = 255)
    private String toNodeName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "to_node_type", nullable = false, length = 20)
    private GraphNodeType toNodeType;

    public GraphEdgeEntity() {
    }

    public GraphEdgeEntity(
            String id,
            AgentGraphEntity agentGraph,
            String fromNodeName,
            GraphNodeType fromNodeType,
            String toNodeName,
            GraphNodeType toNodeType) {
        this.id = id;
        this.agentGraph = agentGraph;
        this.fromNodeName = fromNodeName;
        this.fromNodeType = fromNodeType;
        this.toNodeName = toNodeName;
        this.toNodeType = toNodeType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public AgentGraphEntity getAgentGraph() {
        return agentGraph;
    }

    public void setAgentGraph(AgentGraphEntity agentGraph) {
        this.agentGraph = agentGraph;
    }

    public String getFromNodeName() {
        return fromNodeName;
    }

    public void setFromNodeName(String fromNodeName) {
        this.fromNodeName = fromNodeName;
    }

    public GraphNodeType getFromNodeType() {
        return fromNodeType;
    }

    public void setFromNodeType(GraphNodeType fromNodeType) {
        this.fromNodeType = fromNodeType;
    }

    public String getToNodeName() {
        return toNodeName;
    }

    public void setToNodeName(String toNodeName) {
        this.toNodeName = toNodeName;
    }

    public GraphNodeType getToNodeType() {
        return toNodeType;
    }

    public void setToNodeType(GraphNodeType toNodeType) {
        this.toNodeType = toNodeType;
    }

    @Override
    public String toString() {
        return "GraphEdgeEntity{" +
                "id='" + id + '\'' +
                ", fromNodeName='" + fromNodeName + '\'' +
                ", fromNodeType=" + fromNodeType +
                ", toNodeName='" + toNodeName + '\'' +
                ", toNodeType=" + toNodeType +
                '}';
    }
}

