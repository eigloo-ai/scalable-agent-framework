package ai.eigloo.agentic.graphcomposer.controller;

import ai.eigloo.agentic.graphcomposer.dto.GraphRunSummary;
import ai.eigloo.agentic.graphcomposer.dto.RunTimelineEvent;
import ai.eigloo.agentic.graphcomposer.dto.RunTimelineResponse;
import ai.eigloo.agentic.graphcomposer.exception.GraphComposerExceptionHandler;
import ai.eigloo.agentic.graphcomposer.service.RunObservabilityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GraphRunControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RunObservabilityService runObservabilityService;

    @BeforeEach
    void setup() {
        ObjectMapper objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new GraphRunController(runObservabilityService))
                .setControllerAdvice(new GraphComposerExceptionHandler())
                .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void listRuns_ShouldReturnRunSummaries() throws Exception {
        GraphRunSummary summary = new GraphRunSummary();
        summary.setTenantId("tenant-a");
        summary.setGraphId("graph-1");
        summary.setLifetimeId("life-1");
        summary.setStatus("QUEUED");

        when(runObservabilityService.listRuns("tenant-a", "graph-1", null))
                .thenReturn(List.of(summary));

        mockMvc.perform(get("/api/v1/graphs/graph-1/runs")
                        .param("tenantId", "tenant-a"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].lifetimeId").value("life-1"))
                .andExpect(jsonPath("$[0].status").value("QUEUED"));
    }

    @Test
    void listRuns_ShouldReturnBadRequest_WhenLimitInvalid() throws Exception {
        when(runObservabilityService.listRuns("tenant-a", "graph-1", 0))
                .thenThrow(new IllegalArgumentException("limit must be greater than 0"));

        mockMvc.perform(get("/api/v1/graphs/graph-1/runs")
                        .param("tenantId", "tenant-a")
                        .param("limit", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTimeline_ShouldReturnTimeline() throws Exception {
        RunTimelineEvent event = new RunTimelineEvent();
        event.setEventType("PLAN_EXECUTION");
        event.setNodeName("plan_a");
        event.setStatus("EXECUTION_STATUS_SUCCEEDED");

        RunTimelineResponse response = new RunTimelineResponse();
        response.setTenantId("tenant-a");
        response.setGraphId("graph-1");
        response.setLifetimeId("life-1");
        response.setStatus("RUNNING");
        response.setEvents(List.of(event));

        when(runObservabilityService.getTimeline("tenant-a", "graph-1", "life-1"))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/graphs/graph-1/runs/life-1/timeline")
                        .param("tenantId", "tenant-a"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.lifetimeId").value("life-1"))
                .andExpect(jsonPath("$.events.length()").value(1))
                .andExpect(jsonPath("$.events[0].eventType").value("PLAN_EXECUTION"));
    }

    @Test
    void getTimeline_ShouldReturnNotFound_WhenMissing() throws Exception {
        when(runObservabilityService.getTimeline("tenant-a", "graph-1", "missing-life"))
                .thenThrow(new NoSuchElementException("not found"));

        mockMvc.perform(get("/api/v1/graphs/graph-1/runs/missing-life/timeline")
                        .param("tenantId", "tenant-a"))
                .andExpect(status().isNotFound());
    }
}
