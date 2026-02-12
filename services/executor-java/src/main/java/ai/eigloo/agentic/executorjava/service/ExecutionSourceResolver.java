package ai.eigloo.agentic.executorjava.service;

import ai.eigloo.agentic.executorjava.model.ExecutorFilePayload;
import ai.eigloo.agentic.executorjava.model.NodeType;
import ai.eigloo.agentic.executorjava.model.ResolvedExecutorNode;
import ai.eigloo.agentic.graph.api.GraphLookupFile;
import ai.eigloo.agentic.graph.api.GraphLookupPlan;
import ai.eigloo.agentic.graph.api.GraphLookupResponse;
import ai.eigloo.agentic.graph.api.GraphLookupTask;
import ai.eigloo.proto.model.Common.PlanInput;
import ai.eigloo.proto.model.Common.TaskInput;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ExecutionSourceResolver {

    private final DataPlaneGraphClient dataPlaneGraphClient;

    public ExecutionSourceResolver(DataPlaneGraphClient dataPlaneGraphClient) {
        this.dataPlaneGraphClient = dataPlaneGraphClient;
    }

    public ResolvedExecutorNode resolvePlanNode(String tenantId, PlanInput planInput) {
        String planName = planInput.getPlanName();
        if (planName == null || planName.isBlank()) {
            throw new IllegalArgumentException("PlanInput.plan_name is required");
        }

        String graphId = requireNonBlank(planInput.getGraphId(), "PlanInput.graph_id");
        String lifetimeId = requireNonBlank(planInput.getLifetimeId(), "PlanInput.lifetime_id");
        GraphLookupResponse graph = resolveGraph(tenantId, graphId);

        GraphLookupPlan plan = graph.getPlans().stream()
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

        String graphId = requireNonBlank(taskInput.getGraphId(), "TaskInput.graph_id");
        String lifetimeId = requireNonBlank(taskInput.getLifetimeId(), "TaskInput.lifetime_id");
        GraphLookupResponse graph = resolveGraph(tenantId, graphId);

        GraphLookupTask task = graph.getTasks().stream()
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

    private GraphLookupResponse resolveGraph(String tenantId, String graphId) {
        return dataPlaneGraphClient.getGraph(tenantId, graphId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Graph '" + graphId + "' not found for tenant " + tenantId));
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }

    private List<ExecutorFilePayload> toPayloadFiles(List<GraphLookupFile> files) {
        List<ExecutorFilePayload> payloads = new ArrayList<>();
        if (files == null) {
            return payloads;
        }
        files.stream()
                .filter(file -> file.getName() != null && !file.getName().isBlank())
                .sorted(Comparator.comparing(GraphLookupFile::getName))
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
