package ai.eigloo.agentic.dataplane.service;

import ai.eigloo.agentic.dataplane.entity.PlanExecutionEntity;
import ai.eigloo.agentic.dataplane.entity.TaskExecutionEntity;
import ai.eigloo.agentic.dataplane.repository.PlanExecutionRepository;
import ai.eigloo.agentic.dataplane.repository.TaskExecutionRepository;
import ai.eigloo.agentic.graph.entity.AgentGraphEntity;
import ai.eigloo.agentic.graph.entity.GraphRunEntity;
import ai.eigloo.agentic.graph.entity.GraphRunStatus;
import ai.eigloo.agentic.graph.entity.PlanEntity;
import ai.eigloo.agentic.graph.entity.TaskEntity;
import ai.eigloo.agentic.graph.repository.AgentGraphRepository;
import ai.eigloo.agentic.graph.repository.GraphRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Maintains graph run status transitions from persisted execution events.
 */
@Service
public class GraphRunLifecycleService {

    private static final Logger logger = LoggerFactory.getLogger(GraphRunLifecycleService.class);
    private static final int MAX_ERROR_LENGTH = 1000;

    private final GraphRunRepository graphRunRepository;
    private final PlanExecutionRepository planExecutionRepository;
    private final TaskExecutionRepository taskExecutionRepository;
    private final AgentGraphRepository agentGraphRepository;

    public GraphRunLifecycleService(
            GraphRunRepository graphRunRepository,
            PlanExecutionRepository planExecutionRepository,
            TaskExecutionRepository taskExecutionRepository,
            AgentGraphRepository agentGraphRepository) {
        this.graphRunRepository = graphRunRepository;
        this.planExecutionRepository = planExecutionRepository;
        this.taskExecutionRepository = taskExecutionRepository;
        this.agentGraphRepository = agentGraphRepository;
    }

    /**
     * Apply lifecycle transitions when a plan execution is persisted.
     */
    @Transactional
    public void onPlanExecutionPersisted(PlanExecutionEntity planExecution) {
        ExecutionUpdate update = new ExecutionUpdate(
                planExecution.getTenantId(),
                planExecution.getGraphId(),
                planExecution.getLifetimeId(),
                isFailedStatus(planExecution.getStatus()),
                isSuccessfulStatus(planExecution.getStatus()),
                planExecution.getCreatedAt(),
                planExecution.getErrorMessage());
        applyUpdate(update);
    }

    /**
     * Apply lifecycle transitions when a task execution is persisted.
     *
     * @param taskExecution the persisted task execution entity
     * @param errorMessage  error message from the TaskResult (may be null)
     */
    @Transactional
    public void onTaskExecutionPersisted(TaskExecutionEntity taskExecution, String errorMessage) {
        ExecutionUpdate update = new ExecutionUpdate(
                taskExecution.getTenantId(),
                taskExecution.getGraphId(),
                taskExecution.getLifetimeId(),
                isFailedStatus(taskExecution.getStatus()),
                isSuccessfulStatus(taskExecution.getStatus()),
                taskExecution.getCreatedAt(),
                errorMessage);
        applyUpdate(update);
    }

    private void applyUpdate(ExecutionUpdate update) {
        if (isBlank(update.tenantId()) || isBlank(update.graphId()) || isBlank(update.lifetimeId())) {
            logger.warn(
                    "Skipping graph run lifecycle update due to missing context tenant={} graph={} lifetime={}",
                    update.tenantId(), update.graphId(), update.lifetimeId());
            return;
        }

        GraphRunEntity graphRun = graphRunRepository.findByLifetimeIdAndTenantId(update.lifetimeId(), update.tenantId())
                .orElseGet(() -> createPlaceholderRun(update));

        GraphRunStatus previousStatus = currentStatus(graphRun);
        if (previousStatus.isTerminal()) {
            logger.debug(
                    "Skipping lifecycle update for terminal run tenant={} graph={} lifetime={} status={}",
                    graphRun.getTenantId(), graphRun.getGraphId(), graphRun.getLifetimeId(), previousStatus);
            return;
        }

        if (update.failed()) {
            markFailed(graphRun, compactError(update.errorMessage()), update.createdAt());
            logger.info(
                    "Graph run transitioned tenant={} graph={} lifetime={} {} -> {}",
                    graphRun.getTenantId(), graphRun.getGraphId(), graphRun.getLifetimeId(), previousStatus, graphRun.getStatus());
            return;
        }

        if (graphRun.getStartedAt() == null) {
            graphRun.setStartedAt(fallbackInstant(update.createdAt()));
        }
        transitionRunStatus(graphRun, GraphRunStatus.RUNNING, "execution persisted");

        if (update.succeeded() && isRunComplete(update.tenantId(), update.graphId(), update.lifetimeId())) {
            transitionRunStatus(graphRun, GraphRunStatus.SUCCEEDED, "all graph edges resolved");
            graphRun.setCompletedAt(fallbackInstant(update.createdAt()));
            graphRun.setErrorMessage(null);
        }

        graphRunRepository.save(graphRun);

        if (graphRun.getStatus() != previousStatus) {
            logger.info(
                    "Graph run transitioned tenant={} graph={} lifetime={} {} -> {}",
                    graphRun.getTenantId(), graphRun.getGraphId(), graphRun.getLifetimeId(), previousStatus, graphRun.getStatus());
        }
    }

