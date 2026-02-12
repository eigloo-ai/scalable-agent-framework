package ai.eigloo.agentic.graphbuilder.service;

import ai.eigloo.agentic.graph.model.Plan;
import ai.eigloo.agentic.graph.model.Task;
import ai.eigloo.agentic.graphbuilder.entity.DockerImageEntity;
import ai.eigloo.agentic.graphbuilder.exception.DockerBuildException;
import ai.eigloo.agentic.graphbuilder.repository.DockerImageRepository;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageCmd;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.command.RemoveImageCmd;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DockerImageService.
 */
class DockerImageServiceTest {
    
    @Mock
    private DockerImageRepository dockerImageRepository;
    
    @Mock
    private DockerClient dockerClient;
    
    @Mock
    private BuildImageCmd buildImageCmd;
    
    @Mock
    private BuildImageResultCallback buildCallback;
    
    @Mock
    private RemoveImageCmd removeImageCmd;
    
    private DockerImageService dockerImageService;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        dockerImageService = new DockerImageService(dockerImageRepository, dockerClient);
    }
    
    @Test
    void buildTaskImage_ValidTask_ShouldBuildSuccessfully() throws Exception {
        // Given
        String tenantId = "test-tenant";
        String processId = "test-process-123";
        Task task = new Task("test_task", "Test Task", tempDir.resolve("task.py"), java.util.List.of());
        
        // Create task directory with required files
        Path taskDir = tempDir.resolve("task_dir");
        Files.createDirectories(taskDir);
        Files.createFile(taskDir.resolve("task.py"));
        
        // Mock Docker client behavior
        when(dockerClient.buildImageCmd()).thenReturn(buildImageCmd);
        when(buildImageCmd.withDockerfile(any(File.class))).thenReturn(buildImageCmd);
        when(buildImageCmd.withBaseDirectory(any(File.class))).thenReturn(buildImageCmd);
        when(buildImageCmd.withTags(any(Set.class))).thenReturn(buildImageCmd);
        when(buildImageCmd.withNoCache(anyBoolean())).thenReturn(buildImageCmd);
        when(buildImageCmd.withRemove(anyBoolean())).thenReturn(buildImageCmd);
        when(buildImageCmd.exec(any(BuildImageResultCallback.class))).thenReturn(buildCallback);
        when(buildCallback.awaitImageId(anyLong(), any(TimeUnit.class))).thenReturn("test-image-id");
        
        // When
        String imageName = dockerImageService.buildTaskImage(tenantId, task, taskDir, processId);
        
        // Then
        assertThat(imageName).isEqualTo("tenant-test-tenant-task-test-task:latest");
        
        // Verify Docker client interactions
        verify(dockerClient).buildImageCmd();
        verify(buildImageCmd).withTags(argThat(tags -> tags.contains("tenant-test-tenant-task-test-task:latest")));
        verify(dockerImageRepository).save(any(DockerImageEntity.class));
    }
    
    @Test
    void buildTaskImage_WithRequirementsTxt_ShouldIncludeRequirements() throws Exception {
        // Given
        String tenantId = "test-tenant";
        String processId = "test-process-123";
        Task task = new Task("test_task", "Test Task", tempDir.resolve("task.py"), java.util.List.of());
        
        // Create task directory with requirements.txt
        Path taskDir = tempDir.resolve("task_dir");
        Files.createDirectories(taskDir);
        Files.createFile(taskDir.resolve("task.py"));
        Files.write(taskDir.resolve("requirements.txt"), "requests==2.25.1".getBytes());
        
        // Mock Docker client behavior
        when(dockerClient.buildImageCmd()).thenReturn(buildImageCmd);
        when(buildImageCmd.withDockerfile(any(File.class))).thenReturn(buildImageCmd);
        when(buildImageCmd.withBaseDirectory(any(File.class))).thenReturn(buildImageCmd);
        when(buildImageCmd.withTags(any(Set.class))).thenReturn(buildImageCmd);
        when(buildImageCmd.withNoCache(anyBoolean())).thenReturn(buildImageCmd);
        when(buildImageCmd.withRemove(anyBoolean())).thenReturn(buildImageCmd);
        when(buildImageCmd.exec(any(BuildImageResultCallback.class))).thenReturn(buildCallback);
        when(buildCallback.awaitImageId(anyLong(), any(TimeUnit.class))).thenReturn("test-image-id");
        
        // When
        String imageName = dockerImageService.buildTaskImage(tenantId, task, taskDir, processId);
        
        // Then
        assertThat(imageName).isEqualTo("tenant-test-tenant-task-test-task:latest");
        
        // Verify that the build was successful (requirements.txt would be copied internally)
        verify(dockerImageRepository).save(any(DockerImageEntity.class));
    }
    
    @Test
    void buildTaskImage_MissingTaskFile_ShouldThrowException() throws Exception {
        // Given
        String tenantId = "test-tenant";
        String processId = "test-process-123";
        Task task = new Task("test_task", "Test Task", tempDir.resolve("task.py"), java.util.List.of());
        
        // Create task directory without task.py
        Path taskDir = tempDir.resolve("task_dir");
        Files.createDirectories(taskDir);
        // Missing task.py file
        
        // When & Then
        assertThatThrownBy(() -> dockerImageService.buildTaskImage(tenantId, task, taskDir, processId))
            .isInstanceOf(DockerBuildException.class)
            .hasMessageContaining("Executor file not found");
    }
    
    @Test
    void buildTaskImage_DockerBuildFails_ShouldThrowException() throws Exception {
        // Given
        String tenantId = "test-tenant";
        String processId = "test-process-123";
        Task task = new Task("test_task", "Test Task", tempDir.resolve("task.py"), java.util.List.of());
        
        // Create task directory with required files
        Path taskDir = tempDir.resolve("task_dir");
        Files.createDirectories(taskDir);
        Files.createFile(taskDir.resolve("task.py"));
        
        // Mock Docker client to fail
        when(dockerClient.buildImageCmd()).thenReturn(buildImageCmd);
        when(buildImageCmd.withDockerfile(any(File.class))).thenReturn(buildImageCmd);
        when(buildImageCmd.withBaseDirectory(any(File.class))).thenReturn(buildImageCmd);
        when(buildImageCmd.withTags(any(Set.class))).thenReturn(buildImageCmd);
        when(buildImageCmd.withNoCache(anyBoolean())).thenReturn(buildImageCmd);
        when(buildImageCmd.withRemove(anyBoolean())).thenReturn(buildImageCmd);
        when(buildImageCmd.exec(any(BuildImageResultCallback.class))).thenReturn(buildCallback);
        when(buildCallback.awaitImageId(anyLong(), any(TimeUnit.class))).thenReturn(null); // Build failed
        
        // When & Then
        assertThatThrownBy(() -> dockerImageService.buildTaskImage(tenantId, task, taskDir, processId))
            .isInstanceOf(DockerBuildException.class)
            .hasMessageContaining("Docker build failed - no image ID returned");
    }
    
    @Test
    void buildPlanImage_ValidPlan_ShouldBuildSuccessfully() throws Exception {
        // Given
        String tenantId = "test-tenant";
        String processId = "test-process-123";
        Plan plan = new Plan("test_plan", "Test Plan", tempDir.resolve("plan.py"), java.util.List.of());
        
        // Create plan directory with required files
        Path planDir = tempDir.resolve("plan_dir");
        Files.createDirectories(planDir);
        Files.createFile(planDir.resolve("plan.py"));
        
        // Mock Docker client behavior
        when(dockerClient.buildImageCmd()).thenReturn(buildImageCmd);
        when(buildImageCmd.withDockerfile(any(File.class))).thenReturn(buildImageCmd);
        when(buildImageCmd.withBaseDirectory(any(File.class))).thenReturn(buildImageCmd);
        when(buildImageCmd.withTags(any(Set.class))).thenReturn(buildImageCmd);
        when(buildImageCmd.withNoCache(anyBoolean())).thenReturn(buildImageCmd);
        when(buildImageCmd.withRemove(anyBoolean())).thenReturn(buildImageCmd);
        when(buildImageCmd.exec(any(BuildImageResultCallback.class))).thenReturn(buildCallback);
        when(buildCallback.awaitImageId(anyLong(), any(TimeUnit.class))).thenReturn("test-image-id");
        
        // When
        String imageName = dockerImageService.buildPlanImage(tenantId, plan, planDir, processId);
        
        // Then
        assertThat(imageName).isEqualTo("tenant-test-tenant-plan-test-plan:latest");
        
        // Verify Docker client interactions
        verify(dockerClient).buildImageCmd();
        verify(buildImageCmd).withTags(argThat(tags -> tags.contains("tenant-test-tenant-plan-test-plan:latest")));
        verify(dockerImageRepository).save(any(DockerImageEntity.class));
    }
    
    @Test
    void buildPlanImage_MissingPlanFile_ShouldThrowException() throws Exception {
        // Given
        String tenantId = "test-tenant";
        String processId = "test-process-123";
        Plan plan = new Plan("test_plan", "Test Plan", tempDir.resolve("plan.py"), java.util.List.of());
        
        // Create plan directory without plan.py
        Path planDir = tempDir.resolve("plan_dir");
        Files.createDirectories(planDir);
        // Missing plan.py file
        
        // When & Then
        assertThatThrownBy(() -> dockerImageService.buildPlanImage(tenantId, plan, planDir, processId))
            .isInstanceOf(DockerBuildException.class)
            .hasMessageContaining("Executor file not found");
    }
    
    @Test
    void getBuiltImages_ExistingImages_ShouldReturnImageNames() {
        // Given
        String processId = "test-process-123";
        List<DockerImageEntity> mockImages = Arrays.asList(
            new DockerImageEntity("id1", processId, "image1:latest", "TASK", "task1"),
            new DockerImageEntity("id2", processId, "image2:latest", "PLAN", "plan1")
        );
        
        when(dockerImageRepository.findByProcessId(processId)).thenReturn(mockImages);
        
        // When
        List<String> imageNames = dockerImageService.getBuiltImages(processId);
        
        // Then
        assertThat(imageNames).containsExactly("image1:latest", "image2:latest");
    }
    
    @Test
    void getBuiltImages_NoImages_ShouldReturnEmptyList() {
        // Given
        String processId = "test-process-123";
        when(dockerImageRepository.findByProcessId(processId)).thenReturn(Arrays.asList());
        
        // When
        List<String> imageNames = dockerImageService.getBuiltImages(processId);
        
        // Then
        assertThat(imageNames).isEmpty();
    }
    
    @Test
    void cleanupDockerImages_ExistingImages_ShouldRemoveImagesAndRecords() {
        // Given
        String processId = "test-process-123";
        List<DockerImageEntity> mockImages = Arrays.asList(
            new DockerImageEntity("id1", processId, "image1:latest", "TASK", "task1"),
            new DockerImageEntity("id2", processId, "image2:latest", "PLAN", "plan1")
        );
        
        when(dockerImageRepository.findByProcessId(processId)).thenReturn(mockImages);
        when(dockerClient.removeImageCmd(anyString())).thenReturn(removeImageCmd);
        when(removeImageCmd.withForce(anyBoolean())).thenReturn(removeImageCmd);
        
        // When
        dockerImageService.cleanupDockerImages(processId);
        
        // Then
        verify(dockerClient, times(2)).removeImageCmd(anyString());
        verify(removeImageCmd, times(2)).withForce(true);
        verify(removeImageCmd, times(2)).exec();
        verify(dockerImageRepository).deleteByProcessId(processId);
    }
    
    @Test
    void cleanupDockerImages_DockerRemovalFails_ShouldContinueWithDatabaseCleanup() {
        // Given
        String processId = "test-process-123";
        List<DockerImageEntity> mockImages = Arrays.asList(
            new DockerImageEntity("id1", processId, "image1:latest", "TASK", "task1")
        );
        
        when(dockerImageRepository.findByProcessId(processId)).thenReturn(mockImages);
        when(dockerClient.removeImageCmd(anyString())).thenReturn(removeImageCmd);
        when(removeImageCmd.withForce(anyBoolean())).thenReturn(removeImageCmd);
        doThrow(new RuntimeException("Docker removal failed")).when(removeImageCmd).exec();
        
        // When
        dockerImageService.cleanupDockerImages(processId);
        
        // Then
        verify(dockerClient).removeImageCmd("image1:latest");
        verify(dockerImageRepository).deleteByProcessId(processId); // Should still clean up database
    }
    
    @Test
    void cleanupDockerImages_NoImages_ShouldNotFailSilently() {
        // Given
        String processId = "test-process-123";
        when(dockerImageRepository.findByProcessId(processId)).thenReturn(Arrays.asList());
        
        // When & Then
        assertThatCode(() -> dockerImageService.cleanupDockerImages(processId))
            .doesNotThrowAnyException();
        
        verify(dockerClient, never()).removeImageCmd(anyString());
        verify(dockerImageRepository).deleteByProcessId(processId);
    }
}
