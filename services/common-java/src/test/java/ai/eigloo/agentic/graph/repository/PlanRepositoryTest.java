package ai.eigloo.agentic.graph.repository;

import ai.eigloo.agentic.graph.entity.AgentGraphEntity;
import ai.eigloo.agentic.graph.entity.ExecutorFileEntity;
import ai.eigloo.agentic.graph.entity.GraphStatus;
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
 * Integration tests for PlanRepository custom queries.
 * This test specifically covers the @Query methods that were causing MultipleBagFetchException.
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PlanRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("test_plan_repository")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PlanRepository planRepository;

    private AgentGraphEntity testGraph;
    private PlanEntity testPlan;
    private TaskEntity upstreamTask;

    @BeforeEach
    void setUp() {
        // Create test plan with files
        testPlan = new PlanEntity(
                UUID.randomUUID().toString(),
                "test-plan",
                "Test Plan",
                "/plans/test-plan",
                null // Will be set when added to graph
        );
        
        // Create executor files for the plan
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
        
        // Add files to plan using helper methods
        testPlan.addFile(planFile);
        testPlan.addFile(requirementsFile);
        
        // Create upstream task
        upstreamTask = new TaskEntity(
                UUID.randomUUID().toString(),
                "upstream-task",
                "Upstream Task",
                "/tasks/upstream-task",
                null, // Will be set when added to graph
                null  // No upstream plan for this task
        );
        
        // Create test graph with plans and tasks
        testGraph = new AgentGraphEntity(
                UUID.randomUUID().toString(),
                "test-tenant",
                "test-graph",
                GraphStatus.NEW
        );
        
        // Set up bidirectional relationships
        testPlan.setAgentGraph(testGraph);
        upstreamTask.setAgentGraph(testGraph);
        testGraph.setPlans(List.of(testPlan));
        testGraph.setTasks(List.of(upstreamTask));
        
        // Save only the graph - should cascade to save plans, tasks, and files
        entityManager.persistAndFlush(testGraph);
    }

    @Test
    void shouldFindByAgentGraphIdWithFiles_WithoutMultipleBagFetchException() {
        // This test verifies that the fixed query doesn't throw MultipleBagFetchException
        // When
        List<PlanEntity> plans = planRepository.findByAgentGraphIdWithFiles(testGraph.getId());

        // Then
        assertThat(plans).hasSize(1);
        PlanEntity foundPlan = plans.get(0);
        assertThat(foundPlan.getName()).isEqualTo("test-plan");
        
        // Verify files are loaded (this was the main purpose of the query)
        // The files should be eagerly loaded due to the JOIN FETCH
        assertThat(foundPlan.getFiles()).isNotNull();
        assertThat(foundPlan.getFiles()).hasSize(2);
        assertThat(foundPlan.getFiles()).extracting(ExecutorFileEntity::getName)
                .containsExactlyInAnyOrder("plan.py", "requirements.txt");
        
        // Note: upstreamTasks are not eagerly loaded in the fixed query
        // This is intentional to avoid MultipleBagFetchException
        // If needed, they can be loaded separately
    }

    @Test
    void shouldFindByIdWithAllRelations_WithoutMultipleBagFetchException() {
        // This test verifies that the fixed query doesn't throw MultipleBagFetchException
        // When
        Optional<PlanEntity> foundPlan = planRepository.findByIdWithAllRelations(testPlan.getId());

        // Then
        assertThat(foundPlan).isPresent();
        PlanEntity plan = foundPlan.get();
        assertThat(plan.getName()).isEqualTo("test-plan");
        
        // Verify files are loaded
        assertThat(plan.getFiles()).isNotNull();
        assertThat(plan.getFiles()).hasSize(2);
        assertThat(plan.getFiles()).extracting(ExecutorFileEntity::getName)
                .containsExactlyInAnyOrder("plan.py", "requirements.txt");
        
        // Note: other relationships are not eagerly loaded in the fixed query
        // This is intentional to avoid MultipleBagFetchException
    }

    @Test
    void shouldFindByAgentGraphId_BasicQuery() {
        // Test the basic query without JOIN FETCH
        // When
        List<PlanEntity> plans = planRepository.findByAgentGraphId(testGraph.getId());

        // Then
        assertThat(plans).hasSize(1);
        assertThat(plans.get(0).getName()).isEqualTo("test-plan");
    }

    @Test
    void shouldFindByIdAndAgentGraphId() {
        // When
        Optional<PlanEntity> foundPlan = planRepository.findByIdAndAgentGraphId(
                testPlan.getId(), testGraph.getId());

        // Then
        assertThat(foundPlan).isPresent();
        assertThat(foundPlan.get().getName()).isEqualTo("test-plan");
    }

    @Test
    void shouldFindByIdsWithFiles() {
        // When
        List<PlanEntity> plans = planRepository.findByIdsWithFiles(List.of(testPlan.getId()));

        // Then
        assertThat(plans).hasSize(1);
        PlanEntity foundPlan = plans.get(0);
        assertThat(foundPlan.getName()).isEqualTo("test-plan");
        assertThat(foundPlan.getFiles()).hasSize(2);
    }
}