    private GraphRunEntity createPlaceholderRun(ExecutionUpdate update) {
        GraphRunEntity graphRun = new GraphRunEntity();
        graphRun.setLifetimeId(update.lifetimeId());
        graphRun.setTenantId(update.tenantId());
        graphRun.setGraphId(update.graphId());
        graphRun.setStatus(GraphRunStatus.QUEUED);
        graphRun.setCreatedAt(fallbackInstant(update.createdAt()));
        graphRun.setStartedAt(fallbackInstant(update.createdAt()));
        logger.warn(
                "Creating placeholder graph run for missing lifetime record tenant={} graph={} lifetime={}",
                update.tenantId(), update.graphId(), update.lifetimeId());
        return graphRunRepository.save(graphRun);
    }

    private void markFailed(GraphRunEntity graphRun, String errorMessage, Instant eventTime) {
        transitionRunStatus(graphRun, GraphRunStatus.FAILED, "execution failure persisted");
        graphRun.setStartedAt(graphRun.getStartedAt() == null ? fallbackInstant(eventTime) : graphRun.getStartedAt());
        graphRun.setCompletedAt(fallbackInstant(eventTime));
        graphRun.setErrorMessage(errorMessage);
        graphRunRepository.save(graphRun);
    }

    private static GraphRunStatus currentStatus(GraphRunEntity graphRun) {
        return graphRun.getStatus() != null ? graphRun.getStatus() : GraphRunStatus.QUEUED;
    }

    private boolean transitionRunStatus(GraphRunEntity graphRun, GraphRunStatus targetStatus, String reason) {
        GraphRunStatus currentStatus = currentStatus(graphRun);
        if (!currentStatus.canTransitionTo(targetStatus)) {
            logger.warn(
                    "Illegal graph run transition ignored tenant={} graph={} lifetime={} {} -> {} reason={}",
                    graphRun.getTenantId(),
                    graphRun.getGraphId(),
                    graphRun.getLifetimeId(),
                    currentStatus,
                    targetStatus,
                    reason);
            return false;
        }
        graphRun.setStatus(targetStatus);
        return true;
    }

