package ai.eigloo.agentic.executorjava.service;

import ai.eigloo.agentic.executorjava.model.ExecutorFilePayload;
import ai.eigloo.agentic.executorjava.model.NodeType;
import ai.eigloo.agentic.executorjava.model.ResolvedExecutorNode;
import ai.eigloo.proto.model.Common.ExecutionHeader;
import ai.eigloo.proto.model.Common.ExecutionStatus;
import ai.eigloo.proto.model.Common.PlanExecution;
import ai.eigloo.proto.model.Common.PlanInput;
import ai.eigloo.proto.model.Common.PlanResult;
import ai.eigloo.proto.model.Common.TaskExecution;
import ai.eigloo.proto.model.Common.TaskInput;
import ai.eigloo.proto.model.Common.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

@Service
public class ExecutorOrchestrationService {

    private static final Logger logger = LoggerFactory.getLogger(ExecutorOrchestrationService.class);

    private final ExecutionSourceResolver sourceResolver;
    private final PythonProcessExecutor pythonProcessExecutor;

    public ExecutorOrchestrationService(ExecutionSourceResolver sourceResolver, PythonProcessExecutor pythonProcessExecutor) {
        this.sourceResolver = sourceResolver;
        this.pythonProcessExecutor = pythonProcessExecutor;
    }

    public PlanExecution handlePlanInput(String tenantId, PlanInput planInput) {
        ResolvedExecutorNode resolvedNode = sourceResolver.resolvePlanNode(tenantId, planInput);
        String executionId = UUID.randomUUID().toString();
        logger.info(
                "Executor starting plan tenant={} graph={} lifetime={} plan={} inputId={} exec={}",
                tenantId,
                resolvedNode.graphId(),
                resolvedNode.lifetimeId(),
                resolvedNode.nodeName(),
                planInput.getInputId(),
                executionId);
        try {
            MaterializedNode materializedNode = materializeNode(resolvedNode, tenantId, executionId);
            PlanResult result = pythonProcessExecutor.executePlan(
                    materializedNode.scriptPath(),
                    planInput,
                    tenantId,
                    materializedNode.workingDirectory());
            logger.info(
                    "Executor completed plan tenant={} graph={} lifetime={} plan={} exec={} nextTasks={}",
                    tenantId,
                    resolvedNode.graphId(),
                    resolvedNode.lifetimeId(),
                    resolvedNode.nodeName(),
                    executionId,
                    result.getNextTaskNamesList());
            return buildPlanExecution(planInput, resolvedNode, tenantId, executionId, ExecutionStatus.EXECUTION_STATUS_SUCCEEDED, result);
        } catch (Exception e) {
            logger.error("Plan execution failed for tenant {} plan {}: {}", tenantId, planInput.getPlanName(), e.getMessage(), e);
            PlanResult errorResult = PlanResult.newBuilder().setErrorMessage(compactErrorMessage(e)).build();
            return buildPlanExecution(planInput, resolvedNode, tenantId, executionId, ExecutionStatus.EXECUTION_STATUS_FAILED, errorResult);
        }
    }

    public TaskExecution handleTaskInput(String tenantId, TaskInput taskInput) {
        ResolvedExecutorNode resolvedNode = sourceResolver.resolveTaskNode(tenantId, taskInput);
        String executionId = UUID.randomUUID().toString();
        logger.info(
                "Executor starting task tenant={} graph={} lifetime={} task={} inputId={} exec={}",
                tenantId,
                resolvedNode.graphId(),
                resolvedNode.lifetimeId(),
                resolvedNode.nodeName(),
                taskInput.getInputId(),
                executionId);
        try {
            MaterializedNode materializedNode = materializeNode(resolvedNode, tenantId, executionId);
            TaskResult result = pythonProcessExecutor.executeTask(
                    materializedNode.scriptPath(),
                    taskInput,
                    tenantId,
                    materializedNode.workingDirectory());
            logger.info(
                    "Executor completed task tenant={} graph={} lifetime={} task={} exec={}",
                    tenantId,
                    resolvedNode.graphId(),
                    resolvedNode.lifetimeId(),
                    resolvedNode.nodeName(),
                    executionId);
            return buildTaskExecution(taskInput, resolvedNode, tenantId, executionId, ExecutionStatus.EXECUTION_STATUS_SUCCEEDED, result);
        } catch (Exception e) {
            logger.error("Task execution failed for tenant {} task {}: {}", tenantId, taskInput.getTaskName(), e.getMessage(), e);
            TaskResult errorResult = TaskResult.newBuilder().setErrorMessage(compactErrorMessage(e)).build();
            return buildTaskExecution(taskInput, resolvedNode, tenantId, executionId, ExecutionStatus.EXECUTION_STATUS_FAILED, errorResult);
        }
    }

