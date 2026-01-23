package ai.eigloo.agentic.graphbuilder.service;

import ai.eigloo.agentic.graphbuilder.exception.BundleProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BundleProcessingService.
 */
class BundleProcessingServiceTest {
    
    @Mock
    private MultipartFile mockFile;
    
    private BundleProcessingService bundleProcessingService;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        bundleProcessingService = new BundleProcessingService();
    }
    
    @Test
    void validateFile_ValidFile_ShouldPass() {
        // Given
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getOriginalFilename()).thenReturn("test.tar.gz");
        
        // When & Then
        assertThatCode(() -> bundleProcessingService.validateFile(mockFile))
            .doesNotThrowAnyException();
    }
    
    @Test
    void validateFile_NullFile_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> bundleProcessingService.validateFile(null))
            .isInstanceOf(BundleProcessingException.class)
            .hasMessageContaining("File cannot be null or empty");
    }
    
    @Test
    void validateFile_EmptyFile_ShouldThrowException() {
        // Given
        when(mockFile.isEmpty()).thenReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> bundleProcessingService.validateFile(mockFile))
            .isInstanceOf(BundleProcessingException.class)
            .hasMessageContaining("File cannot be null or empty");
    }
    
    @Test
    void validateFile_FileTooLarge_ShouldThrowException() {
        // Given
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(200L * 1024 * 1024); // 200MB
        when(mockFile.getOriginalFilename()).thenReturn("test.tar.gz");
        
        // When & Then
        assertThatThrownBy(() -> bundleProcessingService.validateFile(mockFile))
            .isInstanceOf(BundleProcessingException.class)
            .hasMessageContaining("File size exceeds maximum allowed size");
    }
    
    @Test
    void validateFile_NoFilename_ShouldThrowException() {
        // Given
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getOriginalFilename()).thenReturn(null);
        
        // When & Then
        assertThatThrownBy(() -> bundleProcessingService.validateFile(mockFile))
            .isInstanceOf(BundleProcessingException.class)
            .hasMessageContaining("File must have a name");
    }
    
    @Test
    void validateFile_InvalidExtension_ShouldThrowException() {
        // Given
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getOriginalFilename()).thenReturn("test.txt");
        
        // When & Then
        assertThatThrownBy(() -> bundleProcessingService.validateFile(mockFile))
            .isInstanceOf(BundleProcessingException.class)
            .hasMessageContaining("File must have one of the following extensions");
    }
    
    @Test
    void validateFile_ValidExtensions_ShouldPass() {
        // Test all valid extensions
        String[] validExtensions = {".tar", ".tar.gz", ".zip"};
        
        for (String extension : validExtensions) {
            when(mockFile.isEmpty()).thenReturn(false);
            when(mockFile.getSize()).thenReturn(1024L);
            when(mockFile.getOriginalFilename()).thenReturn("test" + extension);
            
            assertThatCode(() -> bundleProcessingService.validateFile(mockFile))
                .doesNotThrowAnyException();
        }
    }
    
    @Test
    void extractBundle_ValidZipFile_ShouldExtractSuccessfully() throws IOException {
        // Given
        String processId = "test-process-123";
        byte[] zipData = createTestZipFile();
        
        when(mockFile.getOriginalFilename()).thenReturn("test.zip");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(zipData));
        
        // When
        Path extractDir = bundleProcessingService.extractBundle(mockFile, processId);
        
        // Then
        assertThat(extractDir).exists();
        assertThat(extractDir.resolve("agent_graph.dot")).exists();
        assertThat(extractDir.resolve("plans/test_plan/plan.py")).exists();
        assertThat(extractDir.resolve("tasks/test_task/task.py")).exists();
        
        // Cleanup
        bundleProcessingService.cleanupTempDirectory(extractDir);
    }
    
    @Test
    void extractBundle_InvalidZipFile_ShouldThrowException() throws IOException {
        // Given
        String processId = "test-process-123";
        byte[] invalidData = "invalid zip data".getBytes();
        
        when(mockFile.getOriginalFilename()).thenReturn("test.zip");
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(invalidData));
        
        // When & Then
        assertThatThrownBy(() -> bundleProcessingService.extractBundle(mockFile, processId))
            .isInstanceOf(BundleProcessingException.class)
            .hasMessageContaining("Required file 'agent_graph.dot' not found");
    }
    
    @Test
    void validateExtractedStructure_ValidStructure_ShouldPass() throws IOException {
        // Given
        Path testDir = tempDir.resolve("test-structure");
        Files.createDirectories(testDir);
        Files.createFile(testDir.resolve("agent_graph.dot"));
        
        Path plansDir = testDir.resolve("plans/test_plan");
        Files.createDirectories(plansDir);
        Files.createFile(plansDir.resolve("plan.py"));
        
        Path tasksDir = testDir.resolve("tasks/test_task");
        Files.createDirectories(tasksDir);
        Files.createFile(tasksDir.resolve("task.py"));
        
        // When & Then
        assertThatCode(() -> bundleProcessingService.validateExtractedStructure(testDir))
            .doesNotThrowAnyException();
    }
    
    @Test
    void validateExtractedStructure_MissingAgentGraphDot_ShouldThrowException() throws IOException {
        // Given
        Path testDir = tempDir.resolve("test-structure");
        Files.createDirectories(testDir);
        // Missing agent_graph.dot file
        
        // When & Then
        assertThatThrownBy(() -> bundleProcessingService.validateExtractedStructure(testDir))
            .isInstanceOf(BundleProcessingException.class)
            .hasMessageContaining("Required file 'agent_graph.dot' not found");
    }
    
    @Test
    void validateExtractedStructure_PlanDirectoryMissingPlanPy_ShouldThrowException() throws IOException {
        // Given
        Path testDir = tempDir.resolve("test-structure");
        Files.createDirectories(testDir);
        Files.createFile(testDir.resolve("agent_graph.dot"));
        
        Path plansDir = testDir.resolve("plans/test_plan");
        Files.createDirectories(plansDir);
        // Missing plan.py file
        
        // When & Then
        assertThatThrownBy(() -> bundleProcessingService.validateExtractedStructure(testDir))
            .isInstanceOf(BundleProcessingException.class)
            .hasMessageContaining("must contain a plan.py file");
    }
    
    @Test
    void validateExtractedStructure_TaskDirectoryMissingTaskPy_ShouldThrowException() throws IOException {
        // Given
        Path testDir = tempDir.resolve("test-structure");
        Files.createDirectories(testDir);
        Files.createFile(testDir.resolve("agent_graph.dot"));
        
        Path tasksDir = testDir.resolve("tasks/test_task");
        Files.createDirectories(tasksDir);
        // Missing task.py file
        
        // When & Then
        assertThatThrownBy(() -> bundleProcessingService.validateExtractedStructure(testDir))
            .isInstanceOf(BundleProcessingException.class)
            .hasMessageContaining("must contain a task.py file");
    }
    
    @Test
    void cleanupTempDirectory_ExistingDirectory_ShouldDeleteSuccessfully() throws IOException {
        // Given
        Path testDir = tempDir.resolve("cleanup-test");
        Files.createDirectories(testDir);
        Files.createFile(testDir.resolve("test-file.txt"));
        Path subDir = testDir.resolve("subdir");
        Files.createDirectories(subDir);
        Files.createFile(subDir.resolve("nested-file.txt"));
        
        assertThat(testDir).exists();
        
        // When
        bundleProcessingService.cleanupTempDirectory(testDir);
        
        // Then
        assertThat(testDir).doesNotExist();
    }
    
    @Test
    void cleanupTempDirectory_NonExistentDirectory_ShouldNotThrowException() {
        // Given
        Path nonExistentDir = tempDir.resolve("non-existent");
        
        // When & Then
        assertThatCode(() -> bundleProcessingService.cleanupTempDirectory(nonExistentDir))
            .doesNotThrowAnyException();
    }
    
    @Test
    void cleanupTempDirectory_NullDirectory_ShouldNotThrowException() {
        // When & Then
        assertThatCode(() -> bundleProcessingService.cleanupTempDirectory(null))
            .doesNotThrowAnyException();
    }
    
    /**
     * Creates a test ZIP file with the expected structure.
     */
    private byte[] createTestZipFile() throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // Add agent_graph.dot
            ZipEntry agentGraphEntry = new ZipEntry("agent_graph.dot");
            zos.putNextEntry(agentGraphEntry);
            zos.write("digraph G { plan -> task }".getBytes());
            zos.closeEntry();
            
            // Add plan directory and file
            ZipEntry planDirEntry = new ZipEntry("plans/test_plan/");
            zos.putNextEntry(planDirEntry);
            zos.closeEntry();
            
            ZipEntry planFileEntry = new ZipEntry("plans/test_plan/plan.py");
            zos.putNextEntry(planFileEntry);
            zos.write("# Test plan".getBytes());
            zos.closeEntry();
            
            // Add task directory and file
            ZipEntry taskDirEntry = new ZipEntry("tasks/test_task/");
            zos.putNextEntry(taskDirEntry);
            zos.closeEntry();
            
            ZipEntry taskFileEntry = new ZipEntry("tasks/test_task/task.py");
            zos.putNextEntry(taskFileEntry);
            zos.write("# Test task".getBytes());
            zos.closeEntry();
        }
        
        return baos.toByteArray();
    }
}