package ai.eigloo.agentic.controlplane.service;

import ai.eigloo.agentic.controlplane.kafka.ExecutorProducer;
import ai.eigloo.proto.model.Common.ExecutionHeader;
import ai.eigloo.proto.model.Common.ExecutionStatus;
import ai.eigloo.proto.model.Common.PlanExecution;
import ai.eigloo.proto.model.Common.PlanInput;
import ai.eigloo.proto.model.Common.TaskExecution;
import ai.eigloo.proto.model.Common.TaskInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
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
    private final ExecutionStateGuardService executionStateGuardService;

    public ExecutionRouter(
            ExecutorProducer executorProducer,
            GuardrailEngine guardrailEngine,
            TaskLookupService taskLookupService,
            ExecutionStateGuardService executionStateGuardService) {
        this.executorProducer = executorProducer;
        this.guardrailEngine = guardrailEngine;
        this.taskLookupService = taskLookupService;
        this.executionStateGuardService = executionStateGuardService;
    }

    /**
     * Route a completed task execution to its downstream plan (if any).
     */
    public void routeTaskExecution(TaskExecution taskExecution, String tenantId) {
        try {
            if (!taskExecution.hasHeader()) {
                logger.error("Rejecting TaskExecution without header for tenant {}", tenantId);
                return;
            }

            ExecutionHeader header = taskExecution.getHeader();
            if (!hasRequiredHeaderContext(header)) {
                logger.error(
                        "Rejecting TaskExecution '{}' with missing graph/lifetime context for tenant {}",
                        header.getExecId(), tenantId);
                return;
            }
            if (header.getStatus() != ExecutionStatus.EXECUTION_STATUS_SUCCEEDED) {
                logger.info(
                        "Ignoring TaskExecution with non-terminal-success status tenant={} graph={} lifetime={} task={} exec={} status={}",
                        tenantId,
                        header.getGraphId(),
                        header.getLifetimeId(),
                        header.getName(),
                        header.getExecId(),
                        header.getStatus());
                return;
            }
            if (!executionStateGuardService.canRoute(tenantId, header)) {
                return;
            }

            boolean approved = guardrailEngine.evaluateTaskExecution(taskExecution, tenantId);
            if (!approved) {
                logger.warn("Task execution rejected by guardrails for tenant {}", tenantId);
                return;
            }

            String graphId = header.getGraphId();
            String lifetimeId = header.getLifetimeId();
            String taskName = header.getName();
            logger.info(
                    "Routing task execution tenant={} graph={} lifetime={} task={} exec={} status={}",
                    tenantId,
                    graphId,
                    lifetimeId,
                    taskName,
                    header.getExecId(),
                    header.getStatus());
            List<String> downstreamPlanNames =
                    taskLookupService.lookupDownstreamPlanNames(taskName, tenantId, graphId);

            if (downstreamPlanNames.isEmpty()) {
                logger.info(
                        "No downstream plan found tenant={} graph={} lifetime={} task={} exec={}",
                        tenantId, graphId, lifetimeId, taskName, header.getExecId());
                return;
            }

            for (String downstreamPlanName : downstreamPlanNames) {
                PlanInput planInput = PlanInput.newBuilder()
                        .setInputId(UUID.randomUUID().toString())
                        .setPlanName(downstreamPlanName)
                        .addTaskExecutions(taskExecution)
                        .setGraphId(graphId)
                        .setLifetimeId(lifetimeId)
                        .build();

                executorProducer.publishPlanInput(tenantId, planInput);
                logger.info(
                        "Published downstream PlanInput tenant={} graph={} lifetime={} fromTask={} toPlan={}",
                        tenantId, graphId, lifetimeId, taskName, downstreamPlanName);
            }
        } catch (Exception e) {
            logger.error("Error routing task execution for tenant {}: {}", tenantId, e.getMessage(), e);
        }
    }

    /**
     * Route a completed plan execution to next task inputs listed by the plan result.
     */
    public void routePlanExecution(PlanExecution planExecution, String tenantId) {
        try {
            if (!planExecution.hasHeader()) {
                logger.error("Rejecting PlanExecution without header for tenant {}", tenantId);
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

            ExecutionHeader header = planExecution.getHeader();
            if (!hasRequiredHeaderContext(header)) {
                logger.error(
                        "Rejecting PlanExecution '{}' with missing graph/lifetime context for tenant {}",
                        header.getExecId(), tenantId);
                return;
            }
            if (header.getStatus() != ExecutionStatus.EXECUTION_STATUS_SUCCEEDED) {
                logger.info(
                        "Ignoring PlanExecution with non-terminal-success status tenant={} graph={} lifetime={} plan={} exec={} status={}",
                        tenantId,
                        header.getGraphId(),
                        header.getLifetimeId(),
                        header.getName(),
                        header.getExecId(),
                        header.getStatus());
                return;
            }
            if (!executionStateGuardService.canRoute(tenantId, header)) {
                return;
            }

            boolean approved = guardrailEngine.evaluatePlanExecution(planExecution, tenantId);
            if (!approved) {
                logger.warn("Plan execution rejected by guardrails for tenant {}", tenantId);
                return;
            }

            String graphId = header.getGraphId();
            String lifetimeId = header.getLifetimeId();
            String planName = header.getName();
            logger.info(
                    "Routing plan execution tenant={} graph={} lifetime={} plan={} exec={} status={} requestedTasks={}",
                    tenantId,
                    graphId,
                    lifetimeId,
                    planName,
                    header.getExecId(),
                    header.getStatus(),
                    nextTaskNames);
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
                        .setGraphId(graphId)
                        .setLifetimeId(lifetimeId)
                        .build();

                executorProducer.publishTaskInput(tenantId, taskInput);
                logger.info(
                        "Published TaskInput tenant={} graph={} lifetime={} fromPlan={} toTask={}",
                        tenantId, graphId, lifetimeId, planName, taskName);
            }

            logger.info(
                    "Published {} task inputs tenant={} graph={} lifetime={} plan={} tasks={}",
                    resolvedTaskNames.size(), tenantId, graphId, lifetimeId, planName, resolvedTaskNames);
        } catch (Exception e) {
            logger.error("Error routing plan execution for tenant {}: {}", tenantId, e.getMessage(), e);
        }
    }

    private static boolean hasRequiredHeaderContext(ExecutionHeader header) {
        return header != null
                && !header.getName().isBlank()
                && !header.getGraphId().isBlank()
                && !header.getLifetimeId().isBlank();
    }
}
