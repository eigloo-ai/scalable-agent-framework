package ai.eigloo.agentic.controlplane.service;

import ai.eigloo.agentic.controlplane.kafka.ExecutorProducer;
import ai.eigloo.agentic.graph.model.Task;
import ai.eigloo.proto.model.Common.TaskExecution;
import ai.eigloo.proto.model.Common.PlanExecution;
import ai.eigloo.proto.model.Common.PlanInput;
import ai.eigloo.proto.model.Common.TaskInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Service for routing execution messages to appropriate executors.
 * 
 * This service implements the corrected routing logic:
 * - TaskExecution gets examined to look up the next Plan in the graph and sent to PlanExecutor
 * - PlanExecution gets examined to extract next_task_names, look up Tasks, and create TaskInput messages
 */
@Service
public class ExecutionRouter {
    
    private static final Logger logger = LoggerFactory.getLogger(ExecutionRouter.class);
    
    private final ExecutorProducer executorProducer;
    private final GuardrailEngine guardrailEngine;
    private final TaskLookupService taskLookupService;
    
    @Autowired
    public ExecutionRouter(ExecutorProducer executorProducer, GuardrailEngine guardrailEngine, TaskLookupService taskLookupService) {
        this.executorProducer = executorProducer;
        this.guardrailEngine = guardrailEngine;
        this.taskLookupService = taskLookupService;
    }
    
    /**
     * Route task execution protobuf message to appropriate handler.
     * Examines TaskExecution and looks up the next Plan in the graph.
     * 
     * @param taskExecution the TaskExecution protobuf message
     * @param tenantId the tenant identifier
     */
    public void routeTaskExecution(TaskExecution taskExecution, String tenantId) {
        try {
            logger.debug("Routing task execution for tenant: {}", tenantId);
            
            // Evaluate guardrails for task execution
            boolean approved = guardrailEngine.evaluateTaskExecution(taskExecution, tenantId);
            // TODO:  This is all kinds of wrong.  This needs to wait for multiple TaskExecutions completing before seting a PlanInput
            //  For now it will do.
            if (approved) {
                // Examine TaskExecution and look up the next Plan in the graph
                String nextPlanName = lookupNextPlanInGraph(taskExecution, tenantId);
                
                // Create PlanInput with the next plan information
                PlanInput planInput = PlanInput.newBuilder()
                    .setInputId(taskExecution.getHeader().getExecId())
                    .setPlanName(nextPlanName)
                    .addTaskExecutions(taskExecution)
                    .build();
                
                // Route PlanInput to PlanExecutor
                executorProducer.publishPlanInput(tenantId, planInput);
                
                logger.info("Task execution examined and next plan '{}' routed to PlanExecutor for tenant: {}", 
                    nextPlanName, tenantId);
            } else {
                logger.warn("Task execution rejected by guardrails for tenant: {}", tenantId);
                // Could implement rejection handling here
            }
            
        } catch (Exception e) {
            logger.error("Error routing task execution for tenant {}: {}", tenantId, e.getMessage(), e);
        }
    }
    
    /**
     * Look up the next Plan in the graph based on TaskExecution.
     * 
     * @param taskExecution the TaskExecution to examine
     * @param tenantId the tenant identifier
     * @return the name of the next plan in the graph path
     */
    private String lookupNextPlanInGraph(TaskExecution taskExecution, String tenantId) {
        // TODO: Implement graph lookup logic
        // This should examine the TaskExecution and determine the next plan in the graph
        // For now, return a stub implementation
        logger.debug("Looking up next plan in graph for task execution: {}", 
            taskExecution.getHeader().getExecId());
        
        // Stub implementation - replace with actual graph lookup
        return "next-plan-stub";
    }
    
    /**
     * Route plan execution protobuf message to appropriate handler.
     * Examines PlanExecution.result.next_task_names, looks up Tasks, and creates TaskInput messages.
     * 
     * @param planExecution the PlanExecution protobuf message
     * @param tenantId the tenant identifier
     */
    public void routePlanExecution(PlanExecution planExecution, String tenantId) {
        try {
            logger.debug("Routing plan execution for tenant: {}", tenantId);
            
            // Evaluate guardrails for plan execution
            boolean approved = guardrailEngine.evaluatePlanExecution(planExecution, tenantId);
            
            if (approved) {
                // Extract next_task_names from PlanExecution.result
                List<String> nextTaskNames = planExecution.getResult().getNextTaskNamesList();
                
                if (nextTaskNames.isEmpty()) {
                    logger.info("No next tasks found in plan execution for tenant: {}", tenantId);
                    return;
                }
                
                logger.info("Found {} next tasks in plan execution for tenant {}: {}", 
                    nextTaskNames.size(), tenantId, nextTaskNames);
                
                // Look up task metadata for each task name
                List<Task> tasks = taskLookupService.lookupTasksByNames(nextTaskNames, tenantId);
                
                // Create and publish TaskInput message for each task
                for (Task task : tasks) {
                    String inputId = UUID.randomUUID().toString();
                    
                    TaskInput taskInput = TaskInput.newBuilder()
                        .setInputId(inputId)
                        .setTaskName(task.name())
                        .setPlanExecution(planExecution)
                        .build();
                    
                    // Publish TaskInput to task-inputs topic
                    executorProducer.publishTaskInput(tenantId, taskInput);
                    
                    logger.debug("Created and published TaskInput for task '{}' with input_id '{}' for tenant: {}", 
                        task.name(), inputId, tenantId);
                }
                
                logger.info("Successfully created and published {} TaskInput messages for tenant: {}", 
                    tasks.size(), tenantId);
                
            } else {
                logger.warn("Plan execution rejected by guardrails for tenant: {}", tenantId);
                // Could implement rejection handling here
            }
            
        } catch (Exception e) {
            logger.error("Error routing plan execution for tenant {}: {}", tenantId, e.getMessage(), e);
        }
    }
} 