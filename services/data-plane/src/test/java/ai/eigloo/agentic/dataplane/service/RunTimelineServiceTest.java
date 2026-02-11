package ai.eigloo.agentic.dataplane.service;

import ai.eigloo.agentic.dataplane.dto.RunTimelineResponse;
import ai.eigloo.agentic.dataplane.entity.PlanExecutionEntity;
import ai.eigloo.agentic.dataplane.entity.TaskExecutionEntity;
import ai.eigloo.agentic.dataplane.repository.PlanExecutionRepository;
import ai.eigloo.agentic.dataplane.repository.TaskExecutionRepository;
import ai.eigloo.agentic.graph.entity.GraphRunEntity;
import ai.eigloo.agentic.graph.entity.GraphRunStatus;
import ai.eigloo.agentic.graph.repository.GraphRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RunTimelineServiceTest {

    @Mock
    private GraphRunRepository graphRunRepository;

    @Mock
    private PlanExecutionRepository planExecutionRepository;

    @Mock
    private TaskExecutionRepository taskExecutionRepository;

    private RunTimelineService runTimelineService;

    @BeforeEach
    void setUp() {
        runTimelineService = new RunTimelineService(
                graphRunRepository,
                planExecutionRepository,
                taskExecutionRepository);
    }

    @Test
    void getTimeline_returnsRunWithSortedEvents() {
        GraphRunEntity run = new GraphRunEntity();
        run.setLifetimeId("life-1");
        run.setTenantId("tenant-a");
        run.setGraphId("graph-a");
        run.setStatus(GraphRunStatus.RUNNING);
        run.setEntryPlanNames("PlanA");
        run.setCreatedAt(Instant.now().minusSeconds(100));
        run.setStartedAt(Instant.now().minusSeconds(90));
        when(graphRunRepository.findByLifetimeIdAndTenantId("life-1", "tenant-a"))
                .thenReturn(Optional.of(run));

        PlanExecutionEntity planExecution = new PlanExecutionEntity();
        planExecution.setExecId("plan-exec-1");
        planExecution.setName("PlanA");
        planExecution.setStatus(PlanExecutionEntity.ExecutionStatus.EXECUTION_STATUS_SUCCEEDED);
        planExecution.setCreatedAt(Instant.parse("2026-02-11T00:00:05Z"));
        planExecution.setDbCreatedAt(Instant.parse("2026-02-11T00:00:06Z"));
        planExecution.setResultNextTaskNames(List.of("Task1A"));

        TaskExecutionEntity taskExecution = new TaskExecutionEntity();
        taskExecution.setExecId("task-exec-1");
        taskExecution.setName("Task1A");
        taskExecution.setStatus(TaskExecutionEntity.ExecutionStatus.EXECUTION_STATUS_SUCCEEDED);
        taskExecution.setCreatedAt(Instant.parse("2026-02-11T00:00:03Z"));
        taskExecution.setDbCreatedAt(Instant.parse("2026-02-11T00:00:04Z"));
        taskExecution.setParentPlanName("PlanA");
        taskExecution.setParentPlanExecId("plan-exec-1");

        when(planExecutionRepository.findByTenantIdAndGraphIdAndLifetimeIdOrderByCreatedAtAsc("tenant-a", "graph-a", "life-1"))
                .thenReturn(List.of(planExecution));
        when(taskExecutionRepository.findByTenantIdAndGraphIdAndLifetimeIdOrderByCreatedAtAsc("tenant-a", "graph-a", "life-1"))
                .thenReturn(List.of(taskExecution));

        RunTimelineResponse response = runTimelineService.getTimeline("tenant-a", "graph-a", "life-1");

        assertEquals("RUNNING", response.getStatus());
        assertEquals(1, response.getPlanExecutions());
        assertEquals(1, response.getTaskExecutions());
        assertEquals(2, response.getEvents().size());
        assertEquals("TASK_EXECUTION", response.getEvents().get(0).getEventType());
        assertEquals("PLAN_EXECUTION", response.getEvents().get(1).getEventType());
    }

    @Test
    void getTimeline_throwsWhenRunNotFound() {
        when(graphRunRepository.findByLifetimeIdAndTenantId("missing", "tenant-a"))
                .thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> runTimelineService.getTimeline("tenant-a", "graph-a", "missing"));
    }
}