    private boolean isRunComplete(String tenantId, String graphId, String lifetimeId) {
        Optional<AgentGraphEntity> graphOptional = agentGraphRepository.findByIdAndTenantIdWithAllRelations(graphId, tenantId);
        if (graphOptional.isEmpty()) {
            logger.warn(
                    "Cannot evaluate completion: graph not found tenant={} graph={} lifetime={}",
                    tenantId, graphId, lifetimeId);
            return false;
        }

        AgentGraphEntity graph = graphOptional.get();
        List<PlanExecutionEntity> planExecutions = planExecutionRepository
                .findByTenantIdAndGraphIdAndLifetimeIdOrderByCreatedAtAsc(tenantId, graphId, lifetimeId);
        List<TaskExecutionEntity> taskExecutions = taskExecutionRepository
                .findByTenantIdAndGraphIdAndLifetimeIdOrderByCreatedAtAsc(tenantId, graphId, lifetimeId);

        if (planExecutions.isEmpty() && taskExecutions.isEmpty()) {
            return false;
        }

        boolean hasFailures = planExecutions.stream().anyMatch(p -> isFailedStatus(p.getStatus()))
                || taskExecutions.stream().anyMatch(t -> isFailedStatus(t.getStatus()));
        if (hasFailures) {
            return false;
        }

        Set<String> successfulPlanNames = planExecutions.stream()
                .filter(p -> isSuccessfulStatus(p.getStatus()))
                .map(PlanExecutionEntity::getName)
                .filter(name -> !isBlank(name))
                .collect(Collectors.toSet());

        Set<String> successfulTaskNames = taskExecutions.stream()
                .filter(t -> isSuccessfulStatus(t.getStatus()))
                .map(TaskExecutionEntity::getName)
                .filter(name -> !isBlank(name))
                .collect(Collectors.toSet());

        if (successfulPlanNames.isEmpty() && successfulTaskNames.isEmpty()) {
            return false;
        }

        Set<String> graphTaskNames = graph.getTasks().stream()
                .map(TaskEntity::getName)
                .filter(name -> !isBlank(name))
                .collect(Collectors.toSet());

        Set<String> entryPlanNames = resolveEntryPlanNames(graph);
        if (!successfulPlanNames.containsAll(entryPlanNames)) {
            return false;
        }

        for (PlanExecutionEntity planExecution : planExecutions) {
            if (!isSuccessfulStatus(planExecution.getStatus())) {
                continue;
            }
            List<String> nextTaskNames = planExecution.getResultNextTaskNames() != null
                    ? planExecution.getResultNextTaskNames()
                    : Collections.emptyList();
            for (String taskName : nextTaskNames) {
                if (isBlank(taskName) || !graphTaskNames.contains(taskName)) {
                    continue;
                }
                if (!successfulTaskNames.contains(taskName)) {
                    return false;
                }
            }
        }

        Map<String, Set<String>> downstreamPlansByTaskName = new HashMap<>();
        for (var edge : graph.getEdges()) {
            if (edge.getFromNodeType() == ai.eigloo.agentic.graph.model.GraphNodeType.TASK
                    && edge.getToNodeType() == ai.eigloo.agentic.graph.model.GraphNodeType.PLAN
                    && !isBlank(edge.getFromNodeName())
                    && !isBlank(edge.getToNodeName())) {
                downstreamPlansByTaskName
                        .computeIfAbsent(edge.getFromNodeName(), k -> new HashSet<>())
                        .add(edge.getToNodeName());
            }
        }

        for (TaskExecutionEntity taskExecution : taskExecutions) {
            if (!isSuccessfulStatus(taskExecution.getStatus())) {
                continue;
            }
            Set<String> downstreamPlans = downstreamPlansByTaskName.getOrDefault(
                    taskExecution.getName(), Collections.emptySet());
            for (String downstreamPlan : downstreamPlans) {
                if (!successfulPlanNames.contains(downstreamPlan)) {
                    return false;
                }
            }
        }

        return true;
    }

    private static Set<String> resolveEntryPlanNames(AgentGraphEntity graph) {
        Set<String> plansWithUpstreamTasks = graph.getEdges().stream()
                .filter(edge -> edge.getFromNodeType() == ai.eigloo.agentic.graph.model.GraphNodeType.TASK)
                .filter(edge -> edge.getToNodeType() == ai.eigloo.agentic.graph.model.GraphNodeType.PLAN)
                .map(edge -> edge.getToNodeName())
                .filter(name -> !isBlank(name))
                .collect(Collectors.toSet());

        Set<String> entryPlans = new HashSet<>();
        for (PlanEntity plan : graph.getPlans()) {
            if (isBlank(plan.getName())) {
                continue;
            }
            if (!plansWithUpstreamTasks.contains(plan.getName())) {
                entryPlans.add(plan.getName());
            }
        }
        return entryPlans;
    }

    private static boolean isSuccessfulStatus(PlanExecutionEntity.ExecutionStatus status) {
        return status == PlanExecutionEntity.ExecutionStatus.EXECUTION_STATUS_SUCCEEDED;
    }

    private static boolean isSuccessfulStatus(TaskExecutionEntity.ExecutionStatus status) {
        return status == TaskExecutionEntity.ExecutionStatus.EXECUTION_STATUS_SUCCEEDED;
    }

    private static boolean isFailedStatus(PlanExecutionEntity.ExecutionStatus status) {
        return status == PlanExecutionEntity.ExecutionStatus.EXECUTION_STATUS_FAILED;
    }

    private static boolean isFailedStatus(TaskExecutionEntity.ExecutionStatus status) {
        return status == TaskExecutionEntity.ExecutionStatus.EXECUTION_STATUS_FAILED;
    }

    private static Instant fallbackInstant(Instant instant) {
        return instant != null ? instant : Instant.now();
    }

    private static String compactError(String message) {
        if (message == null || message.isBlank()) {
            return "Execution failed";
        }
        if (message.length() > MAX_ERROR_LENGTH) {
            return message.substring(0, MAX_ERROR_LENGTH);
        }
        return message;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record ExecutionUpdate(
            String tenantId,
            String graphId,
            String lifetimeId,
            boolean failed,
            boolean succeeded,
            Instant createdAt,
            String errorMessage) {
    }
}
