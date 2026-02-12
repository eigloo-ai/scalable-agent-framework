package ai.eigloo.agentic.graph.model;

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
        Task task = Task.of("test_task", testTaskPath);

        assertThat(task.name()).isEqualTo("test_task");
        assertThat(task.taskSource()).isEqualTo(testTaskPath);
        assertThat(task.files()).isEmpty();
    }

    @Test
    void testValidationInConstructor() {
        assertThatThrownBy(() -> Task.of(null, testTaskPath))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Task name cannot be null or empty");

        assertThatThrownBy(() -> Task.of("", testTaskPath))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Task name cannot be null or empty");

        assertThatThrownBy(() -> Task.of("   ", testTaskPath))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Task name cannot be null or empty");

        assertThatThrownBy(() -> Task.of("test_task", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Task source cannot be null");
    }

    @Test
    void testWithFiles() {
        Task task = Task.of("test_task", testTaskPath);
        ExecutorFile file1 = ExecutorFile.of("task.py", "def task(): pass");
        ExecutorFile file2 = ExecutorFile.of("requirements.txt", "requests==2.28.0");

        Task newTask = task.withFiles(java.util.List.of(file1, file2));

        assertThat(newTask.name()).isEqualTo("test_task");
        assertThat(newTask.taskSource()).isEqualTo(testTaskPath);
        assertThat(newTask.files()).containsExactly(file1, file2);
        assertThat(task.files()).isEmpty();
    }

    @Test
    void testWithFile() {
        Task task = Task.of("test_task", testTaskPath);
        ExecutorFile file = ExecutorFile.of("task.py", "def task(): pass");

        Task newTask = task.withFile(file);

        assertThat(newTask.name()).isEqualTo("test_task");
        assertThat(newTask.taskSource()).isEqualTo(testTaskPath);
        assertThat(newTask.files()).containsExactly(file);
        assertThat(task.files()).isEmpty();
    }

    @Test
    void testFileImmutability() {
        ExecutorFile file = ExecutorFile.of("task.py", "def task(): pass");
        Task task = Task.of("test_task", testTaskPath).withFile(file);

        assertThatThrownBy(() -> task.files().add(ExecutorFile.of("test.py", "test")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testNullFiles() {
        Task task = new Task("test_task", "test_task", testTaskPath, null);

        assertThat(task.files()).isEmpty();
    }
}
