package ai.eigloo.agentic.graphbuilder.web;

import ai.eigloo.agentic.graphbuilder.entity.GraphBundleEntity;
import ai.eigloo.agentic.graphbuilder.enums.StepStatus;
import ai.eigloo.agentic.graphbuilder.enums.BundleStatus;
import ai.eigloo.agentic.graphbuilder.service.ProcessingStatusService;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.*;

/**
 * Web interface tests for the graph bundle upload functionality.
 * These tests require Chrome browser and are disabled by default.
 * Set WEB_TESTS_ENABLED=true environment variable to enable them.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:webtestdb",
    "graph-bundle.docker.base-image=test-base:latest"
})
@EnabledIfEnvironmentVariable(named = "WEB_TESTS_ENABLED", matches = "true")
class GraphBundleWebInterfaceTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private ProcessingStatusService processingStatusService;
    
    private WebDriver driver;
    private WebDriverWait wait;
    private String baseUrl;
    
    @BeforeAll
    static void setupClass() {
        WebDriverManager.chromedriver().setup();
    }
    
    @BeforeEach
    void setUp() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless"); // Run in headless mode for CI
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        baseUrl = "http://localhost:" + port;
    }
    
    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
    
    @Test
    void uploadPage_ShouldRenderCorrectly() {
        // When
        driver.get(baseUrl + "/graph-bundle");
        
        // Then
        assertThat(driver.getTitle()).contains("Graph Bundle Upload");
        
        // Verify form elements are present
        WebElement uploadForm = driver.findElement(By.id("uploadForm"));
        assertThat(uploadForm).isNotNull();
        
        WebElement tenantIdInput = driver.findElement(By.id("tenantId"));
        assertThat(tenantIdInput).isNotNull();
        assertThat(tenantIdInput.getAttribute("required")).isNotNull();
        
        WebElement fileInput = driver.findElement(By.id("file"));
        assertThat(fileInput).isNotNull();
        assertThat(fileInput.getAttribute("accept")).contains(".zip,.tar,.tar.gz");
        
        WebElement uploadButton = driver.findElement(By.id("uploadButton"));
        assertThat(uploadButton).isNotNull();
        assertThat(uploadButton.getText()).contains("Upload");
        
        // Verify status section is present but hidden initially
        WebElement statusSection = driver.findElement(By.id("statusSection"));
        assertThat(statusSection).isNotNull();
        assertThat(statusSection.getCssValue("display")).isEqualTo("none");
    }
    
    @Test
    void uploadForm_WithValidInputs_ShouldShowUploadProgress() throws IOException {
        // Given
        driver.get(baseUrl + "/graph-bundle");
        
        // Create a test file
        Path testFile = createTestZipFile();
        
        // When
        WebElement tenantIdInput = driver.findElement(By.id("tenantId"));
        tenantIdInput.sendKeys("test-tenant");
        
        WebElement fileInput = driver.findElement(By.id("file"));
        fileInput.sendKeys(testFile.toAbsolutePath().toString());
        
        WebElement uploadButton = driver.findElement(By.id("uploadButton"));
        uploadButton.click();
        
        // Then
        // Wait for upload to start
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("statusSection")));
        
        WebElement statusSection = driver.findElement(By.id("statusSection"));
        assertThat(statusSection.isDisplayed()).isTrue();
        
        // Verify progress indicator appears
        WebElement progressBar = driver.findElement(By.id("progressBar"));
        assertThat(progressBar).isNotNull();
        
        // Verify process ID is displayed
        WebElement processIdElement = driver.findElement(By.id("processId"));
        assertThat(processIdElement.getText()).isNotEmpty();
        
        // Cleanup
        Files.deleteIfExists(testFile);
    }
    
    @Test
    void uploadForm_WithInvalidFile_ShouldShowError() throws IOException {
        // Given
        driver.get(baseUrl + "/graph-bundle");
        
        // Create an invalid file (text file instead of zip)
        Path invalidFile = Files.createTempFile("invalid", ".txt");
        Files.write(invalidFile, "This is not a zip file".getBytes());
        
        // When
        WebElement tenantIdInput = driver.findElement(By.id("tenantId"));
        tenantIdInput.sendKeys("test-tenant");
        
        WebElement fileInput = driver.findElement(By.id("file"));
        fileInput.sendKeys(invalidFile.toAbsolutePath().toString());
        
        WebElement uploadButton = driver.findElement(By.id("uploadButton"));
        uploadButton.click();
        
        // Then
        // Wait for error message
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("error-message")));
        
        WebElement errorMessage = driver.findElement(By.className("error-message"));
        assertThat(errorMessage.isDisplayed()).isTrue();
        assertThat(errorMessage.getText()).contains("File must have one of the following extensions");
        
        // Cleanup
        Files.deleteIfExists(invalidFile);
    }
    
    @Test
    void uploadForm_WithMissingTenantId_ShouldShowValidationError() {
        // Given
        driver.get(baseUrl + "/graph-bundle");
        
        // When - try to submit without tenant ID
        WebElement uploadButton = driver.findElement(By.id("uploadButton"));
        uploadButton.click();
        
        // Then
        // HTML5 validation should prevent submission
        WebElement tenantIdInput = driver.findElement(By.id("tenantId"));
        String validationMessage = tenantIdInput.getAttribute("validationMessage");
        assertThat(validationMessage).isNotEmpty();
    }
    
    @Test
    void uploadForm_WithMissingFile_ShouldShowValidationError() {
        // Given
        driver.get(baseUrl + "/graph-bundle");
        
        // When - try to submit without file
        WebElement tenantIdInput = driver.findElement(By.id("tenantId"));
        tenantIdInput.sendKeys("test-tenant");
        
        WebElement uploadButton = driver.findElement(By.id("uploadButton"));
        uploadButton.click();
        
        // Then
        // HTML5 validation should prevent submission
        WebElement fileInput = driver.findElement(By.id("file"));
        String validationMessage = fileInput.getAttribute("validationMessage");
        assertThat(validationMessage).isNotEmpty();
    }
    
    @Test
    void statusMonitoring_WithCompletedProcess_ShouldShowSuccess() {
        // Given
        String tenantId = "test-tenant";
        String processId = "web-test-process-123";
        
        // Create a completed process in the database
        GraphBundleEntity bundle = processingStatusService.createBundleEntity(tenantId, "test.zip", processId);
        processingStatusService.createProcessingStep(processId, "VALIDATION", StepStatus.COMPLETED);
        processingStatusService.createProcessingStep(processId, "EXTRACTION", StepStatus.COMPLETED);
        processingStatusService.createProcessingStep(processId, "PARSING", StepStatus.COMPLETED);
        processingStatusService.markProcessAsCompleted(processId);
        
        // When
        driver.get(baseUrl + "/graph-bundle/status?processId=" + processId);
        
        // Then
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("statusContainer")));
        
        WebElement statusContainer = driver.findElement(By.id("statusContainer"));
        assertThat(statusContainer.isDisplayed()).isTrue();
        
        // Verify process ID is displayed
        WebElement processIdElement = driver.findElement(By.id("processId"));
        assertThat(processIdElement.getText()).contains(processId);
        
        // Verify overall status shows completed
        WebElement overallStatus = driver.findElement(By.id("overallStatus"));
        assertThat(overallStatus.getText()).contains("COMPLETED");
        assertThat(overallStatus.getAttribute("class")).contains("success");
        
        // Verify all steps show as completed
        WebElement stepsContainer = driver.findElement(By.id("stepsContainer"));
        var stepElements = stepsContainer.findElements(By.className("step"));
        assertThat(stepElements).hasSize(3);
        
        for (WebElement step : stepElements) {
            assertThat(step.getAttribute("class")).contains("completed");
        }
    }
    
    @Test
    void statusMonitoring_WithFailedProcess_ShouldShowError() {
        // Given
        String tenantId = "test-tenant";
        String processId = "web-failed-test-process-123";
        String errorMessage = "Validation failed: Invalid DOT syntax";
        
        // Create a failed process in the database
        GraphBundleEntity bundle = processingStatusService.createBundleEntity(tenantId, "test.zip", processId);
        processingStatusService.createProcessingStep(processId, "VALIDATION", StepStatus.IN_PROGRESS);
        processingStatusService.markProcessAsFailed(processId, "VALIDATION", errorMessage);
        
        // When
        driver.get(baseUrl + "/graph-bundle/status?processId=" + processId);
        
        // Then
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("statusContainer")));
        
        // Verify overall status shows failed
        WebElement overallStatus = driver.findElement(By.id("overallStatus"));
        assertThat(overallStatus.getText()).contains("FAILED");
        assertThat(overallStatus.getAttribute("class")).contains("error");
        
        // Verify error message is displayed
        WebElement errorMessageElement = driver.findElement(By.id("errorMessage"));
        assertThat(errorMessageElement.isDisplayed()).isTrue();
        assertThat(errorMessageElement.getText()).contains(errorMessage);
        
        // Verify failed step is highlighted
        WebElement stepsContainer = driver.findElement(By.id("stepsContainer"));
        var stepElements = stepsContainer.findElements(By.className("step"));
        assertThat(stepElements).hasSize(1);
        
        WebElement failedStep = stepElements.get(0);
        assertThat(failedStep.getAttribute("class")).contains("failed");
        assertThat(failedStep.getText()).contains("VALIDATION");
    }
    
    @Test
    void statusMonitoring_WithInProgressProcess_ShouldShowProgress() {
        // Given
        String tenantId = "test-tenant";
        String processId = "web-progress-test-process-123";
        
        // Create a process in progress
        GraphBundleEntity bundle = processingStatusService.createBundleEntity(tenantId, "test.zip", processId);
        processingStatusService.updateBundleStatus(processId, BundleStatus.EXTRACTING);
        processingStatusService.createProcessingStep(processId, "VALIDATION", StepStatus.COMPLETED);
        processingStatusService.createProcessingStep(processId, "EXTRACTION", StepStatus.IN_PROGRESS);
        
        // When
        driver.get(baseUrl + "/graph-bundle/status?processId=" + processId);
        
        // Then
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("statusContainer")));
        
        // Verify overall status shows extracting
        WebElement overallStatus = driver.findElement(By.id("overallStatus"));
        assertThat(overallStatus.getText()).contains("EXTRACTING");
        assertThat(overallStatus.getAttribute("class")).contains("extracting");
        
        // Verify steps show correct status
        WebElement stepsContainer = driver.findElement(By.id("stepsContainer"));
        var stepElements = stepsContainer.findElements(By.className("step"));
        assertThat(stepElements).hasSize(2);
        
        // First step should be completed
        WebElement completedStep = stepElements.get(0);
        assertThat(completedStep.getAttribute("class")).contains("completed");
        assertThat(completedStep.getText()).contains("VALIDATION");
        
        // Second step should be in progress
        WebElement inProgressStep = stepElements.get(1);
        assertThat(inProgressStep.getAttribute("class")).contains("in-progress");
        assertThat(inProgressStep.getText()).contains("EXTRACTION");
    }
    
    @Test
    void statusMonitoring_WithNonExistentProcess_ShouldShowNotFound() {
        // Given
        String nonExistentProcessId = "non-existent-process-123";
        
        // When
        driver.get(baseUrl + "/graph-bundle/status?processId=" + nonExistentProcessId);
        
        // Then
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("errorContainer")));
        
        WebElement errorContainer = driver.findElement(By.id("errorContainer"));
        assertThat(errorContainer.isDisplayed()).isTrue();
        assertThat(errorContainer.getText()).contains("Process not found");
    }
    
    @Test
    void statusPage_AutoRefresh_ShouldUpdateStatus() {
        // Given
        String tenantId = "test-tenant";
        String processId = "web-refresh-test-process-123";
        
        // Create a process that will be updated
        GraphBundleEntity bundle = processingStatusService.createBundleEntity(tenantId, "test.zip", processId);
        processingStatusService.updateBundleStatus(processId, BundleStatus.EXTRACTING);
        processingStatusService.createProcessingStep(processId, "VALIDATION", StepStatus.IN_PROGRESS);
        
        // When
        driver.get(baseUrl + "/graph-bundle/status?processId=" + processId);
        
        // Wait for initial load
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("statusContainer")));
        
        // Verify initial status
        WebElement overallStatus = driver.findElement(By.id("overallStatus"));
        assertThat(overallStatus.getText()).contains("EXTRACTING");
        
        // Update the process status in the background
        processingStatusService.updateProcessingStep(processId, "VALIDATION", StepStatus.COMPLETED);
        processingStatusService.markProcessAsCompleted(processId);
        
        // Then
        // Wait for auto-refresh to update the status (assuming 5-second refresh interval)
        wait.until(ExpectedConditions.textToBePresentInElement(overallStatus, "COMPLETED"));
        
        assertThat(overallStatus.getText()).contains("COMPLETED");
        assertThat(overallStatus.getAttribute("class")).contains("success");
    }
    
    @Test
    void errorDetails_ShouldBeExpandable() {
        // Given
        String tenantId = "test-tenant";
        String processId = "web-error-details-test-process-123";
        String errorMessage = "Detailed error message with stack trace information";
        
        // Create a failed process with detailed error
        GraphBundleEntity bundle = processingStatusService.createBundleEntity(tenantId, "test.zip", processId);
        processingStatusService.markProcessAsFailed(processId, "PARSING", errorMessage);
        
        // When
        driver.get(baseUrl + "/graph-bundle/status?processId=" + processId);
        
        // Then
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("statusContainer")));
        
        // Find error details toggle
        WebElement errorToggle = driver.findElement(By.id("errorToggle"));
        assertThat(errorToggle.isDisplayed()).isTrue();
        
        // Initially, detailed error should be hidden
        WebElement errorDetails = driver.findElement(By.id("errorDetails"));
        assertThat(errorDetails.getCssValue("display")).isEqualTo("none");
        
        // Click to expand error details
        errorToggle.click();
        
        // Wait for error details to become visible
        wait.until(ExpectedConditions.visibilityOf(errorDetails));
        
        assertThat(errorDetails.isDisplayed()).isTrue();
        assertThat(errorDetails.getText()).contains(errorMessage);
        
        // Click again to collapse
        errorToggle.click();
        
        // Wait for error details to be hidden
        wait.until(ExpectedConditions.invisibilityOf(errorDetails));
        
        assertThat(errorDetails.getCssValue("display")).isEqualTo("none");
    }
    
    /**
     * Creates a test ZIP file for upload testing.
     */
    private Path createTestZipFile() throws IOException {
        Path tempFile = Files.createTempFile("test-graph", ".zip");
        
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(tempFile))) {
            // Add agent_graph.dot
            ZipEntry agentGraphEntry = new ZipEntry("agent_graph.dot");
            zos.putNextEntry(agentGraphEntry);
            zos.write("digraph G { plan1 -> task1; }".getBytes());
            zos.closeEntry();
            
            // Add plan
            ZipEntry planDirEntry = new ZipEntry("plans/plan1/");
            zos.putNextEntry(planDirEntry);
            zos.closeEntry();
            
            ZipEntry planFileEntry = new ZipEntry("plans/plan1/plan.py");
            zos.putNextEntry(planFileEntry);
            zos.write("print('Plan executed')".getBytes());
            zos.closeEntry();
            
            // Add task
            ZipEntry taskDirEntry = new ZipEntry("tasks/task1/");
            zos.putNextEntry(taskDirEntry);
            zos.closeEntry();
            
            ZipEntry taskFileEntry = new ZipEntry("tasks/task1/task.py");
            zos.putNextEntry(taskFileEntry);
            zos.write("print('Task executed')".getBytes());
            zos.closeEntry();
        }
        
        return tempFile;
    }
}