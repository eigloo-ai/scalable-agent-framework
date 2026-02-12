package ai.eigloo.agentic.integration;

import ai.eigloo.agentic.common.ProtobufUtils;
import ai.eigloo.agentic.common.TenantAwareKafkaConfig;
import ai.eigloo.agentic.common.TopicNames;
import ai.eigloo.agentic.controlplane.ControlPlaneApplication;
import ai.eigloo.agentic.dataplane.DataPlaneApplication;
import ai.eigloo.agentic.dataplane.entity.PlanExecutionEntity;
import ai.eigloo.agentic.dataplane.entity.TaskExecutionEntity;
import ai.eigloo.agentic.dataplane.repository.PlanExecutionRepository;
import ai.eigloo.agentic.dataplane.repository.TaskExecutionRepository;
import ai.eigloo.agentic.executorjava.config.ExecutorPythonProperties;
import ai.eigloo.agentic.executorjava.ExecutorJavaApplication;
import ai.eigloo.agentic.executorjava.service.PythonProcessExecutor;
import ai.eigloo.agentic.graph.entity.AgentGraphEntity;
import ai.eigloo.agentic.graph.entity.ExecutorFileEntity;
import ai.eigloo.agentic.graph.entity.GraphEdgeEntity;
import ai.eigloo.agentic.graph.entity.GraphStatus;
import ai.eigloo.agentic.graph.entity.PlanEntity;
import ai.eigloo.agentic.graph.entity.TaskEntity;
import ai.eigloo.agentic.graph.repository.AgentGraphRepository;
import ai.eigloo.proto.model.Common.PlanInput;
import ai.eigloo.proto.model.Common.PlanResult;
import ai.eigloo.proto.model.Common.TaskInput;
import ai.eigloo.proto.model.Common.TaskResult;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.errors.TopicExistsException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SimpleIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(SimpleIntegrationTest.class);

    private static final Set<String> EXPECTED_PLAN_NAMES = Set.of("PlanA", "PlanB");
    private static final Set<String> EXPECTED_TASK_NAMES = Set.of("Task1A", "Task1B", "Task2");
    private static final String TENANT_ID = "tenantintegration";
    private static final String FAILING_PLAN_NAME = "PlanFail";
    private static final String BLOCKED_TASK_NAME = "TaskNever";

    private static final String PLAN_A_SCRIPT = """
            from agentic_common.pb import PlanResult

            def plan(plan_input):
                return PlanResult(next_task_names=["Task1A", "Task1B"])
            """;

    private static final String PLAN_B_SCRIPT = """
            from agentic_common.pb import PlanResult

            def plan(plan_input):
                return PlanResult(next_task_names=["Task2"])
            """;

    private static final String FAILING_PLAN_SCRIPT = """
            from agentic_common.pb import PlanResult

            def plan(plan_input):
                return PlanResult(next_task_names=["TaskNever"])
            """;

    private static final String TASK_SCRIPT = """
            from agentic_common.pb import TaskResult

            def task(task_input):
                return TaskResult()
            """;

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("agentic_integration")
            .withUsername("agentic")
            .withPassword("agentic");

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    private ConfigurableApplicationContext dataPlaneContext;
    private ConfigurableApplicationContext controlPlaneContext;
    private ConfigurableApplicationContext executorContext;

    @BeforeAll
    void startServices() {
        Path repositoryRoot = resolveRepositoryRoot();
        createTenantTopics(TENANT_ID);
        Map<String, Object> shared = sharedProperties();

        dataPlaneContext = startApplicationContext(
                DataPlaneApplication.class,
                mergeProperties(shared, Map.of(
                        "spring.application.name", "integration-data-plane",
                        "server.port", "0"
                )));

        String dataPlanePort = dataPlaneContext.getEnvironment().getProperty("local.server.port");
        if (dataPlanePort == null || dataPlanePort.isBlank()) {
            throw new IllegalStateException("Data-plane local server port was not assigned");
        }
        String dataPlaneBaseUrl = "http://localhost:" + dataPlanePort;

        controlPlaneContext = startApplicationContext(
                ControlPlaneApplication.class,
                mergeProperties(shared, Map.of(
                        "spring.application.name", "integration-control-plane",
                        "server.port", "0",
                        "agentic.data-plane.base-url", dataPlaneBaseUrl
                )));

        executorContext = startApplicationContext(
                ExecutorJavaApplication.class,
                mergeProperties(shared, Map.of(
                        "spring.application.name", "integration-executor-java",
                        "server.port", "0",
                        "executor.python.command", "python3",
                        "executor.python.common-py-path", repositoryRoot.resolve("services/common-py").toString(),
                        "executor.python.working-root", repositoryRoot.resolve("services/integration_tests-java/target/executor-work").toString(),
                        "executor.python.timeout-seconds", "60",
                        "agentic.data-plane.base-url", dataPlaneBaseUrl
                )));
    }

    @AfterAll
    void stopServices() {
        if (executorContext != null) {
            executorContext.close();
        }
        if (controlPlaneContext != null) {
            controlPlaneContext.close();
        }
        if (dataPlaneContext != null) {
            dataPlaneContext.close();
        }
    }

    @Test
    void shouldExecuteSimpleGraphEndToEnd() throws Exception {
        String tenantId = TENANT_ID;
        String graphId = UUID.randomUUID().toString();
        String lifetimeId = UUID.randomUUID().toString();

        seedSimpleGraph(tenantId, graphId);
        publishInitialPlanInput(tenantId, graphId, lifetimeId);

        awaitAllNodesExecuted(tenantId, graphId, lifetimeId, Duration.ofSeconds(90));
    }

    @Test
    void shouldPersistPlanFailureAndNotExecuteDownstreamTasks() throws Exception {
        String tenantId = TENANT_ID;
        String graphId = UUID.randomUUID().toString();
        String lifetimeId = UUID.randomUUID().toString();

        seedFailingGraph(tenantId, graphId);
        publishPlanInput(tenantId, graphId, lifetimeId, FAILING_PLAN_NAME);

        awaitPlanFailureWithoutTaskExecution(
                tenantId,
                graphId,
                lifetimeId,
                FAILING_PLAN_NAME,
                BLOCKED_TASK_NAME,
                Duration.ofSeconds(90));
    }

    private void seedSimpleGraph(String tenantId, String graphId) {
        AgentGraphRepository graphRepository = dataPlaneContext.getBean(AgentGraphRepository.class);

        AgentGraphEntity graph = new AgentGraphEntity(graphId, tenantId, "integration-graph", GraphStatus.ACTIVE);

        PlanEntity planA = new PlanEntity(UUID.randomUUID().toString(), "PlanA", "Plan A", "plan.py", graph);
        planA.addFile(new ExecutorFileEntity(UUID.randomUUID().toString(), "plan.py", PLAN_A_SCRIPT, "1.0.0", planA));

        PlanEntity planB = new PlanEntity(UUID.randomUUID().toString(), "PlanB", "Plan B", "plan.py", graph);
        planB.addFile(new ExecutorFileEntity(UUID.randomUUID().toString(), "plan.py", PLAN_B_SCRIPT, "1.0.0", planB));

        TaskEntity task1A = new TaskEntity(
                UUID.randomUUID().toString(),
                "Task1A",
                "Task 1A",
                "task.py",
                graph
        );
        task1A.addFile(new ExecutorFileEntity(UUID.randomUUID().toString(), "task.py", TASK_SCRIPT, "1.0.0", task1A));

        TaskEntity task1B = new TaskEntity(
                UUID.randomUUID().toString(),
                "Task1B",
                "Task 1B",
                "task.py",
                graph
        );
        task1B.addFile(new ExecutorFileEntity(UUID.randomUUID().toString(), "task.py", TASK_SCRIPT, "1.0.0", task1B));

        TaskEntity task2 = new TaskEntity(
                UUID.randomUUID().toString(),
                "Task2",
                "Task 2",
                "task.py",
                graph
        );
        task2.addFile(new ExecutorFileEntity(UUID.randomUUID().toString(), "task.py", TASK_SCRIPT, "1.0.0", task2));

        graph.addPlan(planA);
        graph.addPlan(planB);
        graph.addTask(task1A);
        graph.addTask(task1B);
        graph.addTask(task2);
        graph.addEdge(new GraphEdgeEntity(
                UUID.randomUUID().toString(),
                graph,
                "PlanA",
                ai.eigloo.agentic.graph.model.GraphNodeType.PLAN,
                "Task1A",
                ai.eigloo.agentic.graph.model.GraphNodeType.TASK));
        graph.addEdge(new GraphEdgeEntity(
                UUID.randomUUID().toString(),
                graph,
                "PlanA",
                ai.eigloo.agentic.graph.model.GraphNodeType.PLAN,
                "Task1B",
                ai.eigloo.agentic.graph.model.GraphNodeType.TASK));
        graph.addEdge(new GraphEdgeEntity(
                UUID.randomUUID().toString(),
                graph,
                "Task1A",
                ai.eigloo.agentic.graph.model.GraphNodeType.TASK,
                "PlanB",
                ai.eigloo.agentic.graph.model.GraphNodeType.PLAN));
        graph.addEdge(new GraphEdgeEntity(
                UUID.randomUUID().toString(),
                graph,
                "PlanB",
                ai.eigloo.agentic.graph.model.GraphNodeType.PLAN,
                "Task2",
                ai.eigloo.agentic.graph.model.GraphNodeType.TASK));

        graphRepository.saveAndFlush(graph);
    }

    private void seedFailingGraph(String tenantId, String graphId) {
        AgentGraphRepository graphRepository = dataPlaneContext.getBean(AgentGraphRepository.class);

        AgentGraphEntity graph = new AgentGraphEntity(graphId, tenantId, "integration-failing-graph", GraphStatus.ACTIVE);

        PlanEntity failingPlan = new PlanEntity(
                UUID.randomUUID().toString(),
                FAILING_PLAN_NAME,
                "Plan Fail",
                "plan.py",
                graph);
        failingPlan.addFile(new ExecutorFileEntity(
                UUID.randomUUID().toString(),
                "plan.py",
                FAILING_PLAN_SCRIPT,
                "1.0.0",
                failingPlan));

        TaskEntity blockedTask = new TaskEntity(
                UUID.randomUUID().toString(),
                BLOCKED_TASK_NAME,
                "Task Never",
                "task.py",
                graph
        );
        blockedTask.addFile(new ExecutorFileEntity(
                UUID.randomUUID().toString(),
                "task.py",
                TASK_SCRIPT,
                "1.0.0",
                blockedTask));

        graph.addPlan(failingPlan);
        graph.addTask(blockedTask);
        graph.addEdge(new GraphEdgeEntity(
                UUID.randomUUID().toString(),
                graph,
                FAILING_PLAN_NAME,
                ai.eigloo.agentic.graph.model.GraphNodeType.PLAN,
                BLOCKED_TASK_NAME,
                ai.eigloo.agentic.graph.model.GraphNodeType.TASK));
        graphRepository.saveAndFlush(graph);
    }

    private void createTenantTopics(String tenantId) {
        Set<String> topicNames = Set.of(
                TopicNames.planInputs(tenantId),
                TopicNames.taskInputs(tenantId),
                TopicNames.planExecutions(tenantId),
                TopicNames.taskExecutions(tenantId),
                TopicNames.persistedPlanExecutions(tenantId),
                TopicNames.persistedTaskExecutions(tenantId)
        );

        Properties adminProperties = new Properties();
        adminProperties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());

        try (AdminClient adminClient = AdminClient.create(adminProperties)) {
            var newTopics = topicNames.stream()
                    .map(topic -> new NewTopic(topic, 1, (short) 1))
                    .toList();

            try {
                adminClient.createTopics(newTopics).all().get(20, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                if (!(e.getCause() instanceof TopicExistsException)) {
                    throw new IllegalStateException("Failed creating integration test Kafka topics", e);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to prepare tenant topics for integration test", e);
        }
    }

    private void publishInitialPlanInput(String tenantId, String graphId, String lifetimeId) throws Exception {
        publishPlanInput(tenantId, graphId, lifetimeId, "PlanA");
    }

    private void publishPlanInput(String tenantId, String graphId, String lifetimeId, String planName) throws Exception {
        PlanInput input = PlanInput.newBuilder()
                .setInputId(UUID.randomUUID().toString())
                .setPlanName(planName)
                .setGraphId(graphId)
                .setLifetimeId(lifetimeId)
                .build();

        byte[] payload = ProtobufUtils.serializePlanInput(input);
        assertNotNull(payload, "PlanInput serialization should not return null");

        String topic = TopicNames.planInputs(tenantId);
        String key = TopicNames.graphNodeKey(graphId, planName);

        Properties producerProperties = new Properties();
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        producerProperties.put(ProducerConfig.ACKS_CONFIG, "all");

        try (KafkaProducer<String, byte[]> producer = new KafkaProducer<>(producerProperties)) {
            producer.send(new ProducerRecord<>(topic, key, payload)).get(20, TimeUnit.SECONDS);
        }
    }

    private void awaitAllNodesExecuted(
            String tenantId,
            String graphId,
            String expectedLifetimeId,
            Duration timeout) throws InterruptedException {
        PlanExecutionRepository planRepository = dataPlaneContext.getBean(PlanExecutionRepository.class);
        TaskExecutionRepository taskRepository = dataPlaneContext.getBean(TaskExecutionRepository.class);

        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            List<PlanExecutionEntity> planExecutions = planRepository.findByTenantIdAndGraphId(tenantId, graphId);
            List<TaskExecutionEntity> taskExecutions = taskRepository.findByTenantIdAndGraphId(tenantId, graphId);

            Set<String> succeededPlans = new HashSet<>();
            for (PlanExecutionEntity execution : planExecutions) {
                if (execution.getStatus() == PlanExecutionEntity.ExecutionStatus.EXECUTION_STATUS_SUCCEEDED) {
                    succeededPlans.add(execution.getName());
                }
            }

            Set<String> succeededTasks = new HashSet<>();
            for (TaskExecutionEntity execution : taskExecutions) {
                if (execution.getStatus() == TaskExecutionEntity.ExecutionStatus.EXECUTION_STATUS_SUCCEEDED) {
                    succeededTasks.add(execution.getName());
                }
            }

            if (succeededPlans.containsAll(EXPECTED_PLAN_NAMES) && succeededTasks.containsAll(EXPECTED_TASK_NAMES)) {
                assertExecutionContext(planExecutions, taskExecutions, expectedLifetimeId);
                assertExecutionLineage(planExecutions, taskExecutions);
                return;
            }

            Thread.sleep(500);
        }

        List<PlanExecutionEntity> finalPlans = planRepository.findByTenantIdAndGraphId(tenantId, graphId);
        List<TaskExecutionEntity> finalTasks = taskRepository.findByTenantIdAndGraphId(tenantId, graphId);
        fail("Timed out waiting for graph execution. Plans=" + summarizePlans(finalPlans)
                + " Tasks=" + summarizeTasks(finalTasks));
    }

    private void awaitPlanFailureWithoutTaskExecution(
            String tenantId,
            String graphId,
            String expectedLifetimeId,
            String expectedFailedPlanName,
            String blockedTaskName,
            Duration timeout) throws InterruptedException {
        PlanExecutionRepository planRepository = dataPlaneContext.getBean(PlanExecutionRepository.class);
        TaskExecutionRepository taskRepository = dataPlaneContext.getBean(TaskExecutionRepository.class);

        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            List<PlanExecutionEntity> planExecutions = planRepository.findByTenantIdAndGraphId(tenantId, graphId);
            List<TaskExecutionEntity> taskExecutions = taskRepository.findByTenantIdAndGraphId(tenantId, graphId);

            PlanExecutionEntity failedPlanExecution = planExecutions.stream()
                    .filter(e -> expectedLifetimeId.equals(e.getLifetimeId()))
                    .filter(e -> expectedFailedPlanName.equals(e.getName()))
                    .filter(e -> e.getStatus() == PlanExecutionEntity.ExecutionStatus.EXECUTION_STATUS_FAILED)
                    .findFirst()
                    .orElse(null);

            boolean blockedTaskExecuted = taskExecutions.stream()
                    .filter(e -> expectedLifetimeId.equals(e.getLifetimeId()))
                    .anyMatch(e -> blockedTaskName.equals(e.getName()));

            if (failedPlanExecution != null && !blockedTaskExecuted) {
                assertTrue(
                        failedPlanExecution.getErrorMessage() != null && !failedPlanExecution.getErrorMessage().isBlank(),
                        "Failed plan execution should include an error message");
                return;
            }

            Thread.sleep(500);
        }

        List<PlanExecutionEntity> finalPlans = planRepository.findByTenantIdAndGraphId(tenantId, graphId);
        List<TaskExecutionEntity> finalTasks = taskRepository.findByTenantIdAndGraphId(tenantId, graphId);
        fail("Timed out waiting for expected plan failure. Plans=" + summarizePlans(finalPlans)
                + " Tasks=" + summarizeTasks(finalTasks));
    }

    private static void assertExecutionContext(
            List<PlanExecutionEntity> planExecutions,
            List<TaskExecutionEntity> taskExecutions,
            String expectedLifetimeId) {
        assertTrue(planExecutions.stream().allMatch(e -> expectedLifetimeId.equals(e.getLifetimeId())),
                "All plan executions should use the requested lifetime_id");
        assertTrue(taskExecutions.stream().allMatch(e -> expectedLifetimeId.equals(e.getLifetimeId())),
                "All task executions should use the requested lifetime_id");
    }

    private static void assertExecutionLineage(
            List<PlanExecutionEntity> planExecutions,
            List<TaskExecutionEntity> taskExecutions) {
        boolean hasPlanBFromTask1A = planExecutions.stream()
                .filter(e -> "PlanB".equals(e.getName()))
                .anyMatch(e -> e.getParentTaskNames() != null && e.getParentTaskNames().contains("Task1A"));
        assertTrue(hasPlanBFromTask1A, "PlanB should be triggered by Task1A");

        boolean hasTask2FromPlanB = taskExecutions.stream()
                .filter(e -> "Task2".equals(e.getName()))
                .anyMatch(e -> "PlanB".equals(e.getParentPlanName()));
        assertTrue(hasTask2FromPlanB, "Task2 should be triggered by PlanB");
    }

    private static String summarizePlans(List<PlanExecutionEntity> executions) {
        return executions.stream()
                .map(e -> e.getName() + ":" + e.getStatus())
                .toList()
                .toString();
    }

    private static String summarizeTasks(List<TaskExecutionEntity> executions) {
        return executions.stream()
                .map(e -> e.getName() + ":" + e.getStatus())
                .toList()
                .toString();
    }

    private Map<String, Object> sharedProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("spring.datasource.url", POSTGRES.getJdbcUrl());
        properties.put("spring.datasource.username", POSTGRES.getUsername());
        properties.put("spring.datasource.password", POSTGRES.getPassword());
        properties.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
        properties.put("spring.jpa.hibernate.ddl-auto", "update");
        properties.put("spring.jpa.show-sql", "false");
        properties.put("spring.flyway.enabled", "false");
        properties.put("spring.kafka.bootstrap-servers", KAFKA.getBootstrapServers());
        properties.put("spring.kafka.consumer.auto-offset-reset", "earliest");
        properties.put("spring.kafka.listener.missing-topics-fatal", "false");
        properties.put("kafka.tenant.concurrency", "1");
        properties.put("logging.level.ai.eigloo.agentic", "INFO");
        return properties;
    }

    private static Path resolveRepositoryRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isDirectory(current.resolve("services/common-py"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Could not resolve repository root containing services/common-py");
    }

    private static ConfigurableApplicationContext startApplicationContext(
            Class<?> applicationClass,
            Map<String, Object> properties) {
        String[] args = properties.entrySet().stream()
                .map(entry -> "--" + entry.getKey() + "=" + entry.getValue())
                .toArray(String[]::new);

        return new SpringApplicationBuilder(applicationClass, IntegrationKafkaTemplateConfig.class)
                .run(args);
    }

    private static Map<String, Object> mergeProperties(Map<String, Object> base, Map<String, Object> overrides) {
        Map<String, Object> merged = new HashMap<>(base);
        merged.putAll(overrides);
        return merged;
    }

    @Configuration(proxyBeanMethods = false)
    @Import({TenantAwareKafkaConfig.class})
    static class IntegrationKafkaTemplateConfig {
        @Bean
        @Primary
        @ConditionalOnProperty(name = "spring.application.name", havingValue = "integration-executor-java")
        PythonProcessExecutor integrationPythonProcessExecutor() {
            return new PythonProcessExecutor(new ExecutorPythonProperties()) {
                @Override
                public void initializeRunnerScript() {
                    // Test stub: do not initialize external python runner.
                }

                @Override
                public PlanResult executePlan(Path scriptPath, PlanInput planInput, String tenantId, Path workingDirectory) {
                    if (FAILING_PLAN_NAME.equals(planInput.getPlanName())) {
                        throw new IllegalStateException("Intentional integration-test plan failure");
                    }
                    if ("PlanA".equals(planInput.getPlanName())) {
                        return PlanResult.newBuilder()
                                .addNextTaskNames("Task1A")
                                .addNextTaskNames("Task1B")
                                .build();
                    }
                    if ("PlanB".equals(planInput.getPlanName())) {
                        return PlanResult.newBuilder()
                                .addNextTaskNames("Task2")
                                .build();
                    }
                    return PlanResult.newBuilder().build();
                }

                @Override
                public TaskResult executeTask(Path scriptPath, TaskInput taskInput, String tenantId, Path workingDirectory) {
                    return TaskResult.newBuilder().build();
                }
            };
        }
    }
}
