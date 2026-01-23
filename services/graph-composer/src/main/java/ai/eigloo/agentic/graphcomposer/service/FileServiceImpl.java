package ai.eigloo.agentic.graphcomposer.service;

import ai.eigloo.agentic.graph.entity.ExecutorFileEntity;
import ai.eigloo.agentic.graph.entity.PlanEntity;
import ai.eigloo.agentic.graph.entity.TaskEntity;
import ai.eigloo.agentic.graph.repository.ExecutorFileRepository;
import ai.eigloo.agentic.graph.repository.PlanRepository;
import ai.eigloo.agentic.graph.repository.TaskRepository;
import ai.eigloo.agentic.graphcomposer.dto.ExecutorFileDto;
import ai.eigloo.agentic.graphcomposer.exception.FileProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of FileService for ExecutorFile management operations.
 * Provides business logic for file operations associated with plans and tasks.
 */
@Service
@Transactional
public class FileServiceImpl implements FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);

    private final ExecutorFileRepository executorFileRepository;
    private final PlanRepository planRepository;
    private final TaskRepository taskRepository;
    
    @Value("${graph-composer.max-file-size:1048576}")
    private int maxFileSize;

    @Autowired
    public FileServiceImpl(ExecutorFileRepository executorFileRepository,
                          PlanRepository planRepository,
                          TaskRepository taskRepository) {
        this.executorFileRepository = executorFileRepository;
        this.planRepository = planRepository;
        this.taskRepository = taskRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExecutorFileDto> getPlanFiles(String planId) {
        logger.debug("Getting files for plan: {}", planId);
        
        List<ExecutorFileEntity> files = executorFileRepository.findByPlanIdOrderByName(planId);
        
        return files.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExecutorFileDto> getTaskFiles(String taskId) {
        logger.debug("Getting files for task: {}", taskId);
        
        List<ExecutorFileEntity> files = executorFileRepository.findByTaskIdOrderByName(taskId);
        
        return files.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public ExecutorFileDto updatePlanFile(String planId, String fileName, String content) {
        logger.info("Updating file '{}' for plan: {}", fileName, planId);
        
        // Validate file size
        if (content != null && content.length() > maxFileSize) {
            throw new FileSizeExceededException("File content exceeds maximum size of " + maxFileSize + " bytes");
        }
        
        // Verify plan exists
        PlanEntity plan = planRepository.findById(planId)
                .orElseThrow(() -> new PlanNotFoundException("Plan not found: " + planId));
        
        // Find existing file or create new one
        ExecutorFileEntity file = executorFileRepository.findByPlanIdAndName(planId, fileName)
                .orElseGet(() -> {
                    ExecutorFileEntity newFile = new ExecutorFileEntity();
                    newFile.setId(UUID.randomUUID().toString());
                    newFile.setName(fileName);
                    newFile.setPlan(plan);
                    newFile.setCreationDate(LocalDateTime.now());
                    newFile.setVersion("1.0");
                    return newFile;
                });
        
        ExecutorFileEntity savedFile;
        try {
            // Update content and timestamp
            file.setContents(content);
            file.setUpdateDate(LocalDateTime.now());
            
            // Increment version for existing files
            if (file.getCreationDate().isBefore(LocalDateTime.now().minusSeconds(1))) {
                String currentVersion = file.getVersion();
                file.setVersion(incrementVersion(currentVersion));
            }
            
            savedFile = executorFileRepository.save(file);
            
            logger.info("Updated file '{}' for plan {} with version: {}", fileName, planId, savedFile.getVersion());
        } catch (Exception e) {
            logger.error("Failed to save file '{}' for plan {}: {}", fileName, planId, e.getMessage(), e);
            throw new FileProcessingException("Failed to save file", fileName, "save");
        }
        return convertToDto(savedFile);
    }

    @Override
    public ExecutorFileDto updateTaskFile(String taskId, String fileName, String content) {
        logger.info("Updating file '{}' for task: {}", fileName, taskId);
        
        // Validate file size
        if (content != null && content.length() > maxFileSize) {
            throw new FileSizeExceededException("File content exceeds maximum size of " + maxFileSize + " bytes");
        }
        
        // Verify task exists
        TaskEntity task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task not found: " + taskId));
        
        // Find existing file or create new one
        ExecutorFileEntity file = executorFileRepository.findByTaskIdAndName(taskId, fileName)
                .orElseGet(() -> {
                    ExecutorFileEntity newFile = new ExecutorFileEntity();
                    newFile.setId(UUID.randomUUID().toString());
                    newFile.setName(fileName);
                    newFile.setTask(task);
                    newFile.setCreationDate(LocalDateTime.now());
                    newFile.setVersion("1.0");
                    return newFile;
                });
        
        ExecutorFileEntity savedFile;
        try {
            // Update content and timestamp
            file.setContents(content);
            file.setUpdateDate(LocalDateTime.now());
            
            // Increment version for existing files
            if (file.getCreationDate().isBefore(LocalDateTime.now().minusSeconds(1))) {
                String currentVersion = file.getVersion();
                file.setVersion(incrementVersion(currentVersion));
            }
            
            savedFile = executorFileRepository.save(file);
            
            logger.info("Updated file '{}' for task {} with version: {}", fileName, taskId, savedFile.getVersion());
        } catch (Exception e) {
            logger.error("Failed to save file '{}' for task {}: {}", fileName, taskId, e.getMessage(), e);
            throw new FileProcessingException("Failed to save file", fileName, "save");
        }
        return convertToDto(savedFile);
    }

    @Override
    public List<ExecutorFileDto> createDefaultPlanFiles(String planId, String planName) {
        logger.info("Creating default files for plan: {} ({})", planName, planId);
        
        // Verify plan exists
        PlanEntity plan = planRepository.findById(planId)
                .orElseThrow(() -> new PlanNotFoundException("Plan not found: " + planId));
        
        // Create plan.py file
        String planPyContent = generatePlanTemplate(planName);
        ExecutorFileDto planPy = updatePlanFile(planId, "plan.py", planPyContent);
        
        // Create requirements.txt file
        String requirementsContent = generatePlanRequirements();
        ExecutorFileDto requirements = updatePlanFile(planId, "requirements.txt", requirementsContent);
        
        logger.info("Created default files for plan: {}", planName);
        return List.of(planPy, requirements);
    }

    @Override
    public List<ExecutorFileDto> createDefaultTaskFiles(String taskId, String taskName) {
        logger.info("Creating default files for task: {} ({})", taskName, taskId);
        
        // Verify task exists
        TaskEntity task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task not found: " + taskId));
        
        // Create task.py file
        String taskPyContent = generateTaskTemplate(taskName);
        ExecutorFileDto taskPy = updateTaskFile(taskId, "task.py", taskPyContent);
        
        // Create requirements.txt file
        String requirementsContent = generateTaskRequirements();
        ExecutorFileDto requirements = updateTaskFile(taskId, "requirements.txt", requirementsContent);
        
        logger.info("Created default files for task: {}", taskName);
        return List.of(taskPy, requirements);
    }

    @Override
    public ExecutorFileDto convertToDto(ExecutorFileEntity entity) {
        return new ExecutorFileDto(
                entity.getName(),
                entity.getContents(),
                entity.getCreationDate(),
                entity.getVersion(),
                entity.getUpdateDate()
        );
    }

    private String incrementVersion(String currentVersion) {
        try {
            String[] parts = currentVersion.split("\\.");
            if (parts.length >= 2) {
                int major = Integer.parseInt(parts[0]);
                int minor = Integer.parseInt(parts[1]);
                return major + "." + (minor + 1);
            } else {
                return "1.1";
            }
        } catch (NumberFormatException e) {
            return "1.1";
        }
    }

    private String generatePlanTemplate(String planName) {
        return String.format("""
                from agentic_common.pb import PlanInput, PlanResult
                
                def plan(plan_input: PlanInput) -> PlanResult:
                    \"\"\"
                    Plan implementation for %s.
                    
                    Args:
                        plan_input: Input data for the plan execution
                        
                    Returns:
                        PlanResult: Result of the plan execution
                    \"\"\"
                    # TODO: Implement plan logic here
                    
                    # Example implementation:
                    # result = PlanResult()
                    # result.success = True
                    # result.message = "Plan executed successfully"
                    # return result
                    
                    raise NotImplementedError("Plan implementation not yet completed")
                """, planName);
    }

    private String generateTaskTemplate(String taskName) {
        return String.format("""
                from agentic_common.pb import TaskInput, TaskResult
                
                def task(task_input: TaskInput) -> TaskResult:
                    \"\"\"
                    Task implementation for %s.
                    
                    Args:
                        task_input: Input data for the task execution
                        
                    Returns:
                        TaskResult: Result of the task execution
                    \"\"\"
                    # TODO: Implement task logic here
                    
                    # Example implementation:
                    # result = TaskResult()
                    # result.success = True
                    # result.message = "Task executed successfully"
                    # return result
                    
                    raise NotImplementedError("Task implementation not yet completed")
                """, taskName);
    }

    private String generatePlanRequirements() {
        return """
                # Dependencies for plan execution
                # Add any additional Python packages needed for this plan
                """;
    }

    private String generateTaskRequirements() {
        return """
                # Dependencies for task execution
                # Add any additional Python packages needed for this task
                """;
    }
}