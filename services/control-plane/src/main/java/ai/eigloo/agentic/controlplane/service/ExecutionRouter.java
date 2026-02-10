package ai.eigloo.agentic.controlplane.service;

import ai.eigloo.agentic.controlplane.kafka.ExecutorProducer;
import ai.eigloo.proto.model.Common.PlanExecution;
import ai.eigloo.proto.model.Common.PlanInput;
import ai.eigloo.proto.model.Common.TaskExecution;
import ai.eigloo.proto.model.Common.TaskInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Routes persisted execution messages to the next node inputs.
 */
@Service
public class ExecutionRouter {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionRouter.class);

    private final ExecutorProducer executorProducer;
    private final GuardrailEngine guardrailEngine;
    private final TaskLookupService taskLookupService;

    public ExecutionRouter(
            ExecutorProducer executorProducer,
            GuardrailEngine guardrailEngine,
            TaskLookupService taskLookupService) {
        this.executorProducer = executorProducer;
        this.guardrailEngine = guardrailEngine;
        this.taskLookupService = taskLookupService;
    }

    /**
     * Route a completed task execution to its downstream plan (if any).
     */
    public void routeTaskExecution(TaskExecution taskExecution, String tenantId) {
        try {
            logger.debug("Routing task execution for tenant {}", tenantId);

            boolean approved = guardrailEngine.evaluateTaskExecution(taskExecution, tenantId);
            if (!approved) {
                logger.warn("Task execution rejected by guardrails for tenant {}", tenantId);
                return;
            }

            String graphId = taskExecution.hasHeader() ? taskExecution.getHeader().getGraphId() : "";
            String taskName = taskExecution.hasHeader() ? taskExecution.getHeader().getName() : "";
            Optional<String> downstreamPlanName =
                    taskLookupService.lookupDownstreamPlanName(taskName, tenantId, graphId);

            if (downstreamPlanName.isEmpty()) {
                logger.info(
                        "No downstream plan found for task '{}' in graph '{}' tenant '{}'",
                        taskName, graphId, tenantId);
                return;
            }

            PlanInput planInput = PlanInput.newBuilder()
                    .setInputId(UUID.randomUUID().toString())
                    .setPlanName(downstreamPlanName.get())
                    .addTaskExecutions(taskExecution)
                    .build();

            executorProducer.publishPlanInput(tenantId, planInput);
            logger.info(
                    "Routed task '{}' execution to downstream plan '{}' for tenant '{}'",
                    taskName, downstreamPlanName.get(), tenantId);
        } catch (Exception e) {
            logger.error("Error routing task execution for tenant {}: {}", tenantId, e.getMessage(), e);
        }
    }

    /**
     * Route a completed plan execution to next task inputs listed by the plan result.
     */
    public void routePlanExecution(PlanExecution planExecution, String tenantId) {
        try {
            logger.debug("Routing plan execution for tenant {}", tenantId);

            boolean approved = guardrailEngine.evaluatePlanExecution(planExecution, tenantId);
            if (!approved) {
                logger.warn("Plan execution rejected by guardrails for tenant {}", tenantId);
                return;
            }

            if (!planExecution.hasResult()) {
                logger.warn("Plan execution has no result payload for tenant {}", tenantId);
                return;
            }

            List<String> nextTaskNames = planExecution.getResult().getNextTaskNamesList();
            if (nextTaskNames.isEmpty()) {
                logger.info("No next tasks found in plan execution for tenant {}", tenantId);
                return;
            }

            String graphId = planExecution.hasHeader() ? planExecution.getHeader().getGraphId() : "";
            String planName = planExecution.hasHeader() ? planExecution.getHeader().getName() : "";
            List<String> resolvedTaskNames = taskLookupService.lookupExecutableTaskNames(
                    nextTaskNames,
                    tenantId,
                    graphId,
                    planName
            );

            for (String taskName : resolvedTaskNames) {
                TaskInput taskInput = TaskInput.newBuilder()
                        .setInputId(UUID.randomUUID().toString())
                        .setTaskName(taskName)
                        .setPlanExecution(planExecution)
                        .build();

                executorProducer.publishTaskInput(tenantId, taskInput);
                logger.debug(
                        "Published TaskInput for task '{}' graph '{}' tenant '{}'",
                        taskName, graphId, tenantId);
            }

            logger.info(
                    "Published {} task inputs from plan '{}' execution for tenant '{}'",
                    resolvedTaskNames.size(), planName, tenantId);
        } catch (Exception e) {
            logger.error("Error routing plan execution for tenant {}: {}", tenantId, e.getMessage(), e);
        }
    }
}
