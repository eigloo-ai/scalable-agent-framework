package ai.eigloo.agentic.controlplane.service;

import ai.eigloo.agentic.graph.entity.GraphRunEntity;
import ai.eigloo.agentic.graph.entity.GraphRunStatus;
import ai.eigloo.agentic.graph.repository.GraphRunRepository;
import ai.eigloo.proto.model.Common.ExecutionHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExecutionStateGuardServiceTest {

    @Mock
    private GraphRunRepository graphRunRepository;

    private ExecutionStateGuardService service;

    @BeforeEach
    void setUp() {
        service = new ExecutionStateGuardService(graphRunRepository);
    }

    @Test
    void canRoute_shouldAllowRunningRunWithMatchingGraph() {
        ExecutionHeader header = ExecutionHeader.newBuilder()
                .setGraphId("graph-a")
                .setLifetimeId("life-a")
                .setExecId("exec-a")
                .build();

        GraphRunEntity run = new GraphRunEntity();
        run.setGraphId("graph-a");
        run.setTenantId("tenant-a");
        run.setLifetimeId("life-a");
        run.setStatus(GraphRunStatus.RUNNING);

        when(graphRunRepository.findByLifetimeIdAndTenantId("life-a", "tenant-a"))
                .thenReturn(Optional.of(run));

        assertTrue(service.canRoute("tenant-a", header));
    }

    @Test
    void canRoute_shouldRejectTerminalOrMissingRun() {
        ExecutionHeader header = ExecutionHeader.newBuilder()
                .setGraphId("graph-a")
                .setLifetimeId("life-a")
                .setExecId("exec-a")
                .build();

        GraphRunEntity terminalRun = new GraphRunEntity();
        terminalRun.setGraphId("graph-a");
        terminalRun.setTenantId("tenant-a");
        terminalRun.setLifetimeId("life-a");
        terminalRun.setStatus(GraphRunStatus.FAILED);

        when(graphRunRepository.findByLifetimeIdAndTenantId("life-a", "tenant-a"))
                .thenReturn(Optional.of(terminalRun));
        assertFalse(service.canRoute("tenant-a", header));

        when(graphRunRepository.findByLifetimeIdAndTenantId("life-a", "tenant-a"))
                .thenReturn(Optional.empty());
        assertFalse(service.canRoute("tenant-a", header));
    }

    @Test
    void canRoute_shouldRejectGraphMismatch() {
        ExecutionHeader header = ExecutionHeader.newBuilder()
                .setGraphId("graph-a")
                .setLifetimeId("life-a")
                .setExecId("exec-a")
                .build();

        GraphRunEntity run = new GraphRunEntity();
        run.setGraphId("graph-b");
        run.setTenantId("tenant-a");
        run.setLifetimeId("life-a");
        run.setStatus(GraphRunStatus.RUNNING);

        when(graphRunRepository.findByLifetimeIdAndTenantId("life-a", "tenant-a"))
                .thenReturn(Optional.of(run));

        assertFalse(service.canRoute("tenant-a", header));
    }
}
