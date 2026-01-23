package ai.eigloo.agentic.graphcomposer.service;

import ai.eigloo.agentic.graph.entity.ExecutorFileEntity;
import ai.eigloo.agentic.graphcomposer.dto.ExecutorFileDto;

import java.util.List;

/**
 * Service interface for ExecutorFile management operations.
 * Defines the business logic for file operations associated with plans and tasks.
 */
public interface FileService {

    /**
     * Get all files for a specific plan.
     *
     * @param planId the plan identifier
     * @return list of files associated with the plan
     * @throws PlanNotFoundException if plan is not found
     */
    List<ExecutorFileDto> getPlanFiles(String planId);

    /**
     * Get all files for a specific task.
     *
     * @param taskId the task identifier
     * @return list of files associated with the task
     * @throws TaskNotFoundException if task is not found
     */
    List<ExecutorFileDto> getTaskFiles(String taskId);

    /**
     * Update a file for a specific plan.
     *
     * @param planId the plan identifier
     * @param fileName the file name
     * @param content the file content
     * @return the updated file data
     * @throws PlanNotFoundException if plan is not found
     * @throws FileSizeExceededException if file content exceeds size limit
     */
    ExecutorFileDto updatePlanFile(String planId, String fileName, String content);

    /**
     * Update a file for a specific task.
     *
     * @param taskId the task identifier
     * @param fileName the file name
     * @param content the file content
     * @return the updated file data
     * @throws TaskNotFoundException if task is not found
     * @throws FileSizeExceededException if file content exceeds size limit
     */
    ExecutorFileDto updateTaskFile(String taskId, String fileName, String content);

    /**
     * Create default files for a new plan.
     *
     * @param planId the plan identifier
     * @param planName the plan name for template generation
     * @return list of created files
     */
    List<ExecutorFileDto> createDefaultPlanFiles(String planId, String planName);

    /**
     * Create default files for a new task.
     *
     * @param taskId the task identifier
     * @param taskName the task name for template generation
     * @return list of created files
     */
    List<ExecutorFileDto> createDefaultTaskFiles(String taskId, String taskName);

    /**
     * Convert ExecutorFileEntity to DTO.
     * This method is exposed for performance optimization in other services.
     *
     * @param entity the entity to convert
     * @return the converted DTO
     */
    ExecutorFileDto convertToDto(ExecutorFileEntity entity);

    // Exception classes
    class PlanNotFoundException extends RuntimeException {
        public PlanNotFoundException(String message) {
            super(message);
        }
    }

    class TaskNotFoundException extends RuntimeException {
        public TaskNotFoundException(String message) {
            super(message);
        }
    }

    class FileSizeExceededException extends RuntimeException {
        public FileSizeExceededException(String message) {
            super(message);
        }
    }
}