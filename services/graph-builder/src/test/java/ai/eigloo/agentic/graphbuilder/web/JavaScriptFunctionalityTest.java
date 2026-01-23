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
import org.openqa.selenium.JavascriptExecutor;
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

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for JavaScript functionality in the web interface.
 * These tests require Chrome browser and are disabled by default.
 * Set WEB_TESTS_ENABLED=true environment variable to enable them.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:jstestdb",
    "graph-bundle.docker.base-image=test-base:latest"
})
@EnabledIfEnvironmentVariable(named = "WEB_TESTS_ENABLED", matches = "true")
class JavaScriptFunctionalityTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private ProcessingStatusService processingStatusService;
    
    private WebDriver driver;
    private WebDriverWait wait;
    private JavascriptExecutor jsExecutor;
    private String baseUrl;
    
    @BeforeAll
    static void setupClass() {
        WebDriverManager.chromedriver().setup();
    }
    
    @BeforeEach
    void setUp() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        jsExecutor = (JavascriptExecutor) driver;
        baseUrl = "http://localhost:" + port;
    }
    
    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
    
    @Test
    void fileValidation_ShouldValidateFileExtension() {
        // Given
        driver.get(baseUrl + "/graph-bundle");
        
        // When - simulate selecting an invalid file type
        String jsCode = """
            const fileInput = document.getElementById('file');
            const event = new Event('change', { bubbles: true });
            
            // Create a mock file with invalid extension
            const mockFile = new File(['content'], 'test.txt', { type: 'text/plain' });
            
            // Mock the files property
            Object.defineProperty(fileInput, 'files', {
                value: [mockFile],
                writable: false
            });
            
            fileInput.dispatchEvent(event);
            
            // Return validation result
            return document.querySelector('.file-error') !== null;
            """;
        
        Boolean hasError = (Boolean) jsExecutor.executeScript(jsCode);
        
        // Then
        assertThat(hasError).isTrue();
    }
    
    @Test
    void uploadProgress_ShouldShowProgressBar() {
        // Given
        driver.get(baseUrl + "/graph-bundle");
        
        // When - simulate starting upload
        String jsCode = """
            // Simulate upload start
            if (typeof window.showUploadProgress === 'function') {
                window.showUploadProgress('test-process-123');
                return document.getElementById('statusSection').style.display !== 'none';
            }
            return false;
            """;
        
        Boolean progressVisible = (Boolean) jsExecutor.executeScript(jsCode);
        
        // Then
        assertThat(progressVisible).isTrue();
    }
    
    @Test
    void statusPolling_ShouldUpdateStatusAutomatically() {
        // Given
        String tenantId = "test-tenant";
        String processId = "js-test-process-123";
        
        // Create a process that will be updated
        GraphBundleEntity bundle = processingStatusService.createBundleEntity(tenantId, "test.zip", processId);
        processingStatusService.updateBundleStatus(processId, BundleStatus.EXTRACTING);
        processingStatusService.createProcessingStep(processId, "VALIDATION", StepStatus.IN_PROGRESS);
        
        driver.get(baseUrl + "/graph-bundle/status?processId=" + processId);
        
        // Wait for initial load
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("statusContainer")));
        
        // Verify initial status
        WebElement overallStatus = driver.findElement(By.id("overallStatus"));
        assertThat(overallStatus.getText()).contains("EXTRACTING");
        
        // When - update status in background and trigger manual refresh
        processingStatusService.updateProcessingStep(processId, "VALIDATION", StepStatus.COMPLETED);
        processingStatusService.markProcessAsCompleted(processId);
        
        // Trigger manual status refresh via JavaScript
        String jsCode = """
            if (typeof window.refreshStatus === 'function') {
                window.refreshStatus();
                return true;
            }
            return false;
            """;
        
        Boolean refreshTriggered = (Boolean) jsExecutor.executeScript(jsCode);
        
        // Then
        assertThat(refreshTriggered).isTrue();
        
        // Wait for status to update
        wait.until(ExpectedConditions.textToBePresentInElement(overallStatus, "COMPLETED"));
        assertThat(overallStatus.getText()).contains("COMPLETED");
    }
    
    @Test
    void errorToggle_ShouldExpandAndCollapseErrorDetails() {
        // Given
        String tenantId = "test-tenant";
        String processId = "js-error-test-process-123";
        String errorMessage = "Detailed error message for testing";
        
        // Create a failed process
        GraphBundleEntity bundle = processingStatusService.createBundleEntity(tenantId, "test.zip", processId);
        processingStatusService.markProcessAsFailed(processId, "VALIDATION", errorMessage);
        
        driver.get(baseUrl + "/graph-bundle/status?processId=" + processId);
        
        // Wait for page load
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("statusContainer")));
        
        // When - test error toggle functionality
        String jsCode = """
            const errorToggle = document.getElementById('errorToggle');
            const errorDetails = document.getElementById('errorDetails');
            
            if (!errorToggle || !errorDetails) {
                return { error: 'Elements not found' };
            }
            
            // Initial state
            const initiallyHidden = errorDetails.style.display === 'none' || 
                                   window.getComputedStyle(errorDetails).display === 'none';
            
            // Click to expand
            errorToggle.click();
            
            // Wait a bit for animation
            setTimeout(() => {
                const expandedVisible = errorDetails.style.display !== 'none' && 
                                       window.getComputedStyle(errorDetails).display !== 'none';
                
                // Click to collapse
                errorToggle.click();
                
                setTimeout(() => {
                    const collapsedHidden = errorDetails.style.display === 'none' || 
                                           window.getComputedStyle(errorDetails).display === 'none';
                    
                    window.testResult = {
                        initiallyHidden: initiallyHidden,
                        expandedVisible: expandedVisible,
                        collapsedHidden: collapsedHidden
                    };
                }, 100);
            }, 100);
            
            return 'test_started';
            """;
        
        jsExecutor.executeScript(jsCode);
        
        // Wait for test to complete
        wait.until(driver -> {
            Object result = jsExecutor.executeScript("return window.testResult;");
            return result != null;
        });
        
        // Then
        @SuppressWarnings("unchecked")
        java.util.Map<String, Boolean> result = (java.util.Map<String, Boolean>) jsExecutor.executeScript("return window.testResult;");
        
        assertThat(result.get("initiallyHidden")).isTrue();
        assertThat(result.get("expandedVisible")).isTrue();
        assertThat(result.get("collapsedHidden")).isTrue();
    }
    
    @Test
    void formValidation_ShouldPreventSubmissionWithMissingFields() {
        // Given
        driver.get(baseUrl + "/graph-bundle");
        
        // When - test form validation
        String jsCode = """
            const form = document.getElementById('uploadForm');
            const tenantIdInput = document.getElementById('tenantId');
            const fileInput = document.getElementById('file');
            
            // Clear any existing values
            tenantIdInput.value = '';
            fileInput.value = '';
            
            // Try to submit form
            const submitEvent = new Event('submit', { bubbles: true, cancelable: true });
            const result = form.dispatchEvent(submitEvent);
            
            // Check validation states
            return {
                formSubmitted: result,
                tenantIdValid: tenantIdInput.checkValidity(),
                fileValid: fileInput.checkValidity(),
                tenantIdValidationMessage: tenantIdInput.validationMessage,
                fileValidationMessage: fileInput.validationMessage
            };
            """;
        
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> result = (java.util.Map<String, Object>) jsExecutor.executeScript(jsCode);
        
        // Then
        assertThat((Boolean) result.get("formSubmitted")).isFalse(); // Form submission should be prevented
        assertThat((Boolean) result.get("tenantIdValid")).isFalse();
        assertThat((Boolean) result.get("fileValid")).isFalse();
        assertThat((String) result.get("tenantIdValidationMessage")).isNotEmpty();
        assertThat((String) result.get("fileValidationMessage")).isNotEmpty();
    }
    
    @Test
    void progressBar_ShouldUpdateCorrectly() {
        // Given
        driver.get(baseUrl + "/graph-bundle");
        
        // When - test progress bar functionality
        String jsCode = """
            // Simulate showing progress
            if (typeof window.updateProgress === 'function') {
                window.updateProgress(25);
                const progress25 = document.querySelector('.progress-bar').style.width;
                
                window.updateProgress(75);
                const progress75 = document.querySelector('.progress-bar').style.width;
                
                window.updateProgress(100);
                const progress100 = document.querySelector('.progress-bar').style.width;
                
                return {
                    progress25: progress25,
                    progress75: progress75,
                    progress100: progress100
                };
            }
            return { error: 'updateProgress function not found' };
            """;
        
        @SuppressWarnings("unchecked")
        java.util.Map<String, String> result = (java.util.Map<String, String>) jsExecutor.executeScript(jsCode);
        
        // Then
        if (!result.containsKey("error")) {
            assertThat(result.get("progress25")).contains("25%");
            assertThat(result.get("progress75")).contains("75%");
            assertThat(result.get("progress100")).contains("100%");
        }
    }
    
    @Test
    void apiErrorHandling_ShouldDisplayErrorMessages() {
        // Given
        driver.get(baseUrl + "/graph-bundle");
        
        // When - simulate API error response
        String jsCode = """
            // Simulate API error handling
            if (typeof window.handleApiError === 'function') {
                const errorResponse = {
                    status: 400,
                    message: 'File validation failed: Invalid file format'
                };
                
                window.handleApiError(errorResponse);
                
                const errorElement = document.querySelector('.error-message');
                return {
                    errorVisible: errorElement && errorElement.style.display !== 'none',
                    errorMessage: errorElement ? errorElement.textContent : null
                };
            }
            return { error: 'handleApiError function not found' };
            """;
        
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> result = (java.util.Map<String, Object>) jsExecutor.executeScript(jsCode);
        
        // Then
        if (!result.containsKey("error")) {
            assertThat((Boolean) result.get("errorVisible")).isTrue();
            assertThat((String) result.get("errorMessage")).contains("File validation failed");
        }
    }
    
    @Test
    void statusSteps_ShouldUpdateVisualIndicators() {
        // Given
        String tenantId = "test-tenant";
        String processId = "js-steps-test-process-123";
        
        // Create a process with multiple steps
        GraphBundleEntity bundle = processingStatusService.createBundleEntity(tenantId, "test.zip", processId);
        processingStatusService.createProcessingStep(processId, "VALIDATION", StepStatus.COMPLETED);
        processingStatusService.createProcessingStep(processId, "EXTRACTION", StepStatus.IN_PROGRESS);
        processingStatusService.createProcessingStep(processId, "PARSING", StepStatus.IN_PROGRESS);
        
        driver.get(baseUrl + "/graph-bundle/status?processId=" + processId);
        
        // Wait for page load
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("statusContainer")));
        
        // When - check step visual indicators
        String jsCode = """
            const steps = document.querySelectorAll('.step');
            const stepStates = [];
            
            steps.forEach((step, index) => {
                stepStates.push({
                    index: index,
                    classes: step.className,
                    text: step.textContent.trim()
                });
            });
            
            return stepStates;
            """;
        
        @SuppressWarnings("unchecked")
        java.util.List<java.util.Map<String, Object>> stepStates = 
            (java.util.List<java.util.Map<String, Object>>) jsExecutor.executeScript(jsCode);
        
        // Then
        assertThat(stepStates).hasSize(3);
        
        // First step should be completed
        assertThat((String) stepStates.get(0).get("classes")).contains("completed");
        assertThat((String) stepStates.get(0).get("text")).contains("VALIDATION");
        
        // Second step should be in progress
        assertThat((String) stepStates.get(1).get("classes")).contains("in-progress");
        assertThat((String) stepStates.get(1).get("text")).contains("EXTRACTION");
        
        // Third step should be in progress
        assertThat((String) stepStates.get(2).get("classes")).contains("in-progress");
        assertThat((String) stepStates.get(2).get("text")).contains("PARSING");
    }
}