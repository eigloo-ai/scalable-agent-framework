package ai.eigloo.agentic.dataplane.controller;

import ai.eigloo.agentic.dataplane.service.InternalGraphQueryService;
import ai.eigloo.agentic.graph.api.GraphLookupResponse;
import ai.eigloo.agentic.graph.api.GraphRunStateResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.NoSuchElementException;

/**
 * Internal read APIs used by services that resolve graph/run state without DB access.
 */
@RestController
@RequestMapping("/internal/v1")
public class InternalGraphController {

    private final InternalGraphQueryService internalGraphQueryService;

    public InternalGraphController(InternalGraphQueryService internalGraphQueryService) {
        this.internalGraphQueryService = internalGraphQueryService;
    }

    @GetMapping("/graphs/{graphId}")
    public ResponseEntity<GraphLookupResponse> getGraph(
            @PathVariable String graphId,
            @RequestParam String tenantId) {
        try {
            GraphLookupResponse response = internalGraphQueryService.getGraphLookup(tenantId, graphId);
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @GetMapping("/runs/{lifetimeId}/state")
    public ResponseEntity<GraphRunStateResponse> getRunState(
            @PathVariable String lifetimeId,
            @RequestParam String tenantId,
            @RequestParam String graphId) {
        try {
            GraphRunStateResponse response = internalGraphQueryService.getRunState(tenantId, graphId, lifetimeId);
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }
}
