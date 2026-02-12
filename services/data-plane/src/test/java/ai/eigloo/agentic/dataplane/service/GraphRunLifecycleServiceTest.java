package ai.eigloo.agentic.dataplane.service;

import ai.eigloo.agentic.dataplane.entity.PlanExecutionEntity;
import ai.eigloo.agentic.dataplane.entity.TaskExecutionEntity;
import ai.eigloo.agentic.dataplane.repository.PlanExecutionRepository;
import ai.eigloo.agentic.dataplane.repository.TaskExecutionRepository;
import ai.eigloo.agentic.graph.entity.AgentGraphEntity;
import ai.eigloo.agentic.graph.entity.GraphEdgeEntity;
import ai.eigloo.agentic.graph.entity.GraphRunEntity;
import ai.eigloo.agentic.graph.entity.GraphRunStatus;
import ai.eigloo.agentic.graph.entity.GraphStatus;
import ai.eigloo.agentic.graph.entity.PlanEntity;
import ai.eigloo.agentic.graph.entity.TaskEntity;
import ai.eigloo.agentic.graph.repository.AgentGraphRepository;
import ai.eigloo.agentic.graph.repository.GraphRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GraphRunLifecycleServiceTest {

    @Mock
    private GraphRunRepository graphRunRepository;

    @Mock
    private PlanExecutionRepository planExecutionRepository;

    @Mock
    private TaskExecutionRepository taskExecutionRepository;

    @Mock
    private AgentGraphRepository agentGraphRepository;

    private GraphRunLifecycleService service;

    @BeforeEach
    void setUp() {
        service = new GraphRunLifecycleService(
                graphRunRepository,
                planExecutionRepository,
                taskExecutionRepository,
                agentGraphRepository);
        lenient().when(graphRunRepository.save(any(GraphRunEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void onPlanExecutionPersisted_marksRunFailedWhenPlanFails() {
        GraphRunEntity existingRun = new GraphRunEntity();
        existingRun.setLifetimeId("life-1");
        existingRun.setTenantId("tenant-a");
        existingRun.setGraphId("graph-a");
        existingRun.setStatus(GraphRunStatus.RUNNING);
        existingRun.setCreatedAt(Instant.now().minusSeconds(10));
        existingRun.setStartedAt(Instant.now().minusSeconds(9));

        when(graphRunRepository.findByLifetimeIdAndTenantId("life-1", "tenant-a"))
                .thenReturn(Optional.of(existingRun));

        PlanExecutionEntity failedExecution = new PlanExecutionEntity();
        failedExecution.setTenantId("tenant-a");
        failedExecution.setGraphId("graph-a");
        failedExecution.setLifetimeId("life-1");
        failedExecution.setStatus(PlanExecutionEntity.ExecutionStatus.EXECUTION_STATUS_FAILED);
        failedExecution.setErrorMessage("planner crashed");
        failedExecution.setCreatedAt(Instant.now());

        service.onPlanExecutionPersisted(failedExecution);

        ArgumentCaptor<GraphRunEntity> savedCaptor = ArgumentCaptor.forClass(GraphRunEntity.class);
        verify(graphRunRepository, atLeastOnce()).save(savedCaptor.capture());
        GraphRunEntity savedRun = savedCaptor.getValue();
        assertEquals(GraphRunStatus.FAILED, savedRun.getStatus());
        assertEquals("planner crashed", savedRun.getErrorMessage());
        assertNotNull(savedRun.getCompletedAt());
    }

    @Test
    void onTaskExecutionPersisted_marksRunSucceededWhenAllEdgesResolved() {
        GraphRunEntity existingRun = new GraphRunEntity();
        existingRun.setLifetimeId("life-2");
        existingRun.setTenantId("tenant-a");
        existingRun.setGraphId("graph-a");
        existingRun.setStatus(GraphRunStatus.RUNNING);
        existingRun.setCreatedAt(Instant.now().minusSeconds(30));
        existingRun.setStartedAt(Instant.now().minusSeconds(29));
        existingRun.setEntryPlanNames("PlanA");

        when(graphRunRepository.findByLifetimeIdAndTenantId("life-2", "tenant-a"))
                .thenReturn(Optional.of(existingRun));

        AgentGraphEntity graph = new AgentGraphEntity("graph-a", "tenant-a", "sample", GraphStatus.ACTIVE);
        PlanEntity planA = new PlanEntity("plan-a", "PlanA", "Plan A", "plan.py", graph);
        PlanEntity planB = new PlanEntity("plan-b", "PlanB", "Plan B", "plan.py", graph);
        graph.addPlan(planA);
        graph.addPlan(planB);
        graph.addTask(new TaskEntity("task-1a", "Task1A", "Task1A", "task.py", graph));
        graph.addTask(new TaskEntity("task-1b", "Task1B", "Task1B", "task.py", graph));
        graph.addTask(new TaskEntity("task-2", "Task2", "Task2", "task.py", graph));
        graph.addEdge(new GraphEdgeEntity(
                "edge-1",
                graph,
                "PlanA",
                ai.eigloo.agentic.graph.model.GraphNodeType.PLAN,
                "Task1A",
                ai.eigloo.agentic.graph.model.GraphNodeType.TASK));
        graph.addEdge(new GraphEdgeEntity(
                "edge-2",
                graph,
                "PlanA",
                ai.eigloo.agentic.graph.model.GraphNodeType.PLAN,
                "Task1B",
                ai.eigloo.agentic.graph.model.GraphNodeType.TASK));
        graph.addEdge(new GraphEdgeEntity(
                "edge-3",
                graph,
                "Task1A",
                ai.eigloo.agentic.graph.model.GraphNodeType.TASK,
                "PlanB",
                ai.eigloo.agentic.graph.model.GraphNodeType.PLAN));
        graph.addEdge(new GraphEdgeEntity(
                "edge-4",
                graph,
                "PlanB",
                ai.eigloo.agentic.graph.model.GraphNodeType.PLAN,
                "Task2",
                ai.eigloo.agentic.graph.model.GraphNodeType.TASK));
        when(agentGraphRepository.findByIdAndTenantIdWithAllRelations("graph-a", "tenant-a"))
                .thenReturn(Optional.of(graph));

        PlanExecutionEntity planAExecution = new PlanExecutionEntity();
        planAExecution.setName("PlanA");
        planAExecution.setStatus(PlanExecutionEntity.ExecutionStatus.EXECUTION_STATUS_SUCCEEDED);
        planAExecution.setResultNextTaskNames(List.of("Task1A", "Task1B"));
        planAExecution.setCreatedAt(Instant.now().minusSeconds(20));
        PlanExecutionEntity planBExecution = new PlanExecutionEntity();
        planBExecution.setName("PlanB");
        planBExecution.setStatus(PlanExecutionEntity.ExecutionStatus.EXECUTION_STATUS_SUCCEEDED);
        planBExecution.setResultNextTaskNames(List.of("Task2"));
        planBExecution.setCreatedAt(Instant.now().minusSeconds(10));
        when(planExecutionRepository.findByTenantIdAndGraphIdAndLifetimeIdOrderByCreatedAtAsc("tenant-a", "graph-a", "life-2"))
                .thenReturn(List.of(planAExecution, planBExecution));

        TaskExecutionEntity task1AExecution = new TaskExecutionEntity();
        task1AExecution.setName("Task1A");
        task1AExecution.setStatus(TaskExecutionEntity.ExecutionStatus.EXECUTION_STATUS_SUCCEEDED);
        task1AExecution.setCreatedAt(Instant.now().minusSeconds(18));
        TaskExecutionEntity task1BExecution = new TaskExecutionEntity();
        task1BExecution.setName("Task1B");
        task1BExecution.setStatus(TaskExecutionEntity.ExecutionStatus.EXECUTION_STATUS_SUCCEEDED);
        task1BExecution.setCreatedAt(Instant.now().minusSeconds(17));
        TaskExecutionEntity task2Execution = new TaskExecutionEntity();
        task2Execution.setTenantId("tenant-a");
        task2Execution.setGraphId("graph-a");
        task2Execution.setLifetimeId("life-2");
        task2Execution.setName("Task2");
        task2Execution.setStatus(TaskExecutionEntity.ExecutionStatus.EXECUTION_STATUS_SUCCEEDED);
        task2Execution.setCreatedAt(Instant.now().minusSeconds(5));
        when(taskExecutionRepository.findByTenantIdAndGraphIdAndLifetimeIdOrderByCreatedAtAsc("tenant-a", "graph-a", "life-2"))
                .thenReturn(List.of(task1AExecution, task1BExecution, task2Execution));

        service.onTaskExecutionPersisted(task2Execution);

        ArgumentCaptor<GraphRunEntity> savedCaptor = ArgumentCaptor.forClass(GraphRunEntity.class);
        verify(graphRunRepository, atLeastOnce()).save(savedCaptor.capture());
        GraphRunEntity savedRun = savedCaptor.getValue();
        assertEquals(GraphRunStatus.SUCCEEDED, savedRun.getStatus());
        assertNotNull(savedRun.getCompletedAt());
        verify(planExecutionRepository).findByTenantIdAndGraphIdAndLifetimeIdOrderByCreatedAtAsc("tenant-a", "graph-a", "life-2");
        verify(taskExecutionRepository).findByTenantIdAndGraphIdAndLifetimeIdOrderByCreatedAtAsc("tenant-a", "graph-a", "life-2");
        verify(agentGraphRepository).findByIdAndTenantIdWithAllRelations(eq("graph-a"), eq("tenant-a"));
    }

    @Test
    void onPlanExecutionPersisted_shouldIgnoreUpdatesForTerminalRun() {
        GraphRunEntity existingRun = new GraphRunEntity();
        existingRun.setLifetimeId("life-3");
        existingRun.setTenantId("tenant-a");
        existingRun.setGraphId("graph-a");
        existingRun.setStatus(GraphRunStatus.CANCELED);

        when(graphRunRepository.findByLifetimeIdAndTenantId("life-3", "tenant-a"))
                .thenReturn(Optional.of(existingRun));

        PlanExecutionEntity planExecution = new PlanExecutionEntity();
        planExecution.setTenantId("tenant-a");
        planExecution.setGraphId("graph-a");
        planExecution.setLifetimeId("life-3");
        planExecution.setStatus(PlanExecutionEntity.ExecutionStatus.EXECUTION_STATUS_SUCCEEDED);
        planExecution.setCreatedAt(Instant.now());

        service.onPlanExecutionPersisted(planExecution);

        verify(graphRunRepository, never()).save(any(GraphRunEntity.class));
        verifyNoInteractions(planExecutionRepository, taskExecutionRepository, agentGraphRepository);
    }
}
