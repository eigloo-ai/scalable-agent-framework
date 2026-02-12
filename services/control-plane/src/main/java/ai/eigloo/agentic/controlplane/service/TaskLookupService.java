package ai.eigloo.agentic.controlplane.service;

import ai.eigloo.agentic.graph.api.GraphLookupResponse;
import ai.eigloo.agentic.graph.api.GraphLookupEdge;
import ai.eigloo.agentic.graph.api.GraphLookupNodeType;
import ai.eigloo.agentic.graph.api.GraphLookupTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/**
 * Service for graph-backed plan/task relationship lookups.
 */
@Service
public class TaskLookupService {

    private static final Logger logger = LoggerFactory.getLogger(TaskLookupService.class);

    private final DataPlaneGraphClient dataPlaneGraphClient;

    public TaskLookupService(DataPlaneGraphClient dataPlaneGraphClient) {
        this.dataPlaneGraphClient = dataPlaneGraphClient;
    }

    /**
     * Resolve task names from plan output against the graph model.
     */
    public List<String> lookupExecutableTaskNames(
            List<String> taskNames,
            String tenantId,
            String graphId,
            String upstreamPlanName) {
        GraphLookupResponse graph = resolveGraph(tenantId, graphId);
        List<GraphLookupEdge> edges = graph.getEdges() != null ? graph.getEdges() : List.of();
        LinkedHashSet<String> uniqueTaskNames = new LinkedHashSet<>(taskNames);
        List<String> resolvedTaskNames = new ArrayList<>();

        if (!edges.isEmpty()) {
            LinkedHashSet<String> executableTaskNames = new LinkedHashSet<>();
            for (GraphLookupEdge edge : edges) {
                if (edge.getFromType() != GraphLookupNodeType.PLAN || edge.getToType() != GraphLookupNodeType.TASK) {
                    continue;
                }
                if (upstreamPlanName == null || upstreamPlanName.isBlank() || upstreamPlanName.equals(edge.getFrom())) {
                    executableTaskNames.add(edge.getTo());
                }
            }

            for (String taskName : uniqueTaskNames) {
                if (executableTaskNames.contains(taskName)) {
                    resolvedTaskNames.add(taskName);
                } else {
                    logger.warn(
                            "Task '{}' is not executable in graph {} for upstream plan '{}'",
                            taskName, graph.getId(), upstreamPlanName);
                }
            }
            return resolvedTaskNames;
        }

        for (String taskName : uniqueTaskNames) {
            Optional<GraphLookupTask> matchingTask = graph.getTasks().stream()
                    .filter(task -> taskName.equals(task.getName()))
                    .filter(task -> upstreamPlanName == null
                            || upstreamPlanName.isBlank()
                            || upstreamPlanName.equals(task.getUpstreamPlanName()))
                    .findFirst();

            if (matchingTask.isPresent()) {
                resolvedTaskNames.add(taskName);
            } else {
                logger.warn(
                        "Task '{}' is not executable in graph {} for upstream plan '{}'",
                        taskName, graph.getId(), upstreamPlanName);
            }
        }

        return resolvedTaskNames;
    }

    /**
     * Resolve downstream plan name for a completed task.
     */
    public Optional<String> lookupDownstreamPlanName(String taskName, String tenantId, String graphId) {
        return lookupDownstreamPlanNames(taskName, tenantId, graphId).stream().findFirst();
    }

    /**
     * Resolve downstream plan names for a completed task.
     */
    public List<String> lookupDownstreamPlanNames(String taskName, String tenantId, String graphId) {
        GraphLookupResponse graph = resolveGraph(tenantId, graphId);
        List<GraphLookupEdge> edges = graph.getEdges() != null ? graph.getEdges() : List.of();
        if (!edges.isEmpty()) {
            LinkedHashSet<String> plans = new LinkedHashSet<>();
            for (GraphLookupEdge edge : edges) {
                if (edge.getFromType() == GraphLookupNodeType.TASK
                        && edge.getToType() == GraphLookupNodeType.PLAN
                        && taskName.equals(edge.getFrom())
                        && edge.getTo() != null
                        && !edge.getTo().isBlank()) {
                    plans.add(edge.getTo());
                }
            }
            return new ArrayList<>(plans);
        }

        return graph.getTasks().stream()
                .filter(task -> taskName.equals(task.getName()))
                .map(GraphLookupTask::getDownstreamPlanName)
                .filter(planName -> planName != null && !planName.isBlank())
                .distinct()
                .toList();
    }

    private GraphLookupResponse resolveGraph(String tenantId, String graphId) {
        if (graphId == null || graphId.isBlank()) {
            throw new IllegalArgumentException("graph_id is required for task/plan lookup");
        }

        return dataPlaneGraphClient.getGraph(tenantId, graphId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Graph '" + graphId + "' not found for tenant " + tenantId));
    }
}
