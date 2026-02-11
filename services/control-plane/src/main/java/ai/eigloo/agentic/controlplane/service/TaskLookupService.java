package ai.eigloo.agentic.controlplane.service;

import ai.eigloo.agentic.graph.entity.AgentGraphEntity;
import ai.eigloo.agentic.graph.entity.TaskEntity;
import ai.eigloo.agentic.graph.repository.AgentGraphRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/**
 * Service for graph-backed plan/task relationship lookups.
 */
@Service
@Transactional(readOnly = true)
public class TaskLookupService {

    private static final Logger logger = LoggerFactory.getLogger(TaskLookupService.class);

    private final AgentGraphRepository agentGraphRepository;

    public TaskLookupService(AgentGraphRepository agentGraphRepository) {
        this.agentGraphRepository = agentGraphRepository;
    }

    /**
     * Resolve task names from plan output against the graph model.
     */
    public List<String> lookupExecutableTaskNames(
            List<String> taskNames,
            String tenantId,
            String graphId,
            String upstreamPlanName) {
        AgentGraphEntity graph = resolveGraph(tenantId, graphId);
        LinkedHashSet<String> uniqueTaskNames = new LinkedHashSet<>(taskNames);
        List<String> resolvedTaskNames = new ArrayList<>();

        for (String taskName : uniqueTaskNames) {
            Optional<TaskEntity> matchingTask = graph.getTasks().stream()
                    .filter(task -> taskName.equals(task.getName()))
                    .filter(task -> {
                        if (upstreamPlanName == null || upstreamPlanName.isBlank()) {
                            return true;
                        }
                        return task.getUpstreamPlan() != null
                                && upstreamPlanName.equals(task.getUpstreamPlan().getName());
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
        AgentGraphEntity graph = resolveGraph(tenantId, graphId);
        return graph.getTasks().stream()
                .filter(task -> taskName.equals(task.getName()))
                .map(TaskEntity::getDownstreamPlan)
                .filter(plan -> plan != null && plan.getName() != null && !plan.getName().isBlank())
                .map(plan -> plan.getName())
                .findFirst();
    }

    private AgentGraphEntity resolveGraph(String tenantId, String graphId) {
        if (graphId == null || graphId.isBlank()) {
            throw new IllegalArgumentException("graph_id is required for task/plan lookup");
        }

        return agentGraphRepository.findByIdAndTenantIdWithAllRelations(graphId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Graph '" + graphId + "' not found for tenant " + tenantId));
    }
}
