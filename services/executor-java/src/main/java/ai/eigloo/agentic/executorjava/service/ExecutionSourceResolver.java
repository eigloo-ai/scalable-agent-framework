package ai.eigloo.agentic.executorjava.service;

import ai.eigloo.agentic.executorjava.model.ExecutorFilePayload;
import ai.eigloo.agentic.executorjava.model.NodeType;
import ai.eigloo.agentic.executorjava.model.ResolvedExecutorNode;
import ai.eigloo.agentic.graph.entity.AgentGraphEntity;
import ai.eigloo.agentic.graph.entity.ExecutorFileEntity;
import ai.eigloo.agentic.graph.entity.GraphStatus;
import ai.eigloo.agentic.graph.entity.PlanEntity;
import ai.eigloo.agentic.graph.entity.TaskEntity;
import ai.eigloo.agentic.graph.repository.AgentGraphRepository;
import ai.eigloo.proto.model.Common.PlanInput;
import ai.eigloo.proto.model.Common.TaskExecution;
import ai.eigloo.proto.model.Common.TaskInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class ExecutionSourceResolver {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionSourceResolver.class);

    private final AgentGraphRepository agentGraphRepository;

    public ExecutionSourceResolver(AgentGraphRepository agentGraphRepository) {
        this.agentGraphRepository = agentGraphRepository;
    }

    public ResolvedExecutorNode resolvePlanNode(String tenantId, PlanInput planInput) {
        String planName = planInput.getPlanName();
        if (planName == null || planName.isBlank()) {
            throw new IllegalArgumentException("PlanInput.plan_name is required");
        }

        String lifetimeId = extractLifetimeIdFromPlanInput(planInput);
        Optional<String> graphId = extractGraphIdFromPlanInput(planInput);
        AgentGraphEntity graph = resolveGraph(tenantId, graphId, NodeType.PLAN, planName);

        PlanEntity plan = graph.getPlans().stream()
                .filter(p -> planName.equals(p.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Plan '" + planName + "' not found in graph " + graph.getId()));

        List<ExecutorFilePayload> files = toPayloadFiles(plan.getFiles());
        String scriptFileName = resolveScriptFileName(files, "plan.py");

        return new ResolvedExecutorNode(
                NodeType.PLAN,
                graph.getId(),
                lifetimeId,
                plan.getName(),
                scriptFileName,
                files
        );
    }

    public ResolvedExecutorNode resolveTaskNode(String tenantId, TaskInput taskInput) {
        String taskName = taskInput.getTaskName();
        if (taskName == null || taskName.isBlank()) {
            throw new IllegalArgumentException("TaskInput.task_name is required");
        }

        String lifetimeId = extractLifetimeIdFromTaskInput(taskInput);
        Optional<String> graphId = extractGraphIdFromTaskInput(taskInput);
        AgentGraphEntity graph = resolveGraph(tenantId, graphId, NodeType.TASK, taskName);

        TaskEntity task = graph.getTasks().stream()
                .filter(t -> taskName.equals(t.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Task '" + taskName + "' not found in graph " + graph.getId()));

        List<ExecutorFilePayload> files = toPayloadFiles(task.getFiles());
        String scriptFileName = resolveScriptFileName(files, "task.py");

        return new ResolvedExecutorNode(
                NodeType.TASK,
                graph.getId(),
                lifetimeId,
                task.getName(),
                scriptFileName,
                files
        );
    }

    private AgentGraphEntity resolveGraph(String tenantId, Optional<String> explicitGraphId, NodeType nodeType, String nodeName) {
        if (explicitGraphId.isPresent()) {
            return agentGraphRepository.findByIdAndTenantIdWithAllRelations(explicitGraphId.get(), tenantId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Graph '" + explicitGraphId.get() + "' not found for tenant " + tenantId));
        }

        List<AgentGraphEntity> runningGraphs =
                agentGraphRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, GraphStatus.RUNNING);

        for (AgentGraphEntity graph : runningGraphs) {
            boolean hasNode = nodeType == NodeType.PLAN
                    ? graph.getPlans().stream().anyMatch(p -> nodeName.equals(p.getName()))
                    : graph.getTasks().stream().anyMatch(t -> nodeName.equals(t.getName()));
            if (hasNode) {
                logger.warn(
                        "Resolved {} '{}' without explicit graph id to running graph {}",
                        nodeType, nodeName, graph.getId());
                return graph;
            }
        }

        throw new IllegalArgumentException(
                "Could not resolve graph for " + nodeType + " '" + nodeName + "' for tenant " + tenantId);
    }

    private Optional<String> extractGraphIdFromPlanInput(PlanInput planInput) {
        for (TaskExecution taskExecution : planInput.getTaskExecutionsList()) {
            if (taskExecution.hasHeader() && !taskExecution.getHeader().getGraphId().isBlank()) {
                return Optional.of(taskExecution.getHeader().getGraphId());
            }
        }
        return Optional.empty();
    }

    private Optional<String> extractGraphIdFromTaskInput(TaskInput taskInput) {
        if (taskInput.hasPlanExecution()
                && taskInput.getPlanExecution().hasHeader()
                && !taskInput.getPlanExecution().getHeader().getGraphId().isBlank()) {
            return Optional.of(taskInput.getPlanExecution().getHeader().getGraphId());
        }
        return Optional.empty();
    }

    private String extractLifetimeIdFromPlanInput(PlanInput planInput) {
        for (TaskExecution taskExecution : planInput.getTaskExecutionsList()) {
            if (taskExecution.hasHeader() && !taskExecution.getHeader().getLifetimeId().isBlank()) {
                return taskExecution.getHeader().getLifetimeId();
            }
        }
        return "";
    }

    private String extractLifetimeIdFromTaskInput(TaskInput taskInput) {
        if (taskInput.hasPlanExecution()
                && taskInput.getPlanExecution().hasHeader()
                && !taskInput.getPlanExecution().getHeader().getLifetimeId().isBlank()) {
            return taskInput.getPlanExecution().getHeader().getLifetimeId();
        }
        return "";
    }

    private List<ExecutorFilePayload> toPayloadFiles(List<ExecutorFileEntity> files) {
        List<ExecutorFilePayload> payloads = new ArrayList<>();
        if (files == null) {
            return payloads;
        }
        files.stream()
                .filter(file -> file.getName() != null && !file.getName().isBlank())
                .sorted(Comparator.comparing(ExecutorFileEntity::getName))
                .forEach(file -> payloads.add(new ExecutorFilePayload(file.getName(), file.getContents())));
        return payloads;
    }

    private String resolveScriptFileName(List<ExecutorFilePayload> files, String preferredFileName) {
        return files.stream()
                .map(ExecutorFilePayload::name)
                .filter(name -> name.equals(preferredFileName) || name.endsWith("/" + preferredFileName))
                .findFirst()
                .orElseGet(() -> files.stream()
                        .map(ExecutorFilePayload::name)
                        .filter(name -> name.endsWith(".py"))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                                "No python script file found; expected " + preferredFileName)));
    }
}
