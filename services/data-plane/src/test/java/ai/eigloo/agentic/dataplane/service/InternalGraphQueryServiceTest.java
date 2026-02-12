package ai.eigloo.agentic.dataplane.service;

import ai.eigloo.agentic.graph.api.GraphLookupEdge;
import ai.eigloo.agentic.graph.api.GraphLookupNodeType;
import ai.eigloo.agentic.graph.api.GraphLookupResponse;
import ai.eigloo.agentic.graph.entity.AgentGraphEntity;
import ai.eigloo.agentic.graph.entity.GraphEdgeEntity;
import ai.eigloo.agentic.graph.entity.GraphStatus;
import ai.eigloo.agentic.graph.entity.PlanEntity;
import ai.eigloo.agentic.graph.entity.TaskEntity;
import ai.eigloo.agentic.graph.repository.AgentGraphRepository;
import ai.eigloo.agentic.graph.repository.GraphRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalGraphQueryServiceTest {

    @Mock
    private AgentGraphRepository agentGraphRepository;

    @Mock
    private GraphRunRepository graphRunRepository;

    private InternalGraphQueryService internalGraphQueryService;

    @BeforeEach
    void setUp() {
        internalGraphQueryService = new InternalGraphQueryService(agentGraphRepository, graphRunRepository);
    }

    @Test
    void getGraphLookup_ShouldExposeDirectedEdgesFromPersistedRelationships() {
        AgentGraphEntity graph = new AgentGraphEntity();
        graph.setId("graph-1");
        graph.setTenantId("tenant-a");
        graph.setName("Graph 1");
        graph.setStatus(GraphStatus.ACTIVE);

        PlanEntity planA = new PlanEntity();
        planA.setName("PlanA");
        planA.setAgentGraph(graph);

        PlanEntity planB = new PlanEntity();
        planB.setName("PlanB");
        planB.setAgentGraph(graph);

        TaskEntity task1A = new TaskEntity();
        task1A.setName("Task1A");
        task1A.setAgentGraph(graph);

        TaskEntity task1B = new TaskEntity();
        task1B.setName("Task1B");
        task1B.setAgentGraph(graph);

        graph.setPlans(List.of(planB, planA));
        graph.setTasks(List.of(task1B, task1A));
        graph.setEdges(List.of(
                new GraphEdgeEntity(
                        "edge-1",
                        graph,
                        "PlanA",
                        ai.eigloo.agentic.graph.model.GraphNodeType.PLAN,
                        "Task1A",
                        ai.eigloo.agentic.graph.model.GraphNodeType.TASK),
                new GraphEdgeEntity(
                        "edge-2",
                        graph,
                        "PlanA",
                        ai.eigloo.agentic.graph.model.GraphNodeType.PLAN,
                        "Task1B",
                        ai.eigloo.agentic.graph.model.GraphNodeType.TASK),
                new GraphEdgeEntity(
                        "edge-3",
                        graph,
                        "Task1A",
                        ai.eigloo.agentic.graph.model.GraphNodeType.TASK,
                        "PlanB",
                        ai.eigloo.agentic.graph.model.GraphNodeType.PLAN)
        ));

        when(agentGraphRepository.findByIdAndTenantIdWithAllRelations("graph-1", "tenant-a"))
                .thenReturn(Optional.of(graph));

        GraphLookupResponse response = internalGraphQueryService.getGraphLookup("tenant-a", "graph-1");

        assertEquals(List.of("PlanA", "PlanB"),
                response.getPlans().stream().map(plan -> plan.getName()).toList());
        assertEquals(List.of("Task1A", "Task1B"),
                response.getTasks().stream().map(task -> task.getName()).toList());

        Set<String> edgeSignatures = response.getEdges().stream()
                .map(this::signature)
                .collect(Collectors.toSet());

        assertEquals(
                Set.of(
                        "PLAN:PlanA->TASK:Task1A",
                        "PLAN:PlanA->TASK:Task1B",
                        "TASK:Task1A->PLAN:PlanB"),
                edgeSignatures);
    }

    @Test
    void getGraphLookup_ShouldThrow_WhenGraphMissing() {
        when(agentGraphRepository.findByIdAndTenantIdWithAllRelations("missing", "tenant-a"))
                .thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> internalGraphQueryService.getGraphLookup("tenant-a", "missing"));
    }

    private String signature(GraphLookupEdge edge) {
        return edge.getFromType() + ":" + edge.getFrom() + "->" + edge.getToType() + ":" + edge.getTo();
    }
}