    private PlanExecution buildPlanExecution(
            PlanInput planInput,
            ResolvedExecutorNode resolvedNode,
            String tenantId,
            String executionId,
            ExecutionStatus status,
            PlanResult result) {
        ExecutionHeader.Builder headerBuilder = ExecutionHeader.newBuilder()
                .setName(resolvedNode.nodeName())
                .setExecId(executionId)
                .setGraphId(resolvedNode.graphId())
                .setLifetimeId(resolvedNode.lifetimeId())
                .setTenantId(tenantId)
                .setCreatedAt(Instant.now().toString())
                .setStatus(status)
                .setEdgeTaken("");

        if (!planInput.getTaskExecutionsList().isEmpty() && planInput.getTaskExecutions(0).hasHeader()) {
            ExecutionHeader parentHeader = planInput.getTaskExecutions(0).getHeader();
            headerBuilder.setAttempt(parentHeader.getAttempt());
            headerBuilder.setIterationIdx(parentHeader.getIterationIdx());
        } else {
            headerBuilder.setAttempt(1);
            headerBuilder.setIterationIdx(0);
        }

        PlanExecution.Builder executionBuilder = PlanExecution.newBuilder()
                .setHeader(headerBuilder.build())
                .setResult(result != null ? result : PlanResult.newBuilder().build());

        for (TaskExecution taskExecution : planInput.getTaskExecutionsList()) {
            if (taskExecution.hasHeader()) {
                executionBuilder.addParentTaskExecIds(taskExecution.getHeader().getExecId());
                executionBuilder.addParentTaskNames(taskExecution.getHeader().getName());
            }
        }

        return executionBuilder.build();
    }

    private TaskExecution buildTaskExecution(
            TaskInput taskInput,
            ResolvedExecutorNode resolvedNode,
            String tenantId,
            String executionId,
            ExecutionStatus status,
            TaskResult result) {
        ExecutionHeader.Builder headerBuilder = ExecutionHeader.newBuilder()
                .setName(resolvedNode.nodeName())
                .setExecId(executionId)
                .setGraphId(resolvedNode.graphId())
                .setLifetimeId(resolvedNode.lifetimeId())
                .setTenantId(tenantId)
                .setCreatedAt(Instant.now().toString())
                .setStatus(status)
                .setEdgeTaken("");

        if (taskInput.hasPlanExecution() && taskInput.getPlanExecution().hasHeader()) {
            ExecutionHeader parentHeader = taskInput.getPlanExecution().getHeader();
            headerBuilder.setAttempt(parentHeader.getAttempt());
            headerBuilder.setIterationIdx(parentHeader.getIterationIdx());
        } else {
            headerBuilder.setAttempt(1);
            headerBuilder.setIterationIdx(0);
        }

        String parentPlanExecId = "";
        String parentPlanName = "";
        if (taskInput.hasPlanExecution() && taskInput.getPlanExecution().hasHeader()) {
            parentPlanExecId = taskInput.getPlanExecution().getHeader().getExecId();
            parentPlanName = taskInput.getPlanExecution().getHeader().getName();
        }

        return TaskExecution.newBuilder()
                .setHeader(headerBuilder.build())
                .setParentPlanExecId(parentPlanExecId)
                .setParentPlanName(parentPlanName)
                .setResult(result != null ? result : TaskResult.newBuilder().build())
                .build();
    }

    private MaterializedNode materializeNode(ResolvedExecutorNode resolvedNode, String tenantId, String executionId) throws IOException {
        Path workingDirectory = pythonProcessExecutor.resolveWorkingRootPath()
                .resolve(sanitizeSegment(tenantId))
                .resolve(sanitizeSegment(resolvedNode.graphId()))
                .resolve(resolvedNode.nodeType().name().toLowerCase())
                .resolve(sanitizeSegment(resolvedNode.nodeName()))
                .resolve(executionId);

        Files.createDirectories(workingDirectory);

        for (ExecutorFilePayload file : resolvedNode.files()) {
            writeExecutorFile(workingDirectory, file);
        }

        Path scriptPath = resolveSafePath(workingDirectory, resolvedNode.scriptFileName());
        if (!Files.exists(scriptPath)) {
            throw new IllegalStateException(
                    "Resolved script '" + resolvedNode.scriptFileName() + "' was not materialized for " + resolvedNode.nodeType());
        }

        logger.debug(
                "Materialized executor node tenant={} graph={} lifetime={} nodeType={} node={} workingDir={}",
                tenantId,
                resolvedNode.graphId(),
                resolvedNode.lifetimeId(),
                resolvedNode.nodeType(),
                resolvedNode.nodeName(),
                workingDirectory);

        return new MaterializedNode(workingDirectory, scriptPath);
    }

    private void writeExecutorFile(Path workingDirectory, ExecutorFilePayload file) throws IOException {
        Path filePath = resolveSafePath(workingDirectory, file.name());
        Path parent = filePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        String contents = file.contents() == null ? "" : file.contents();
        Files.writeString(filePath, contents, StandardCharsets.UTF_8);
    }

    private static Path resolveSafePath(Path baseDirectory, String fileName) {
        Path resolved = baseDirectory.resolve(fileName).normalize();
        if (!resolved.startsWith(baseDirectory)) {
            throw new IllegalArgumentException("Illegal file path outside working directory: " + fileName);
        }
        return resolved;
    }

    private static String sanitizeSegment(String value) {
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static String compactErrorMessage(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            message = throwable.getClass().getSimpleName();
        }
        if (message.length() > 1000) {
            return message.substring(0, 1000);
        }
        return message;
    }

    private record MaterializedNode(Path workingDirectory, Path scriptPath) {
    }
}
