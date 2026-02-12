package ai.eigloo.agentic.controlplane.service;

import ai.eigloo.agentic.graph.api.GraphLookupResponse;
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
        LinkedHashSet<String> uniqueTaskNames = new LinkedHashSet<>(taskNames);
        List<String> resolvedTaskNames = new ArrayList<>();

        for (String taskName : uniqueTaskNames) {
            Optional<GraphLookupTask> matchingTask = graph.getTasks().stream()
                    .filter(task -> taskName.equals(task.getName()))
                    .filter(task -> {
                        if (upstreamPlanName == null || upstreamPlanName.isBlank()) {
                            return true;
                        }
                        return upstreamPlanName.equals(task.getUpstreamPlanName());
                    })
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
        GraphLookupResponse graph = resolveGraph(tenantId, graphId);
        return graph.getTasks().stream()
                .filter(task -> taskName.equals(task.getName()))
                .map(GraphLookupTask::getDownstreamPlanName)
                .filter(planName -> planName != null && !planName.isBlank())
                .findFirst();
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
