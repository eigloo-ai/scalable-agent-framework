package ai.eigloo.agentic.executorjava.service;

import ai.eigloo.agentic.executorjava.config.DataPlaneClientProperties;
import ai.eigloo.agentic.graph.api.GraphLookupResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Optional;

/**
 * Data-plane client used by executor-java for graph/node source lookups.
 */
@Service
public class DataPlaneGraphClient {

    private final RestClient restClient;

    public DataPlaneGraphClient(DataPlaneClientProperties properties, RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.baseUrl(normalizeBaseUrl(properties.getBaseUrl())).build();
    }

    public Optional<GraphLookupResponse> getGraph(String tenantId, String graphId) {
        try {
            GraphLookupResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/internal/v1/graphs/{graphId}")
                            .queryParam("tenantId", tenantId)
                            .build(graphId))
                    .retrieve()
                    .body(GraphLookupResponse.class);
            return Optional.ofNullable(response);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            throw ex;
        }
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://localhost:8081";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
