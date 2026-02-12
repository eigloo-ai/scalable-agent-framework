package ai.eigloo.agentic.dataplane.service;

import ai.eigloo.agentic.graph.api.GraphLookupFile;
import ai.eigloo.agentic.graph.api.GraphLookupEdge;
import ai.eigloo.agentic.graph.api.GraphLookupNodeType;
import ai.eigloo.agentic.graph.api.GraphLookupPlan;
import ai.eigloo.agentic.graph.api.GraphLookupResponse;
import ai.eigloo.agentic.graph.api.GraphLookupTask;
import ai.eigloo.agentic.graph.api.GraphRunStateResponse;
import ai.eigloo.agentic.graph.entity.AgentGraphEntity;
import ai.eigloo.agentic.graph.entity.ExecutorFileEntity;
import ai.eigloo.agentic.graph.entity.GraphEdgeEntity;
import ai.eigloo.agentic.graph.entity.GraphRunEntity;
import ai.eigloo.agentic.graph.entity.PlanEntity;
import ai.eigloo.agentic.graph.entity.TaskEntity;
import ai.eigloo.agentic.graph.repository.AgentGraphRepository;
import ai.eigloo.agentic.graph.repository.GraphRunRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Internal data-plane read model for graph topology and run state lookups.
 */
@Service
@Transactional(readOnly = true)
public class InternalGraphQueryService {

    private final AgentGraphRepository agentGraphRepository;
    private final GraphRunRepository graphRunRepository;

    public InternalGraphQueryService(
            AgentGraphRepository agentGraphRepository,
            GraphRunRepository graphRunRepository) {
        this.agentGraphRepository = agentGraphRepository;
        this.graphRunRepository = graphRunRepository;
    }

    public GraphLookupResponse getGraphLookup(String tenantId, String graphId) {
        AgentGraphEntity graph = agentGraphRepository.findByIdAndTenantIdWithAllRelations(graphId, tenantId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Graph not found for tenant=" + tenantId + " graph_id=" + graphId));

        List<GraphLookupPlan> plans = graph.getPlans().stream()
                .sorted(Comparator.comparing(PlanEntity::getName, Comparator.nullsLast(String::compareTo)))
                .map(this::toPlanLookup)
                .collect(Collectors.toList());

        List<GraphLookupEdge> edges = graph.getEdges().stream()
                .map(this::toEdgeLookup)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();

        List<GraphLookupTask> tasks = graph.getTasks().stream()
                .sorted(Comparator.comparing(TaskEntity::getName, Comparator.nullsLast(String::compareTo)))
                .map(this::toTaskLookup)
                .collect(Collectors.toList());

        return new GraphLookupResponse(
                graph.getId(),
                graph.getTenantId(),
                graph.getStatus() != null ? graph.getStatus().name() : null,
                plans,
                tasks,
                edges);
    }

    public GraphRunStateResponse getRunState(String tenantId, String graphId, String lifetimeId) {
        GraphRunEntity run = graphRunRepository.findByLifetimeIdAndTenantId(lifetimeId, tenantId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Graph run not found for tenant=" + tenantId + " lifetime_id=" + lifetimeId));

        if (!graphId.equals(run.getGraphId())) {
            throw new IllegalArgumentException("graphId does not match run lifetime_id");
        }

        return new GraphRunStateResponse(
                run.getTenantId(),
                run.getGraphId(),
                run.getLifetimeId(),
                run.getStatus() != null ? run.getStatus().name() : null);
    }

    private GraphLookupPlan toPlanLookup(PlanEntity plan) {
        return new GraphLookupPlan(plan.getName(), toFileLookup(plan.getFiles()));
    }

    private GraphLookupTask toTaskLookup(TaskEntity task) {
        return new GraphLookupTask(task.getName(), toFileLookup(task.getFiles()));
    }

    private GraphLookupEdge toEdgeLookup(GraphEdgeEntity edge) {
        return new GraphLookupEdge(
                edge.getFromNodeName(),
                toLookupType(edge.getFromNodeType()),
                edge.getToNodeName(),
                toLookupType(edge.getToNodeType()));
    }

    private static GraphLookupNodeType toLookupType(ai.eigloo.agentic.graph.model.GraphNodeType type) {
        return switch (type) {
            case PLAN -> GraphLookupNodeType.PLAN;
            case TASK -> GraphLookupNodeType.TASK;
        };
    }

    private List<GraphLookupFile> toFileLookup(List<ExecutorFileEntity> files) {
        if (files == null) {
            return List.of();
        }
        return files.stream()
                .sorted(Comparator.comparing(ExecutorFileEntity::getName, Comparator.nullsLast(String::compareTo)))
                .map(file -> new GraphLookupFile(file.getName(), file.getContents()))
                .collect(Collectors.toList());
    }
}
