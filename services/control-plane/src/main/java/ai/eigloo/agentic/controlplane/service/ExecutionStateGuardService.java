package ai.eigloo.agentic.controlplane.service;

import ai.eigloo.agentic.graph.api.GraphRunStateResponse;
import ai.eigloo.proto.model.Common.ExecutionHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Enforces runtime execution state checks before control-plane routing.
 */
@Service
public class ExecutionStateGuardService {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionStateGuardService.class);

    private final DataPlaneGraphClient dataPlaneGraphClient;

    public ExecutionStateGuardService(DataPlaneGraphClient dataPlaneGraphClient) {
        this.dataPlaneGraphClient = dataPlaneGraphClient;
    }

    /**
     * Returns true when a persisted execution can be routed to downstream nodes.
     */
    public boolean canRoute(String tenantId, ExecutionHeader header) {
        if (header == null || header.getGraphId().isBlank() || header.getLifetimeId().isBlank()) {
            logger.warn("Rejecting route due to missing graph/lifetime context tenant={}", tenantId);
            return false;
        }

        Optional<GraphRunStateResponse> runOptional = dataPlaneGraphClient.getRunState(
                tenantId,
                header.getGraphId(),
                header.getLifetimeId());
        if (runOptional.isEmpty()) {
            logger.warn(
                    "Rejecting route: graph run not found tenant={} graph={} lifetime={} exec={}",
                    tenantId, header.getGraphId(), header.getLifetimeId(), header.getExecId());
            return false;
        }

        GraphRunStateResponse run = runOptional.get();
        if (!header.getGraphId().equals(run.getGraphId())) {
            logger.warn(
                    "Rejecting route: graph_id mismatch tenant={} headerGraph={} runGraph={} lifetime={} exec={}",
                    tenantId, header.getGraphId(), run.getGraphId(), header.getLifetimeId(), header.getExecId());
            return false;
        }

        String runStatus = run.getStatus();
        if (!"RUNNING".equals(runStatus)) {
            logger.info(
                    "Rejecting route for non-running graph run tenant={} graph={} lifetime={} status={} exec={}",
                    tenantId, run.getGraphId(), run.getLifetimeId(), runStatus, header.getExecId());
            return false;
        }

        return true;
    }
}
