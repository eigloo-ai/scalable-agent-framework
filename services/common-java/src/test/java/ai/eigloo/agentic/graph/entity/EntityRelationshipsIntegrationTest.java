package ai.eigloo.agentic.graph.entity;

import ai.eigloo.agentic.graph.repository.AgentGraphRepository;
import ai.eigloo.agentic.graph.repository.ExecutorFileRepository;
import ai.eigloo.agentic.graph.repository.PlanRepository;
import ai.eigloo.agentic.graph.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EntityRelationshipsIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("test_entity_relationships")
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
    private AgentGraphRepository agentGraphRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ExecutorFileRepository executorFileRepository;

    private AgentGraphEntity testGraph;

    @BeforeEach
    void setUp() {
        testGraph = new AgentGraphEntity(
                UUID.randomUUID().toString(),
                "test-tenant",
                "test-graph",
                GraphStatus.NEW
        );
        testGraph = agentGraphRepository.saveAndFlush(testGraph);
    }

    @Test
    void shouldCreateAgentGraphWithStatus() {
        AgentGraphEntity found = agentGraphRepository.findById(testGraph.getId()).orElse(null);

        assertThat(found).isNotNull();
        assertThat(found.getStatus()).isEqualTo(GraphStatus.NEW);
        assertThat(found.getTenantId()).isEqualTo("test-tenant");
        assertThat(found.getName()).isEqualTo("test-graph");
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldCreatePlanWithExecutorFiles() {
        PlanEntity plan = new PlanEntity(
                UUID.randomUUID().toString(),
                "data-collection",
                "Data Collection Plan",
                "/plans/data-collection",
                testGraph
        );
        PlanEntity savedPlan = planRepository.save(plan);

        ExecutorFileEntity planFile = new ExecutorFileEntity(
                UUID.randomUUID().toString(),
                "plan.py",
                "def plan(input): return result",
                "1.0",
                savedPlan
        );
        executorFileRepository.save(planFile);

        List<ExecutorFileEntity> files = executorFileRepository.findByPlanId(savedPlan.getId());
        assertThat(files).hasSize(1);
        assertThat(files.getFirst().getPlan()).isEqualTo(savedPlan);
        assertThat(files.getFirst().getTask()).isNull();
    }

    @Test
    void shouldCreateTaskWithExecutorFiles() {
        TaskEntity task = new TaskEntity(
                UUID.randomUUID().toString(),
                "fetch-data",
                "Fetch Data Task",
                "/tasks/fetch-data",
                testGraph
        );
        TaskEntity savedTask = taskRepository.save(task);

        ExecutorFileEntity taskFile = new ExecutorFileEntity(
                UUID.randomUUID().toString(),
                "task.py",
                "def task(input): return result",
                "1.0",
                savedTask
        );
        executorFileRepository.save(taskFile);

        List<ExecutorFileEntity> files = executorFileRepository.findByTaskId(savedTask.getId());
        assertThat(files).hasSize(1);
        assertThat(files.getFirst().getTask()).isEqualTo(savedTask);
        assertThat(files.getFirst().getPlan()).isNull();
    }

    @Test
    void shouldPersistCanonicalGraphEdges() {
        AgentGraphEntity graph = new AgentGraphEntity(
                UUID.randomUUID().toString(),
                "test-tenant",
                "edge-graph",
                GraphStatus.NEW
        );

        PlanEntity planA = new PlanEntity(UUID.randomUUID().toString(), "PlanA", "PlanA", "plans/PlanA", graph);
        PlanEntity planB = new PlanEntity(UUID.randomUUID().toString(), "PlanB", "PlanB", "plans/PlanB", graph);
        TaskEntity task1 = new TaskEntity(UUID.randomUUID().toString(), "Task1", "Task1", "tasks/Task1", graph);

        graph.addPlan(planA);
        graph.addPlan(planB);
        graph.addTask(task1);
        graph.addEdge(new GraphEdgeEntity(
                UUID.randomUUID().toString(),
                graph,
                "PlanA",
                ai.eigloo.agentic.graph.model.GraphNodeType.PLAN,
                "Task1",
                ai.eigloo.agentic.graph.model.GraphNodeType.TASK));
        graph.addEdge(new GraphEdgeEntity(
                UUID.randomUUID().toString(),
                graph,
                "Task1",
                ai.eigloo.agentic.graph.model.GraphNodeType.TASK,
                "PlanB",
                ai.eigloo.agentic.graph.model.GraphNodeType.PLAN));

        AgentGraphEntity saved = agentGraphRepository.saveAndFlush(graph);
        AgentGraphEntity loaded = agentGraphRepository.findById(saved.getId()).orElse(null);

        assertThat(loaded).isNotNull();
        assertThat(loaded.getEdges()).hasSize(2);
        assertThat(loaded.getEdges()).anyMatch(edge ->
                "PlanA".equals(edge.getFromNodeName())
                        && edge.getFromNodeType() == ai.eigloo.agentic.graph.model.GraphNodeType.PLAN
                        && "Task1".equals(edge.getToNodeName())
                        && edge.getToNodeType() == ai.eigloo.agentic.graph.model.GraphNodeType.TASK);
        assertThat(loaded.getEdges()).anyMatch(edge ->
                "Task1".equals(edge.getFromNodeName())
                        && edge.getFromNodeType() == ai.eigloo.agentic.graph.model.GraphNodeType.TASK
                        && "PlanB".equals(edge.getToNodeName())
                        && edge.getToNodeType() == ai.eigloo.agentic.graph.model.GraphNodeType.PLAN);
    }

    @Test
    void shouldDeleteFilesByPlanId() {
        PlanEntity plan = new PlanEntity(
                UUID.randomUUID().toString(),
                "data-collection",
                "Data Collection Plan",
                "/plans/data-collection",
                testGraph
        );
        plan = planRepository.save(plan);

        ExecutorFileEntity planFile = new ExecutorFileEntity(
                UUID.randomUUID().toString(),
                "plan.py",
                "def plan(input): return result",
                "1.0",
                plan
        );
        executorFileRepository.save(planFile);

        executorFileRepository.deleteByPlanId(plan.getId());
        assertThat(executorFileRepository.findByPlanId(plan.getId())).isEmpty();
    }

    @Test
    void shouldDeleteFilesByTaskId() {
        TaskEntity task = new TaskEntity(
                UUID.randomUUID().toString(),
                "fetch-data",
                "Fetch Data Task",
                "/tasks/fetch-data",
                testGraph
        );
        task = taskRepository.save(task);

        ExecutorFileEntity taskFile = new ExecutorFileEntity(
                UUID.randomUUID().toString(),
                "task.py",
                "def task(input): return result",
                "1.0",
                task
        );
        executorFileRepository.save(taskFile);

        executorFileRepository.deleteByTaskId(task.getId());
        assertThat(executorFileRepository.findByTaskId(task.getId())).isEmpty();
    }
}

