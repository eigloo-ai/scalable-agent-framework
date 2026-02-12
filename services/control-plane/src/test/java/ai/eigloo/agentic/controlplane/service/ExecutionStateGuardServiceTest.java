package ai.eigloo.agentic.controlplane.service;

import ai.eigloo.agentic.graph.api.GraphRunStateResponse;
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
    private DataPlaneGraphClient dataPlaneGraphClient;

    private ExecutionStateGuardService service;

    @BeforeEach
    void setUp() {
        service = new ExecutionStateGuardService(dataPlaneGraphClient);
    }

    @Test
    void canRoute_shouldAllowRunningRunWithMatchingGraph() {
        ExecutionHeader header = ExecutionHeader.newBuilder()
                .setGraphId("graph-a")
                .setLifetimeId("life-a")
                .setExecId("exec-a")
                .build();

        GraphRunStateResponse run = new GraphRunStateResponse("tenant-a", "graph-a", "life-a", "RUNNING");
        when(dataPlaneGraphClient.getRunState("tenant-a", "graph-a", "life-a"))
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

        GraphRunStateResponse terminalRun = new GraphRunStateResponse("tenant-a", "graph-a", "life-a", "FAILED");
        when(dataPlaneGraphClient.getRunState("tenant-a", "graph-a", "life-a"))
                .thenReturn(Optional.of(terminalRun));
        assertFalse(service.canRoute("tenant-a", header));

        when(dataPlaneGraphClient.getRunState("tenant-a", "graph-a", "life-a"))
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

        GraphRunStateResponse run = new GraphRunStateResponse("tenant-a", "graph-b", "life-a", "RUNNING");
        when(dataPlaneGraphClient.getRunState("tenant-a", "graph-a", "life-a"))
                .thenReturn(Optional.of(run));

        assertFalse(service.canRoute("tenant-a", header));
    }
}
