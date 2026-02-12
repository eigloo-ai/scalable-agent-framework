package ai.eigloo.agentic.graphcomposer.controller;

import ai.eigloo.agentic.graphcomposer.dto.GraphRunSummary;
import ai.eigloo.agentic.graphcomposer.dto.RunTimelineResponse;
import ai.eigloo.agentic.graphcomposer.service.RunObservabilityService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Tenant read APIs for graph execution run summaries and observability timelines.
 */
@RestController
@RequestMapping("/api/v1/graphs/{graphId}/runs")
@CrossOrigin(origins = "*", maxAge = 3600)
public class GraphRunController {

    private final RunObservabilityService runObservabilityService;

    public GraphRunController(RunObservabilityService runObservabilityService) {
        this.runObservabilityService = runObservabilityService;
    }

    @GetMapping
    public ResponseEntity<List<GraphRunSummary>> listRuns(
            @PathVariable String graphId,
            @RequestParam String tenantId,
            @RequestParam(required = false) Integer limit) {
        try {
            List<GraphRunSummary> runs = runObservabilityService.listRuns(tenantId, graphId, limit);
            return ResponseEntity.ok(runs);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @GetMapping("/{lifetimeId}/timeline")
    public ResponseEntity<RunTimelineResponse> getTimeline(
            @PathVariable String graphId,
            @PathVariable String lifetimeId,
            @RequestParam String tenantId) {
        try {
            RunTimelineResponse timeline = runObservabilityService.getTimeline(tenantId, graphId, lifetimeId);
            return ResponseEntity.ok(timeline);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }
}
