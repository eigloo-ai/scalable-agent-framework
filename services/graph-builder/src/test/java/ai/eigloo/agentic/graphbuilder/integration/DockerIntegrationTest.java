package ai.eigloo.agentic.graphbuilder.integration;

import ai.eigloo.agentic.graph.model.Plan;
import ai.eigloo.agentic.graph.model.Task;
import ai.eigloo.agentic.graphbuilder.service.DockerImageService;
import ai.eigloo.agentic.graphbuilder.repository.DockerImageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for Docker functionality using TestContainers.
 * These tests require Docker to be available and are disabled by default.
 * Set DOCKER_TESTS_ENABLED=true environment variable to enable them.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:dockertestdb",
    "graph-bundle.docker.base-image=python:3.9-slim",
    "graph-bundle.docker.build-timeout=120"
})
@EnabledIfEnvironmentVariable(named = "DOCKER_TESTS_ENABLED", matches = "true")
class DockerIntegrationTest {
    
    @Container
    static GenericContainer<?> dockerDaemon = new GenericContainer<>(DockerImageName.parse("docker:dind"))
        .withPrivilegedMode(true)
        .withExposedPorts(2376);
    
    @Autowired
    private DockerImageService dockerImageService;
    
    @Autowired
    private DockerImageRepository dockerImageRepository;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        // Ensure Docker daemon is running
        assertThat(dockerDaemon.isRunning()).isTrue();
    }
    
    @Test
    void buildTaskImage_WithValidTaskFiles_ShouldBuildSuccessfully() throws Exception {
        // Given
        String tenantId = "integration-test-tenant";
        String processId = "docker-test-process-123";
        
        // Create task directory with files
        Path taskDir = tempDir.resolve("task_dir");
        Files.createDirectories(taskDir);
        
        // Create a simple Python task
        String taskContent = """
            #!/usr/bin/env python3
            import sys
            import os
            
            def main():
                print(f"Task executed for tenant: {os.getenv('TENANT_ID', 'unknown')}")
                print(f"Executor name: {os.getenv('EXECUTOR_NAME', 'unknown')}")
                return 0
            
            if __name__ == "__main__":
                sys.exit(main())
            """;
        Files.write(taskDir.resolve("task.py"), taskContent.getBytes());
        
        // Create requirements.txt
        String requirements = """
            requests==2.28.1
            """;
        Files.write(taskDir.resolve("requirements.txt"), requirements.getBytes());
        
        Task task = new Task("integration_task", "Integration Test Task", taskDir.resolve("task.py"), java.util.List.of());
        
        // When
        String imageName = dockerImageService.buildTaskImage(tenantId, task, taskDir, processId);
        
        // Then
        assertThat(imageName).isNotNull();
        assertThat(imageName).contains("tenant-integration-test-tenant-task-integration-task");
        
        // Verify image was recorded in database
        var builtImages = dockerImageService.getBuiltImages(processId);
        assertThat(builtImages).contains(imageName);
        
        // Cleanup
        dockerImageService.cleanupDockerImages(processId);
    }
    
    @Test
    void buildPlanImage_WithValidPlanFiles_ShouldBuildSuccessfully() throws Exception {
        // Given
        String tenantId = "integration-test-tenant";
        String processId = "docker-plan-test-process-123";
        
        // Create plan directory with files
        Path planDir = tempDir.resolve("plan_dir");
        Files.createDirectories(planDir);
        
        // Create a simple Python plan
        String planContent = """
            #!/usr/bin/env python3
            import sys
            import os
            
            def main():
                print(f"Plan executed for tenant: {os.getenv('TENANT_ID', 'unknown')}")
                print(f"Executor name: {os.getenv('EXECUTOR_NAME', 'unknown')}")
                print("Plan completed successfully")
                return 0
            
            if __name__ == "__main__":
                sys.exit(main())
            """;
        Files.write(planDir.resolve("plan.py"), planContent.getBytes());
        
        Plan plan = new Plan("integration_plan", "Integration Test Plan", planDir.resolve("plan.py"), java.util.List.of());
        
        // When
        String imageName = dockerImageService.buildPlanImage(tenantId, plan, planDir, processId);
        
        // Then
        assertThat(imageName).isNotNull();
        assertThat(imageName).contains("tenant-integration-test-tenant-plan-integration-plan");
        
        // Verify image was recorded in database
        var builtImages = dockerImageService.getBuiltImages(processId);
        assertThat(builtImages).contains(imageName);
        
        // Cleanup
        dockerImageService.cleanupDockerImages(processId);
    }
    
    @Test
    void buildTaskImage_WithInvalidPythonSyntax_ShouldFailGracefully() throws Exception {
        // Given
        String tenantId = "integration-test-tenant";
        String processId = "docker-invalid-test-process-123";
        
        // Create task directory with invalid Python file
        Path taskDir = tempDir.resolve("invalid_task_dir");
        Files.createDirectories(taskDir);
        
        // Create invalid Python task (syntax error)
        String invalidTaskContent = """
            #!/usr/bin/env python3
            import sys
            
            def main(
                # Missing closing parenthesis - syntax error
                print("This will cause a syntax error")
                return 0
            
            if __name__ == "__main__":
                sys.exit(main())
            """;
        Files.write(taskDir.resolve("task.py"), invalidTaskContent.getBytes());
        
        Task task = new Task("invalid_task", "Invalid Test Task", taskDir.resolve("task.py"), java.util.List.of());
        
        // When & Then
        assertThatThrownBy(() -> dockerImageService.buildTaskImage(tenantId, task, taskDir, processId))
            .hasMessageContaining("Docker build failed");
        
        // Verify no images were created
        var builtImages = dockerImageService.getBuiltImages(processId);
        assertThat(builtImages).isEmpty();
    }
    
    @Test
    void cleanupDockerImages_WithMultipleImages_ShouldRemoveAllImages() throws Exception {
        // Given
        String tenantId = "integration-test-tenant";
        String processId = "docker-cleanup-test-process-123";
        
        // Create and build multiple images
        Path taskDir = tempDir.resolve("cleanup_task_dir");
        Files.createDirectories(taskDir);
        Files.write(taskDir.resolve("task.py"), "print('Task')".getBytes());
        
        Path planDir = tempDir.resolve("cleanup_plan_dir");
        Files.createDirectories(planDir);
        Files.write(planDir.resolve("plan.py"), "print('Plan')".getBytes());
        
        Task task = new Task("cleanup_task", "Cleanup Test Task", taskDir.resolve("task.py"), java.util.List.of());
        Plan plan = new Plan("cleanup_plan", "Cleanup Test Plan", planDir.resolve("plan.py"), java.util.List.of());
        
        // Build images
        String taskImageName = dockerImageService.buildTaskImage(tenantId, task, taskDir, processId);
        String planImageName = dockerImageService.buildPlanImage(tenantId, plan, planDir, processId);
        
        // Verify images exist
        var builtImages = dockerImageService.getBuiltImages(processId);
        assertThat(builtImages).hasSize(2);
        assertThat(builtImages).contains(taskImageName, planImageName);
        
        // When
        dockerImageService.cleanupDockerImages(processId);
        
        // Then
        var remainingImages = dockerImageService.getBuiltImages(processId);
        assertThat(remainingImages).isEmpty();
        
        // Verify database records were also cleaned up
        var dbRecords = dockerImageRepository.findByProcessId(processId);
        assertThat(dbRecords).isEmpty();
    }
    
    @Test
    void buildTaskImage_WithLargeRequirementsFile_ShouldHandleGracefully() throws Exception {
        // Given
        String tenantId = "integration-test-tenant";
        String processId = "docker-large-req-test-process-123";
        
        // Create task directory with large requirements file
        Path taskDir = tempDir.resolve("large_req_task_dir");
        Files.createDirectories(taskDir);
        Files.write(taskDir.resolve("task.py"), "print('Task with many requirements')".getBytes());
        
        // Create a requirements file with many packages
        StringBuilder requirements = new StringBuilder();
        String[] packages = {
            "requests==2.28.1",
            "numpy==1.21.0",
            "pandas==1.3.3",
            "matplotlib==3.4.3",
            "scipy==1.7.1",
            "scikit-learn==0.24.2",
            "flask==2.0.1",
            "django==3.2.6",
            "fastapi==0.68.0",
            "pydantic==1.8.2"
        };
        
        for (String pkg : packages) {
            requirements.append(pkg).append("\n");
        }
        Files.write(taskDir.resolve("requirements.txt"), requirements.toString().getBytes());
        
        Task task = new Task("large_req_task", "Large Requirements Task", taskDir.resolve("task.py"), java.util.List.of());
        
        // When
        String imageName = dockerImageService.buildTaskImage(tenantId, task, taskDir, processId);
        
        // Then
        assertThat(imageName).isNotNull();
        
        // Verify image was built successfully
        var builtImages = dockerImageService.getBuiltImages(processId);
        assertThat(builtImages).contains(imageName);
        
        // Cleanup
        dockerImageService.cleanupDockerImages(processId);
    }
}
