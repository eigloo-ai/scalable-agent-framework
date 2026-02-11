package ai.eigloo.agentic.graph.repository;

import ai.eigloo.agentic.graph.entity.AgentGraphEntity;
import ai.eigloo.agentic.graph.entity.ExecutorFileEntity;
import ai.eigloo.agentic.graph.entity.PlanEntity;
import ai.eigloo.agentic.graph.entity.TaskEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for ExecutorFileRepository using TestContainers.
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ExecutorFileRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("test_executor_files")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
    }



    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ExecutorFileRepository executorFileRepository;

    private AgentGraphEntity testGraph;
    private PlanEntity testPlan;
    private TaskEntity testTask;

    @BeforeEach
    void setUp() {
        // Create test graph
        testGraph = new AgentGraphEntity(
                UUID.randomUUID().toString(),
                "test-tenant",
                "test-graph"
        );
        entityManager.persistAndFlush(testGraph);

        // Create test plan
        testPlan = new PlanEntity(
                UUID.randomUUID().toString(),
                "test-plan",
                "Test Plan",
                "/plans/test-plan",
                testGraph
        );
        entityManager.persistAndFlush(testPlan);

        // Create test task
        testTask = new TaskEntity(
                UUID.randomUUID().toString(),
                "test-task",
                "Test Task",
                "/tasks/test-task",
                testGraph,
                testPlan
        );
        entityManager.persistAndFlush(testTask);
    }

    @Test
    void shouldSaveAndFindExecutorFileForPlan() {
        // Given
        ExecutorFileEntity file = new ExecutorFileEntity(
                UUID.randomUUID().toString(),
                "plan.py",
                "def plan(input): return result",
                "1.0",
                testPlan
        );

        // When
        ExecutorFileEntity saved = executorFileRepository.save(file);
        entityManager.flush();

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("plan.py");
        assertThat(saved.getContents()).isEqualTo("def plan(input): return result");
        assertThat(saved.getVersion()).isEqualTo("1.0");
        assertThat(saved.getPlan()).isEqualTo(testPlan);
        assertThat(saved.getTask()).isNull();
        assertThat(saved.getCreationDate()).isNotNull();
        assertThat(saved.getUpdateDate()).isNotNull();
    }

    @Test
    void shouldSaveAndFindExecutorFileForTask() {
        // Given
        ExecutorFileEntity file = new ExecutorFileEntity(
                UUID.randomUUID().toString(),
                "task.py",
                "def task(input): return result",
                "1.0",
                testTask
        );

        // When
        ExecutorFileEntity saved = executorFileRepository.save(file);
        entityManager.flush();

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("task.py");
        assertThat(saved.getContents()).isEqualTo("def task(input): return result");
        assertThat(saved.getVersion()).isEqualTo("1.0");
        assertThat(saved.getTask()).isEqualTo(testTask);
        assertThat(saved.getPlan()).isNull();
        assertThat(saved.getCreationDate()).isNotNull();
        assertThat(saved.getUpdateDate()).isNotNull();
    }

    @Test
    void shouldFindFilesByPlanId() {
        // Given
        ExecutorFileEntity planFile = new ExecutorFileEntity(
                UUID.randomUUID().toString(),
                "plan.py",
                "def plan(input): return result",
                "1.0",
                testPlan
        );
        ExecutorFileEntity requirementsFile = new ExecutorFileEntity(
                UUID.randomUUID().toString(),
                "requirements.txt",
                "requests==2.28.1",
                "1.0",
                testPlan
        );
        executorFileRepository.save(planFile);
        executorFileRepository.save(requirementsFile);
        entityManager.flush();

        // When
        List<ExecutorFileEntity> files = executorFileRepository.findByPlanId(testPlan.getId());

        // Then
        assertThat(files).hasSize(2);
        assertThat(files).extracting(ExecutorFileEntity::getName)
                .containsExactlyInAnyOrder("plan.py", "requirements.txt");
    }

    @Test
    void shouldFindFilesByTaskId() {
        // Given
        ExecutorFileEntity taskFile = new ExecutorFileEntity(
                UUID.randomUUID().toString(),
                "task.py",
                "def task(input): return result",
                "1.0",
                testTask
        );
        ExecutorFileEntity requirementsFile = new ExecutorFileEntity(
                UUID.randomUUID().toString(),
                "requirements.txt",
                "numpy==1.24.0",
                "1.0",
                testTask
        );
        executorFileRepository.save(taskFile);
        executorFileRepository.save(requirementsFile);
        entityManager.flush();

        // When
        List<ExecutorFileEntity> files = executorFileRepository.findByTaskId(testTask.getId());

        // Then
        assertThat(files).hasSize(2);
        assertThat(files).extracting(ExecutorFileEntity::getName)
                .containsExactlyInAnyOrder("task.py", "requirements.txt");
    }

    @Test
    void shouldFindFileByPlanIdAndName() {
        // Given
        ExecutorFileEntity file = new ExecutorFileEntity(
                UUID.randomUUID().toString(),
                "plan.py",
                "def plan(input): return result",
                "1.0",
                testPlan
        );
        executorFileRepository.save(file);
        entityManager.flush();

        // When
        Optional<ExecutorFileEntity> found = executorFileRepository.findByPlanIdAndName(
                testPlan.getId(), "plan.py");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("plan.py");
        assertThat(found.get().getPlan()).isEqualTo(testPlan);
    }

    @Test
    void shouldFindFileByTaskIdAndName() {
        // Given
        ExecutorFileEntity file = new ExecutorFileEntity(
                UUID.randomUUID().toString(),
                "task.py",
                "def task(input): return result",
                "1.0",
                testTask
        );
        executorFileRepository.save(file);
        entityManager.flush();

        // When
        Optional<ExecutorFileEntity> found = executorFileRepository.findByTaskIdAndName(
                testTask.getId(), "task.py");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("task.py");
        assertThat(found.get().getTask()).isEqualTo(testTask);
    }

    @Test
    void shouldReturnEmptyWhenFileNotFound() {
        // When
        Optional<ExecutorFileEntity> found = executorFileRepository.findByPlanIdAndName(
                testPlan.getId(), "nonexistent.py");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void shouldCheckExistenceByPlanIdAndName() {
        // Given
        ExecutorFileEntity file = new ExecutorFileEntity(
                UUID.randomUUID().toString(),
                "plan.py",
                "def plan(input): return result",
                "1.0",
                testPlan
        );
        executorFileRepository.save(file);
        entityManager.flush();

        // When & Then
        assertThat(executorFileRepository.existsByPlanIdAndName(testPlan.getId(), "plan.py"))
                .isTrue();
        assertThat(executorFileRepository.existsByPlanIdAndName(testPlan.getId(), "nonexistent.py"))
                .isFalse();
    }

    @Test
    void shouldCheckExistenceByTaskIdAndName() {
        // Given
        ExecutorFileEntity file = new ExecutorFileEntity(
                UUID.randomUUID().toString(),
                "task.py",
                "def task(input): return result",
                "1.0",
                testTask
        );
        executorFileRepository.save(file);
        entityManager.flush();

        // When & Then
        assertThat(executorFileRepository.existsByTaskIdAndName(testTask.getId(), "task.py"))
                .isTrue();
        assertThat(executorFileRepository.existsByTaskIdAndName(testTask.getId(), "nonexistent.py"))
                .isFalse();
    }

    @Test
    void shouldDeleteFilesByPlanId() {
        // Given
        ExecutorFileEntity file1 = new ExecutorFileEntity(
                UUID.randomUUID().toString(),
                "plan.py",
                "def plan(input): return result",
                "1.0",
                testPlan
        );
        ExecutorFileEntity file2 = new ExecutorFileEntity(
                UUID.randomUUID().toString(),
                "requirements.txt",
                "requests==2.28.1",
                "1.0",
                testPlan
        );
        executorFileRepository.save(file1);
        executorFileRepository.save(file2);
        entityManager.flush();

        // When
        executorFileRepository.deleteByPlanId(testPlan.getId());
        entityManager.flush();

        // Then
        List<ExecutorFileEntity> remainingFiles = executorFileRepository.findByPlanId(testPlan.getId());
        assertThat(remainingFiles).isEmpty();
    }

    @Test
    void shouldDeleteFilesByTaskId() {
        // Given
        ExecutorFileEntity file1 = new ExecutorFileEntity(
                UUID.randomUUID().toString(),
                "task.py",
                "def task(input): return result",
                "1.0",
                testTask
        );
        ExecutorFileEntity file2 = new ExecutorFileEntity(
                UUID.randomUUID().toString(),
                "requirements.txt",
                "numpy==1.24.0",
                "1.0",
                testTask
        );
        executorFileRepository.save(file1);
        executorFileRepository.save(file2);
        entityManager.flush();

        // When
        executorFileRepository.deleteByTaskId(testTask.getId());
        entityManager.flush();

        // Then
        List<ExecutorFileEntity> remainingFiles = executorFileRepository.findByTaskId(testTask.getId());
        assertThat(remainingFiles).isEmpty();
    }
}
