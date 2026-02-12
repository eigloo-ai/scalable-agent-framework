package ai.eigloo.agentic.controlplane.service;

import ai.eigloo.agentic.graph.api.GraphLookupEdge;
import ai.eigloo.agentic.graph.api.GraphLookupNodeType;
import ai.eigloo.agentic.graph.api.GraphLookupResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskLookupServiceTest {

    @Mock
    private DataPlaneGraphClient dataPlaneGraphClient;

    @InjectMocks
    private TaskLookupService taskLookupService;

    @Test
    void lookupExecutableTaskNames_ShouldUseEdges_WhenPresent() {
        GraphLookupResponse graph = new GraphLookupResponse(
                "graph-1",
                "tenant-a",
                "ACTIVE",
                List.of(),
                List.of(),
                List.of(
                        new GraphLookupEdge("PlanA", GraphLookupNodeType.PLAN, "Task1A", GraphLookupNodeType.TASK),
                        new GraphLookupEdge("PlanA", GraphLookupNodeType.PLAN, "Task1B", GraphLookupNodeType.TASK),
                        new GraphLookupEdge("PlanB", GraphLookupNodeType.PLAN, "Task2", GraphLookupNodeType.TASK)
                ));

        when(dataPlaneGraphClient.getGraph("tenant-a", "graph-1")).thenReturn(Optional.of(graph));

        List<String> resolved = taskLookupService.lookupExecutableTaskNames(
                List.of("Task1B", "Task1A", "Task1A", "TaskX"),
                "tenant-a",
                "graph-1",
                "PlanA");

        assertEquals(List.of("Task1B", "Task1A"), resolved);
    }

    @Test
    void lookupDownstreamPlanNames_ShouldUseEdges_WhenPresent() {
        GraphLookupResponse graph = new GraphLookupResponse(
                "graph-1",
                "tenant-a",
                "ACTIVE",
                List.of(),
                List.of(),
                List.of(
                        new GraphLookupEdge("Task1A", GraphLookupNodeType.TASK, "PlanB", GraphLookupNodeType.PLAN),
                        new GraphLookupEdge("Task1A", GraphLookupNodeType.TASK, "PlanC", GraphLookupNodeType.PLAN),
                        new GraphLookupEdge("Task1A", GraphLookupNodeType.TASK, "PlanB", GraphLookupNodeType.PLAN)
                ));

        when(dataPlaneGraphClient.getGraph("tenant-a", "graph-1")).thenReturn(Optional.of(graph));

        List<String> downstreamPlans = taskLookupService.lookupDownstreamPlanNames("Task1A", "tenant-a", "graph-1");

        assertEquals(List.of("PlanB", "PlanC"), downstreamPlans);
    }

    @Test
    void lookupDownstreamPlanNames_ShouldReturnEmpty_WhenEdgesAbsent() {
        GraphLookupResponse graph = new GraphLookupResponse(
                "graph-1",
                "tenant-a",
                "ACTIVE",
                List.of(),
                List.of()
        );

        when(dataPlaneGraphClient.getGraph("tenant-a", "graph-1")).thenReturn(Optional.of(graph));

        List<String> downstreamPlans = taskLookupService.lookupDownstreamPlanNames("Task1A", "tenant-a", "graph-1");

        assertEquals(List.of(), downstreamPlans);
    }

    @Test
    void lookupExecutableTaskNames_ShouldRejectMissingGraphId() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> taskLookupService.lookupExecutableTaskNames(
                        List.of("Task1A"),
                        "tenant-a",
                        "",
                        "PlanA"));

        assertEquals("graph_id is required for task/plan lookup", ex.getMessage());
    }
}
