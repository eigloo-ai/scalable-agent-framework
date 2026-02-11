package ai.eigloo.agentic.dataplane.controller;

import ai.eigloo.agentic.dataplane.dto.RunTimelineResponse;
import ai.eigloo.agentic.dataplane.service.RunTimelineService;
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
 * Read API for graph run observability timelines.
 */
@RestController
@RequestMapping("/api/v1/runs")
public class RunTimelineController {

    private final RunTimelineService runTimelineService;

    public RunTimelineController(RunTimelineService runTimelineService) {
        this.runTimelineService = runTimelineService;
    }

    @GetMapping("/{lifetimeId}/timeline")
    public ResponseEntity<RunTimelineResponse> getTimeline(
            @PathVariable String lifetimeId,
            @RequestParam String tenantId,
            @RequestParam String graphId) {
        try {
            RunTimelineResponse response = runTimelineService.getTimeline(tenantId, graphId, lifetimeId);
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }
}
