package ai.eigloo.agentic.controlplane.service;

import ai.eigloo.agentic.graph.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for looking up task metadata by task names.
 * 
 * This service is responsible for retrieving task information from the graph/database
 * based on task names. Currently implemented as a stub that returns mock data.
 * 
 * TODO: Replace with actual graph/database lookup logic
 */
@Service
public class TaskLookupService {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskLookupService.class);
    
    /**
     * Look up tasks by their names for a specific tenant.
     * 
     * @param taskNames list of task names to look up
     * @param tenantId the tenant identifier
     * @return list of Task objects with metadata
     */
    public List<Task> lookupTasksByNames(List<String> taskNames, String tenantId) {
        logger.info("Looking up {} tasks for tenant {}: {}", taskNames.size(), tenantId, taskNames);
        
        try {
            // TODO: Replace with actual graph/database lookup logic
            // For now, return mock Task objects with the provided names
            List<Task> tasks = taskNames.stream()
                .map(taskName -> {
                    // Create mock task with default values
                    Path mockTaskSource = Path.of("/mock/task/source/" + taskName);
                    String mockUpstreamPlanId = "mock-upstream-plan-" + taskName;
                    return new Task(taskName, taskName, mockTaskSource, mockUpstreamPlanId, List.of());
                })
                .collect(Collectors.toList());
            
            logger.debug("Successfully looked up {} tasks for tenant {}", tasks.size(), tenantId);
            return tasks;
            
        } catch (Exception e) {
            logger.error("Failed to lookup tasks for tenant {}: {}", tenantId, e.getMessage(), e);
            throw new RuntimeException("Task lookup failed for tenant " + tenantId, e);
        }
    }
} 