package ai.eigloo.agentic.graphcomposer.service;

import ai.eigloo.agentic.graph.entity.GraphRunEntity;
import ai.eigloo.agentic.graph.repository.AgentGraphRepository;
import ai.eigloo.agentic.graph.repository.GraphRunRepository;
import ai.eigloo.agentic.graphcomposer.config.DataPlaneClientProperties;
import ai.eigloo.agentic.graphcomposer.dto.GraphRunSummary;
import ai.eigloo.agentic.graphcomposer.dto.RunTimelineResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Read-model service for tenant run summaries and run timeline observability.
 */
@Service
@Transactional(readOnly = true)
public class RunObservabilityService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 200;

    private final AgentGraphRepository agentGraphRepository;
    private final GraphRunRepository graphRunRepository;
    private final RestClient restClient;

    public RunObservabilityService(
            AgentGraphRepository agentGraphRepository,
            GraphRunRepository graphRunRepository,
            DataPlaneClientProperties properties,
            RestClient.Builder restClientBuilder) {
        this.agentGraphRepository = agentGraphRepository;
        this.graphRunRepository = graphRunRepository;
        this.restClient = restClientBuilder.baseUrl(properties.getBaseUrl()).build();
    }

    public List<GraphRunSummary> listRuns(String tenantId, String graphId, Integer requestedLimit) {
        ensureGraphExists(tenantId, graphId);
        int limit = sanitizeLimit(requestedLimit);

        List<GraphRunEntity> runs = graphRunRepository.findByTenantIdAndGraphIdOrderByCreatedAtDesc(tenantId, graphId);
        List<GraphRunSummary> summaries = new ArrayList<>(Math.min(limit, runs.size()));
        for (int i = 0; i < runs.size() && i < limit; i++) {
            summaries.add(toSummary(runs.get(i)));
        }
        return summaries;
    }

    public RunTimelineResponse getTimeline(String tenantId, String graphId, String lifetimeId) {
        ensureGraphExists(tenantId, graphId);
        try {
            RunTimelineResponse timeline = restClient.get()
                    .uri(builder -> builder.path("/api/v1/runs/{lifetimeId}/timeline")
                            .queryParam("tenantId", tenantId)
                            .queryParam("graphId", graphId)
                            .build(lifetimeId))
                    .retrieve()
                    .body(RunTimelineResponse.class);
            if (timeline == null) {
                throw new IllegalStateException("Timeline response was empty from data-plane");
            }
            return timeline;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new NoSuchElementException("Run timeline not found for lifetime_id=" + lifetimeId);
            }
            if (ex.getStatusCode().value() == 400) {
                throw new IllegalArgumentException("Invalid timeline request: " + ex.getResponseBodyAsString());
            }
            throw new IllegalStateException("Failed to fetch run timeline from data-plane", ex);
        }
    }

    private void ensureGraphExists(String tenantId, String graphId) {
        if (agentGraphRepository.findByIdAndTenantId(graphId, tenantId).isEmpty()) {
            throw new GraphService.GraphNotFoundException("Graph not found: " + graphId);
        }
    }

    private static int sanitizeLimit(Integer requestedLimit) {
        if (requestedLimit == null) {
            return DEFAULT_LIMIT;
        }
        if (requestedLimit <= 0) {
            throw new IllegalArgumentException("limit must be greater than 0");
        }
        return Math.min(requestedLimit, MAX_LIMIT);
    }

    private static GraphRunSummary toSummary(GraphRunEntity run) {
        GraphRunSummary summary = new GraphRunSummary();
        summary.setTenantId(run.getTenantId());
        summary.setGraphId(run.getGraphId());
        summary.setLifetimeId(run.getLifetimeId());
        summary.setStatus(run.getStatus() != null ? run.getStatus().name() : "UNKNOWN");
        summary.setEntryPlanNames(parseEntryPlanNames(run.getEntryPlanNames()));
        summary.setErrorMessage(run.getErrorMessage());
        summary.setCreatedAt(run.getCreatedAt());
        summary.setStartedAt(run.getStartedAt());
        summary.setCompletedAt(run.getCompletedAt());
        return summary;
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
}
