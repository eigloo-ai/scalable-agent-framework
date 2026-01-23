package ai.eigloo.agentic.graphbuilder.controller;

import ai.eigloo.agentic.graphbuilder.enums.StepStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import ai.eigloo.agentic.graphbuilder.TestApplication;
import ai.eigloo.agentic.graphbuilder.dto.BundleUploadResponse;
import ai.eigloo.agentic.graphbuilder.dto.ProcessingStatus;
import ai.eigloo.agentic.graphbuilder.entity.GraphBundleEntity;
import ai.eigloo.agentic.graphbuilder.enums.BundleStatus;
import ai.eigloo.agentic.graphbuilder.repository.GraphBundleRepository;
import ai.eigloo.agentic.graphbuilder.service.ProcessingStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for GraphBundleController API endpoints.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = TestApplication.class)
@Testcontainers
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "graph-bundle.docker.base-image=test-base:latest",
    "graph-bundle.docker.build-timeout=30"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class GraphBundleControllerIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private GraphBundleRepository graphBundleRepository;
    
    @Autowired
    private ProcessingStatusService processingStatusService;
    
    private String baseUrl;
    
    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/graph-bundle";
    }
    
    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        // Clean up database after each test
        try {
            graphBundleRepository.deleteAll();
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }
    
    @Test
    void uploadBundle_ValidZipFile_ShouldReturnCreatedStatus() throws IOException {
        // Given
        String tenantId = "test-tenant";
        byte[] zipData = createValidTestZipFile();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("tenantId", tenantId);
        body.add("file", new ByteArrayResource(zipData) {
            @Override
            public String getFilename() {
                return "test-graph.zip";
            }
        });
        
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        
        // When
        ResponseEntity<BundleUploadResponse> response = restTemplate.postForEntity(
            baseUrl + "/upload", requestEntity, BundleUploadResponse.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getProcessId()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("UPLOADED");
        assertThat(response.getBody().getMessage()).contains("Upload successful");
        
        // Verify database record was created
        GraphBundleEntity savedBundle = graphBundleRepository.findByProcessId(response.getBody().getProcessId()).orElse(null);
        assertThat(savedBundle).isNotNull();
        assertThat(savedBundle.getTenantId()).isEqualTo(tenantId);
        assertThat(savedBundle.getFileName()).isEqualTo("test-graph.zip");
    }
    
    @Test
    void uploadBundle_InvalidFileExtension_ShouldReturnBadRequest() throws IOException {
        // Given
        String tenantId = "test-tenant";
        byte[] textData = "This is not a zip file".getBytes();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("tenantId", tenantId);
        body.add("file", new ByteArrayResource(textData) {
            @Override
            public String getFilename() {
                return "test-graph.txt";
            }
        });
        
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        
        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/upload", requestEntity, String.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("File must have one of the following extensions");
    }
    
    @Test
    void uploadBundle_MissingTenantId_ShouldReturnBadRequest() throws IOException {
        // Given
        byte[] zipData = createValidTestZipFile();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        // Missing tenantId
        body.add("file", new ByteArrayResource(zipData) {
            @Override
            public String getFilename() {
                return "test-graph.zip";
            }
        });
        
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        
        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/upload", requestEntity, String.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Tenant ID cannot be null");
    }
    
    @Test
    void uploadBundle_MissingFile_ShouldReturnBadRequest() {
        // Given
        String tenantId = "test-tenant";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("tenantId", tenantId);
        // Missing file
        
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        
        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/upload", requestEntity, String.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("File cannot be null");
    }
    
    @Test
    void uploadBundle_FileTooLarge_ShouldReturnBadRequest() throws IOException {
        // Given
        String tenantId = "test-tenant";
        byte[] largeData = new byte[150 * 1024 * 1024]; // 150MB - exceeds 100MB limit
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("tenantId", tenantId);
        body.add("file", new ByteArrayResource(largeData) {
            @Override
            public String getFilename() {
                return "large-file.zip";
            }
        });
        
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        
        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/upload", requestEntity, String.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("File size exceeds maximum allowed size");
    }
    
    @Test
    void getStatus_ExistingProcess_ShouldReturnProcessingStatus() {
        // Given
        String tenantId = "test-tenant";
        String processId = "test-process-123";
        
        // Create a bundle entity directly in the database
        GraphBundleEntity bundle = processingStatusService.createBundleEntity(tenantId, "test.zip", processId);
        processingStatusService.updateBundleStatus(processId, BundleStatus.EXTRACTING);
        processingStatusService.createProcessingStep(processId, "VALIDATION", StepStatus.COMPLETED);
        processingStatusService.createProcessingStep(processId, "EXTRACTION", StepStatus.IN_PROGRESS);
        
        // When
        ResponseEntity<ProcessingStatus> response = restTemplate.getForEntity(
            baseUrl + "/status/" + processId, ProcessingStatus.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getProcessId()).isEqualTo(processId);
        assertThat(response.getBody().getStatus()).isEqualTo(BundleStatus.EXTRACTING.getValue());
        assertThat(response.getBody().getSteps()).hasSize(2);
        assertThat(response.getBody().getSteps().get(0).getStepName()).isEqualTo("VALIDATION");
        assertThat(response.getBody().getSteps().get(1).getStepName()).isEqualTo("EXTRACTION");
    }
    
    @Test
    void getStatus_NonExistentProcess_ShouldReturnNotFound() {
        // Given
        String nonExistentProcessId = "non-existent-process";
        
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl + "/status/" + nonExistentProcessId, String.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("Process ID not found");
    }
    
    @Test
    void getStatus_CompletedProcess_ShouldReturnCompletedStatus() {
        // Given
        String tenantId = "test-tenant";
        String processId = "completed-process-123";
        
        // Create a completed bundle
        GraphBundleEntity bundle = processingStatusService.createBundleEntity(tenantId, "test.zip", processId);
        processingStatusService.createProcessingStep(processId, "VALIDATION", StepStatus.COMPLETED);
        processingStatusService.createProcessingStep(processId, "EXTRACTION", StepStatus.COMPLETED);
        processingStatusService.createProcessingStep(processId, "PARSING", StepStatus.COMPLETED);
        processingStatusService.markProcessAsCompleted(processId);
        
        // When
        ResponseEntity<ProcessingStatus> response = restTemplate.getForEntity(
            baseUrl + "/status/" + processId, ProcessingStatus.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getProcessId()).isEqualTo(processId);
        assertThat(response.getBody().getStatus()).isEqualTo(BundleStatus.COMPLETED.getValue());
        assertThat(response.getBody().getEndTime()).isNotNull();
        assertThat(response.getBody().getSteps()).hasSize(3);
    }
    
    @Test
    void getStatus_FailedProcess_ShouldReturnFailedStatusWithError() {
        // Given
        String tenantId = "test-tenant";
        String processId = "failed-process-123";
        String errorMessage = "Validation failed: Invalid DOT syntax";
        
        // Create a failed bundle
        GraphBundleEntity bundle = processingStatusService.createBundleEntity(tenantId, "test.zip", processId);
        processingStatusService.createProcessingStep(processId, "VALIDATION", StepStatus.IN_PROGRESS);
        processingStatusService.markProcessAsFailed(processId, "VALIDATION", errorMessage);
        
        // When
        ResponseEntity<ProcessingStatus> response = restTemplate.getForEntity(
            baseUrl + "/status/" + processId, ProcessingStatus.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getProcessId()).isEqualTo(processId);
        assertThat(response.getBody().getStatus()).isEqualTo(BundleStatus.FAILED.getValue());
        assertThat(response.getBody().getErrorMessage()).isEqualTo(errorMessage);
        assertThat(response.getBody().getSteps()).hasSize(1);
        assertThat(response.getBody().getSteps().get(0).getStatus()).isEqualTo(StepStatus.FAILED.getValue());
        assertThat(response.getBody().getSteps().get(0).getErrorMessage()).isEqualTo(errorMessage);
    }
    
    @Test
    void listBundles_ExistingBundles_ShouldReturnBundleList() {
        // Given
        String tenantId = "test-tenant";
        
        // Create multiple bundles for the tenant
        GraphBundleEntity bundle1 = processingStatusService.createBundleEntity(tenantId, "bundle1.zip", "process-1");
        processingStatusService.markProcessAsCompleted("process-1");
        
        GraphBundleEntity bundle2 = processingStatusService.createBundleEntity(tenantId, "bundle2.zip", "process-2");
        processingStatusService.updateBundleStatus("process-2", BundleStatus.EXTRACTING);
        
        // Create bundle for different tenant (should not be included)
        GraphBundleEntity otherBundle = processingStatusService.createBundleEntity("other-tenant", "other.zip", "process-3");
        
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl + "/list?tenantId=" + tenantId, String.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("bundle1.zip");
        assertThat(response.getBody()).contains("bundle2.zip");
        assertThat(response.getBody()).doesNotContain("other.zip"); // Should not include other tenant's bundles
    }
    
    @Test
    void listBundles_MissingTenantId_ShouldReturnBadRequest() {
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl + "/list", String.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Tenant ID is required");
    }
    
    @Test
    void listBundles_EmptyTenantId_ShouldReturnBadRequest() {
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl + "/list?tenantId=", String.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Tenant ID cannot be empty");
    }
    
    @Test
    void listBundles_NoExistingBundles_ShouldReturnEmptyList() {
        // Given
        String tenantId = "tenant-with-no-bundles";
        
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl + "/list?tenantId=" + tenantId, String.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("[]");
    }
    
    /**
     * Creates a valid test ZIP file with the expected structure.
     */
    private byte[] createValidTestZipFile() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // Add agent_graph.dot
            ZipEntry agentGraphEntry = new ZipEntry("agent_graph.dot");
            zos.putNextEntry(agentGraphEntry);
            zos.write("digraph G {\n  plan1 -> task1;\n  task1 -> plan2;\n}".getBytes());
            zos.closeEntry();
            
            // Add plan directory and file
            ZipEntry planDirEntry = new ZipEntry("plans/plan1/");
            zos.putNextEntry(planDirEntry);
            zos.closeEntry();
            
            ZipEntry planFileEntry = new ZipEntry("plans/plan1/plan.py");
            zos.putNextEntry(planFileEntry);
            zos.write("# Test plan implementation\nprint('Plan 1 executed')".getBytes());
            zos.closeEntry();
            
            ZipEntry plan2DirEntry = new ZipEntry("plans/plan2/");
            zos.putNextEntry(plan2DirEntry);
            zos.closeEntry();
            
            ZipEntry plan2FileEntry = new ZipEntry("plans/plan2/plan.py");
            zos.putNextEntry(plan2FileEntry);
            zos.write("# Test plan 2 implementation\nprint('Plan 2 executed')".getBytes());
            zos.closeEntry();
            
            // Add task directory and file
            ZipEntry taskDirEntry = new ZipEntry("tasks/task1/");
            zos.putNextEntry(taskDirEntry);
            zos.closeEntry();
            
            ZipEntry taskFileEntry = new ZipEntry("tasks/task1/task.py");
            zos.putNextEntry(taskFileEntry);
            zos.write("# Test task implementation\nprint('Task 1 executed')".getBytes());
            zos.closeEntry();
            
            // Add requirements.txt for task
            ZipEntry requirementsEntry = new ZipEntry("tasks/task1/requirements.txt");
            zos.putNextEntry(requirementsEntry);
            zos.write("requests==2.25.1\nnumpy==1.21.0".getBytes());
            zos.closeEntry();
        }
        
        return baos.toByteArray();
    }
}