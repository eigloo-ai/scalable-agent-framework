package com.pcallahan.agentic.graph.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskTest {
    
    @TempDir
    Path tempDir;
    
    private Path testTaskPath;
    
    @BeforeEach
    void setUp() {
        testTaskPath = tempDir.resolve("test_task");
    }
    
    @Test
    void testValidConstruction() {
        // Given & When
        Task task = Task.of("test_task", testTaskPath, "test_plan");
        
        // Then
        assertThat(task.name()).isEqualTo("test_task");
        assertThat(task.taskSource()).isEqualTo(testTaskPath);
        assertThat(task.upstreamPlanId()).isEqualTo("test_plan");
    }
    
    @Test
    void testValidationInConstructor() {
        // Given & When & Then
        assertThatThrownBy(() -> Task.of(null, testTaskPath, "test_plan"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Task name cannot be null or empty");
        
        assertThatThrownBy(() -> Task.of("", testTaskPath, "test_plan"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Task name cannot be null or empty");
        
        assertThatThrownBy(() -> Task.of("   ", testTaskPath, "test_plan"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Task name cannot be null or empty");
        
        assertThatThrownBy(() -> Task.of("test_task", null, "test_plan"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Task source cannot be null");
        
        // Null upstream plan ID is now converted to empty string (allowed for orphaned tasks)
        Task taskWithNullUpstream = Task.of("test_task", testTaskPath, null);
        assertThat(taskWithNullUpstream.upstreamPlanId()).isEqualTo("");
        
        // Empty upstream plan ID is allowed (for orphaned tasks)
        Task taskWithEmptyUpstream = Task.of("test_task", testTaskPath, "");
        assertThat(taskWithEmptyUpstream.upstreamPlanId()).isEqualTo("");
        
        assertThatThrownBy(() -> Task.of("test_task", testTaskPath, "   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Upstream plan ID cannot be null or empty");
    }
    
    @Test
    void testEqualsAndHashCode() {
        // Given
        Task task1 = Task.of("test_task", testTaskPath, "test_plan");
        Task task2 = Task.of("test_task", testTaskPath, "test_plan");
        Task task3 = Task.of("different_task", testTaskPath, "test_plan");
        Task task4 = Task.of("test_task", testTaskPath, "different_plan");
        
        // When & Then
        assertThat(task1).isEqualTo(task2);
        assertThat(task1).isNotEqualTo(task3);
        assertThat(task1).isNotEqualTo(task4);
        assertThat(task1.hashCode()).isEqualTo(task2.hashCode());
        assertThat(task1.hashCode()).isNotEqualTo(task3.hashCode());
        assertThat(task1.hashCode()).isNotEqualTo(task4.hashCode());
    }
    
    @Test
    void testWithFiles() {
        // Given
        Task task = Task.of("test_task", testTaskPath, "test_plan");
        ExecutorFile file1 = ExecutorFile.of("task.py", "def task(): pass");
        ExecutorFile file2 = ExecutorFile.of("requirements.txt", "requests==2.28.0");
        
        // When
        Task newTask = task.withFiles(java.util.List.of(file1, file2));
        
        // Then
        assertThat(newTask.name()).isEqualTo("test_task");
        assertThat(newTask.taskSource()).isEqualTo(testTaskPath);
        assertThat(newTask.upstreamPlanId()).isEqualTo("test_plan");
        assertThat(newTask.files()).containsExactly(file1, file2);
        
        // Original task should be unchanged
        assertThat(task.files()).isEmpty();
    }
    
    @Test
    void testWithFile() {
        // Given
        Task task = Task.of("test_task", testTaskPath, "test_plan");
        ExecutorFile file = ExecutorFile.of("task.py", "def task(): pass");
        
        // When
        Task newTask = task.withFile(file);
        
        // Then
        assertThat(newTask.name()).isEqualTo("test_task");
        assertThat(newTask.taskSource()).isEqualTo(testTaskPath);
        assertThat(newTask.upstreamPlanId()).isEqualTo("test_plan");
        assertThat(newTask.files()).containsExactly(file);
        
        // Original task should be unchanged
        assertThat(task.files()).isEmpty();
    }
    
    @Test
    void testFileImmutability() {
        // Given
        ExecutorFile file = ExecutorFile.of("task.py", "def task(): pass");
        Task task = Task.of("test_task", testTaskPath, "test_plan").withFile(file);
        
        // When & Then
        assertThatThrownBy(() -> task.files().add(ExecutorFile.of("test.py", "test")))
            .isInstanceOf(UnsupportedOperationException.class);
    }
    
    @Test
    void testNullFiles() {
        // Given & When
        Task task = new Task("test_task", "test_task", testTaskPath, "test_plan", null);
        
        // Then
        assertThat(task.files()).isEmpty();
    }
} 