package ai.eigloo.agentic.controlplane.service;

import ai.eigloo.proto.model.Common.TaskExecution;
import ai.eigloo.proto.model.Common.PlanExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service responsible for evaluating guardrail policies for task and plan executions.
 * 
 * This service:
 * - Consumes execution messages from data plane topics
 * - Evaluates guardrail policies (token limits, cost thresholds, etc.)
 * - Routes approved executions to appropriate executor topics
 * - Rejects executions that violate policies
 */
@Service
public class GuardrailEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(GuardrailEngine.class);
    
    private final Map<String, Object> executionStatus = new ConcurrentHashMap<>();
    
    public GuardrailEngine() {}
    
    /**
     * Evaluate guardrails for a TaskExecution protobuf message.
     * 
     * @param taskExecution the TaskExecution protobuf message
     * @param tenantId the tenant identifier
     * @return true if execution is approved, false otherwise
     */
    public boolean evaluateTaskExecution(TaskExecution taskExecution, String tenantId) {
        try {
            logger.debug("Evaluating guardrails for TaskExecution {}/{} for tenant: {}", 
                taskExecution.getHeader().getName(), taskExecution.getHeader().getExecId(), tenantId);
            
            // TODO: Implement actual guardrail evaluation
            // - Check token limits from taskExecution.getResult()
            // - Validate cost thresholds
            // - Verify execution permissions from taskExecution.getHeader()
            
            // For now, always approve
            logger.debug("TaskExecution {}/{} approved by guardrails for tenant: {}", 
                taskExecution.getHeader().getName(), taskExecution.getHeader().getExecId(), tenantId);
            return true;
            
        } catch (Exception e) {
            logger.error("Error evaluating guardrails for TaskExecution {}/{} for tenant {}: {}", 
                taskExecution.getHeader().getName(), taskExecution.getHeader().getExecId(), tenantId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Evaluate guardrails for a PlanExecution protobuf message.
     * 
     * @param planExecution the PlanExecution protobuf message
     * @param tenantId the tenant identifier
     * @return true if execution is approved, false otherwise
     */
    public boolean evaluatePlanExecution(PlanExecution planExecution, String tenantId) {
        try {
            logger.debug("Evaluating guardrails for PlanExecution {}/{} for tenant: {}", 
                planExecution.getHeader().getName(), planExecution.getHeader().getExecId(), tenantId);
            
            // TODO: Implement actual guardrail evaluation
            // - Check plan complexity limits from planExecution.getResult().getNextTaskNamesList()
            // - Validate resource allocation
            // - Verify execution permissions from planExecution.getHeader()
            
            // For now, always approve
            logger.debug("PlanExecution {}/{} approved by guardrails for tenant: {}", 
                planExecution.getHeader().getName(), planExecution.getHeader().getExecId(), tenantId);
            return true;
            
        } catch (Exception e) {
            logger.error("Error evaluating guardrails for PlanExecution {}/{} for tenant {}: {}", 
                planExecution.getHeader().getName(), planExecution.getHeader().getExecId(), tenantId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Get execution status
     */
    public Map<String, Object> getExecutionStatus(String executionId) {
        return Map.of("status", executionStatus.getOrDefault(executionId, "unknown"));
    }
    
    /**
     * Abort an execution
     */
    public boolean abortExecution(String executionId, String reason) {
        logger.info("Aborting execution {}: {}", executionId, reason);
        executionStatus.put(executionId, "aborted");
        return true;
    }
} 
