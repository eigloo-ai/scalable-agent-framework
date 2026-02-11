package ai.eigloo.agentic.dataplane.service;

import ai.eigloo.agentic.dataplane.dto.RunTimelineEvent;
import ai.eigloo.agentic.dataplane.dto.RunTimelineResponse;
import ai.eigloo.agentic.dataplane.entity.PlanExecutionEntity;
import ai.eigloo.agentic.dataplane.entity.TaskExecutionEntity;
import ai.eigloo.agentic.dataplane.repository.PlanExecutionRepository;
import ai.eigloo.agentic.dataplane.repository.TaskExecutionRepository;
import ai.eigloo.agentic.graph.entity.GraphRunEntity;
import ai.eigloo.agentic.graph.repository.GraphRunRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Read-model service for graph run status and execution timeline.
 */
@Service
@Transactional(readOnly = true)
public class RunTimelineService {

    private final GraphRunRepository graphRunRepository;
    private final PlanExecutionRepository planExecutionRepository;
    private final TaskExecutionRepository taskExecutionRepository;

    public RunTimelineService(
            GraphRunRepository graphRunRepository,
            PlanExecutionRepository planExecutionRepository,
            TaskExecutionRepository taskExecutionRepository) {
        this.graphRunRepository = graphRunRepository;
        this.planExecutionRepository = planExecutionRepository;
        this.taskExecutionRepository = taskExecutionRepository;
    }

    public RunTimelineResponse getTimeline(String tenantId, String graphId, String lifetimeId) {
        GraphRunEntity graphRun = graphRunRepository.findByLifetimeIdAndTenantId(lifetimeId, tenantId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Graph run not found for tenant=" + tenantId + " lifetime_id=" + lifetimeId));

        if (!graphId.equals(graphRun.getGraphId())) {
            throw new IllegalArgumentException("graphId does not match run lifetime_id");
        }

        List<PlanExecutionEntity> planExecutions = planExecutionRepository
                .findByTenantIdAndGraphIdAndLifetimeIdOrderByCreatedAtAsc(tenantId, graphId, lifetimeId);
        List<TaskExecutionEntity> taskExecutions = taskExecutionRepository
                .findByTenantIdAndGraphIdAndLifetimeIdOrderByCreatedAtAsc(tenantId, graphId, lifetimeId);

        List<RunTimelineEvent> events = new ArrayList<>(planExecutions.size() + taskExecutions.size());
        for (PlanExecutionEntity planExecution : planExecutions) {
            RunTimelineEvent event = new RunTimelineEvent();
            event.setEventType("PLAN_EXECUTION");
            event.setNodeName(planExecution.getName());
            event.setExecutionId(planExecution.getExecId());
            event.setStatus(planExecution.getStatus() != null ? planExecution.getStatus().name() : "UNKNOWN");
            event.setCreatedAt(planExecution.getCreatedAt());
            event.setPersistedAt(planExecution.getDbCreatedAt());
            event.setParentNodeName(planExecution.getParentTaskNames());
            event.setNextTaskNames(planExecution.getResultNextTaskNames());
            event.setErrorMessage(planExecution.getErrorMessage());
            events.add(event);
        }

        for (TaskExecutionEntity taskExecution : taskExecutions) {
            RunTimelineEvent event = new RunTimelineEvent();
            event.setEventType("TASK_EXECUTION");
            event.setNodeName(taskExecution.getName());
            event.setExecutionId(taskExecution.getExecId());
            event.setStatus(taskExecution.getStatus() != null ? taskExecution.getStatus().name() : "UNKNOWN");
            event.setCreatedAt(taskExecution.getCreatedAt());
            event.setPersistedAt(taskExecution.getDbCreatedAt());
            event.setParentNodeName(taskExecution.getParentPlanName());
            event.setParentExecutionId(taskExecution.getParentPlanExecId());
            events.add(event);
        }

        events.sort(Comparator
                .comparing((RunTimelineEvent e) -> nullableInstant(e.getCreatedAt()))
                .thenComparing(e -> nullableInstant(e.getPersistedAt())));

        RunTimelineResponse response = new RunTimelineResponse();
        response.setTenantId(graphRun.getTenantId());
        response.setGraphId(graphRun.getGraphId());
        response.setLifetimeId(graphRun.getLifetimeId());
        response.setStatus(graphRun.getStatus() != null ? graphRun.getStatus().name() : "UNKNOWN");
        response.setEntryPlanNames(parseEntryPlanNames(graphRun.getEntryPlanNames()));
        response.setErrorMessage(graphRun.getErrorMessage());
        response.setCreatedAt(graphRun.getCreatedAt());
        response.setStartedAt(graphRun.getStartedAt());
        response.setCompletedAt(graphRun.getCompletedAt());
        response.setPlanExecutions(planExecutions.size());
        response.setTaskExecutions(taskExecutions.size());
        response.setEvents(events);
        return response;
    }

    private static List<String> parseEntryPlanNames(String entryPlanNames) {
        if (entryPlanNames == null || entryPlanNames.isBlank()) {
            return List.of();
        }
        String[] parts = entryPlanNames.split(",");
        List<String> values = new ArrayList<>(parts.length);
        for (String part : parts) {
            String value = part.trim();
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return values;
    }

    private static Instant nullableInstant(Instant instant) {
        return instant != null ? instant : Instant.EPOCH;
    }
}
