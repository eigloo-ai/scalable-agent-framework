package ai.eigloo.agentic.graphbuilder.web;

import ai.eigloo.agentic.graphbuilder.TestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for Thymeleaf template rendering.
 */
@SpringBootTest(classes = TestApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ThymeleafTemplateTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void uploadPage_ShouldRenderCorrectTemplate() throws Exception {
        mockMvc.perform(get("/graph-bundle"))
            .andExpect(status().isOk())
            .andExpect(view().name("graph-bundle/upload"))
            .andExpect(model().attributeExists("pageTitle"))
            .andExpect(content().contentTypeCompatibleWith("text/html"));
    }
    
    @Test
    void uploadPage_ShouldContainRequiredFormElements() throws Exception {
        mockMvc.perform(get("/graph-bundle"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"upload-form\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"tenantId\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"file\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"upload-btn\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("accept=\".tar,.tar.gz,.zip\"")));
    }
    
    @Test
    void uploadPage_ShouldIncludeRequiredCSS() throws Exception {
        mockMvc.perform(get("/graph-bundle"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("common.css")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("graph-bundle.css")));
    }
    
    @Test
    void uploadPage_ShouldIncludeRequiredJavaScript() throws Exception {
        mockMvc.perform(get("/graph-bundle"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("common.js")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("graph-bundle.js")));
    }
    
    @Test
    void uploadPage_ShouldHaveCorrectPageTitle() throws Exception {
        mockMvc.perform(get("/graph-bundle"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("<title>Graph Bundle Upload</title>")));
    }
    
    @Test
    void uploadPage_ShouldIncludeStatusSection() throws Exception {
        mockMvc.perform(get("/graph-bundle"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"uploads-table\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("style=\"display: none;\"")));
    }
    
    @Test
    void statusPage_ShouldRenderCorrectTemplate() throws Exception {
        mockMvc.perform(get("/graph-bundle/status").param("processId", "test-process-123"))
            .andExpect(status().isOk())
            .andExpect(view().name("graph-bundle/status"))
            .andExpect(model().attributeExists("pageTitle"))
            .andExpect(model().attributeExists("processId"))
            .andExpect(content().contentTypeCompatibleWith("text/html"));
    }
    
    @Test
    void statusPage_ShouldContainRequiredElements() throws Exception {
        mockMvc.perform(get("/graph-bundle/status").param("processId", "test-process-123"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"status-section\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"processIdInput\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"status-content\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"all-processes-table\"")));
    }
    
    @Test
    void statusPage_ShouldIncludeRequiredCSS() throws Exception {
        mockMvc.perform(get("/graph-bundle/status").param("processId", "test-process-123"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("common.css")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("status-monitor.css")));
    }
    
    @Test
    void statusPage_ShouldIncludeRequiredJavaScript() throws Exception {
        mockMvc.perform(get("/graph-bundle/status").param("processId", "test-process-123"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("common.js")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("status-monitor.js")));
    }
    
    @Test
    void statusPage_WithoutProcessId_ShouldStillRender() throws Exception {
        mockMvc.perform(get("/graph-bundle/status"))
            .andExpect(status().isOk())
            .andExpect(view().name("graph-bundle/status"))
            .andExpect(model().attributeExists("pageTitle"));
    }
    
    @Test
    void statusPage_ShouldHaveCorrectPageTitle() throws Exception {
        mockMvc.perform(get("/graph-bundle/status").param("processId", "test-process-123"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("<title>Processing Status</title>")));
    }
    
    @Test
    void statusPage_ShouldIncludeErrorHandlingElements() throws Exception {
        mockMvc.perform(get("/graph-bundle/status").param("processId", "test-process-123"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"error-message\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"success-message\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"status-modal\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"modal-body\"")));
    }
    
    @Test
    void statusPage_ShouldIncludeProgressElements() throws Exception {
        mockMvc.perform(get("/graph-bundle/status").param("processId", "test-process-123"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"progress-fill\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"progress-bar\"")));
    }
    
    @Test
    void statusPage_ShouldIncludeAutoRefreshMeta() throws Exception {
        mockMvc.perform(get("/graph-bundle/status").param("processId", "test-process-123"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"auto-refresh-checkbox\"")));
    }
}