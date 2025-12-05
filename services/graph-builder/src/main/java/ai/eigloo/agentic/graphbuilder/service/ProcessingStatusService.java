package ai.eigloo.agentic.graphbuilder.service;

import ai.eigloo.agentic.graphbuilder.dto.ProcessingStatus;
import ai.eigloo.agentic.graphbuilder.dto.ProcessingStep;
import ai.eigloo.agentic.graphbuilder.entity.GraphBundleEntity;
import ai.eigloo.agentic.graphbuilder.entity.ProcessingStepEntity;
import ai.eigloo.agentic.graphbuilder.enums.BundleStatus;
import ai.eigloo.agentic.graphbuilder.enums.StepStatus;
import ai.eigloo.agentic.graphbuilder.repository.GraphBundleRepository;
import ai.eigloo.agentic.graphbuilder.repository.ProcessingStepRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service responsible for tracking and managing processing status for bundle uploads.
 */
@Service
public class ProcessingStatusService {
    
    private static final Logger logger = LoggerFactory.getLogger(ProcessingStatusService.class);
    
    private final GraphBundleRepository graphBundleRepository;
    private final ProcessingStepRepository processingStepRepository;
    
    @Autowired
    public ProcessingStatusService(GraphBundleRepository graphBundleRepository,
                                  ProcessingStepRepository processingStepRepository) {
        this.graphBundleRepository = graphBundleRepository;
        this.processingStepRepository = processingStepRepository;
    }
    
    /**
     * Creates a new processing step entity.
     * 
     * @param processId The process ID
     * @param stepName The name of the processing step
     * @param status The status of the step
     */
    public void createProcessingStep(String processId, String stepName, StepStatus status) {
        createProcessingStep(processId, stepName, status, null);
    }
    
    /**
     * Creates a new processing step entity with an optional error message.
     * 
     * @param processId The process ID
     * @param stepName The name of the processing step
     * @param status The status of the step
     * @param errorMessage Optional error message for failed steps
     */
    public void createProcessingStep(String processId, String stepName, StepStatus status, String errorMessage) {
        String stepId = UUID.randomUUID().toString();
        ProcessingStepEntity step = new ProcessingStepEntity(stepId, processId, stepName, status.getValue());
        
        if (errorMessage != null) {
            step.setErrorMessage(errorMessage);
        }
        
        if (status == StepStatus.COMPLETED || status == StepStatus.FAILED) {
            step.setEndTime(LocalDateTime.now());
        }
        
        processingStepRepository.save(step);
        logger.debug("Created processing step {} with status {} for process {}", stepName, status, processId);
    }
    
    /**
     * Updates an existing processing step status.
     * 
     * @param processId The process ID
     * @param stepName The name of the processing step
     * @param status The new status
     */
    public void updateProcessingStep(String processId, String stepName, StepStatus status) {
        updateProcessingStep(processId, stepName, status, null);
    }
    
    /**
     * Updates an existing processing step status with an optional error message.
     * 
     * @param processId The process ID
     * @param stepName The name of the processing step
     * @param status The new status
     * @param errorMessage Optional error message for failed steps
     */
    public void updateProcessingStep(String processId, String stepName, StepStatus status, String errorMessage) {
        Optional<ProcessingStepEntity> stepOpt = processingStepRepository.findByProcessIdAndStepName(processId, stepName);
        
        if (stepOpt.isPresent()) {
            ProcessingStepEntity step = stepOpt.get();
            step.setStatus(status.getValue());
            
            if (errorMessage != null) {
                step.setErrorMessage(errorMessage);
            }
            
            if (status == StepStatus.COMPLETED || status == StepStatus.FAILED) {
                step.setEndTime(LocalDateTime.now());
            }
            
            processingStepRepository.save(step);
            logger.debug("Updated processing step {} to status {} for process {}", stepName, status, processId);
        } else {
            // Create the step if it doesn't exist
            createProcessingStep(processId, stepName, status, errorMessage);
        }
    }
    
    /**
     * Updates the bundle status.
     * 
     * @param processId The process ID
     * @param status The new bundle status
     */
    public void updateBundleStatus(String processId, BundleStatus status) {
        updateBundleStatus(processId, status, null);
    }
    
    /**
     * Updates the bundle status with an optional error message.
     * 
     * @param processId The process ID
     * @param status The new bundle status
     * @param errorMessage Optional error message for failed bundles
     */
    public void updateBundleStatus(String processId, BundleStatus status, String errorMessage) {
        Optional<GraphBundleEntity> bundleOpt = graphBundleRepository.findByProcessId(processId);
        if (bundleOpt.isPresent()) {
            GraphBundleEntity bundle = bundleOpt.get();
            bundle.setStatus(status.getValue());
            bundle.setErrorMessage(errorMessage);
            
            if (status == BundleStatus.COMPLETED || status == BundleStatus.FAILED) {
                bundle.setCompletionTime(LocalDateTime.now());
            }
            
            graphBundleRepository.save(bundle);
            logger.debug("Updated bundle status to {} for process {}", status, processId);
        } else {
            logger.warn("Bundle not found for process ID: {}", processId);
        }
    }
    
    /**
     * Gets the processing status for a given process ID.
     * 
     * @param processId The process ID to query
     * @return The processing status
     * @throws IllegalArgumentException if process ID is not found
     */
    public ProcessingStatus getProcessingStatus(String processId) {
        logger.debug("Getting processing status for process ID {}", processId);
        
        Optional<GraphBundleEntity> bundleOpt = graphBundleRepository.findByProcessId(processId);
        if (bundleOpt.isEmpty()) {
            throw new IllegalArgumentException("Process ID not found: " + processId);
        }
        
        GraphBundleEntity bundle = bundleOpt.get();
        List<ProcessingStepEntity> stepEntities = processingStepRepository.findByProcessIdOrderByStartTimeAsc(processId);
        
        ProcessingStatus status = new ProcessingStatus(processId, bundle.getStatus());
        status.setErrorMessage(bundle.getErrorMessage());
        status.setStartTime(bundle.getUploadTime());
        status.setEndTime(bundle.getCompletionTime());
        
        List<ProcessingStep> steps = stepEntities.stream()
            .map(this::convertToProcessingStep)
            .collect(Collectors.toList());
        status.setSteps(steps);
        
        return status;
    }
    
    /**
     * Creates a bundle entity for tracking.
     * 
     * @param tenantId The tenant ID
     * @param fileName The uploaded file name
     * @param processId The process ID
     * @return The created bundle entity
     */
    public GraphBundleEntity createBundleEntity(String tenantId, String fileName, String processId) {
        String bundleId = UUID.randomUUID().toString();
        
        GraphBundleEntity bundle = new GraphBundleEntity(
            bundleId, 
            tenantId, 
            fileName, 
            BundleStatus.UPLOADED.getValue(), 
            processId
        );
        
        GraphBundleEntity savedBundle = graphBundleRepository.save(bundle);
        logger.debug("Created bundle entity with ID {} for process {}", bundleId, processId);
        return savedBundle;
    }
    
    /**
     * Gets all processing steps for a process ID.
     * 
     * @param processId The process ID
     * @return List of processing steps
     */
    public List<ProcessingStep> getProcessingSteps(String processId) {
        List<ProcessingStepEntity> stepEntities = processingStepRepository.findByProcessIdOrderByStartTimeAsc(processId);
        
        return stepEntities.stream()
            .map(this::convertToProcessingStep)
            .collect(Collectors.toList());
    }
    
    /**
     * Checks if a process has failed.
     * 
     * @param processId The process ID
     * @return true if the process has failed, false otherwise
     */
    public boolean isProcessFailed(String processId) {
        Optional<GraphBundleEntity> bundleOpt = graphBundleRepository.findByProcessId(processId);
        if (bundleOpt.isPresent()) {
            return BundleStatus.FAILED.getValue().equals(bundleOpt.get().getStatus());
        }
        return false;
    }
    
    /**
     * Checks if a process has completed successfully.
     * 
     * @param processId The process ID
     * @return true if the process has completed successfully, false otherwise
     */
    public boolean isProcessCompleted(String processId) {
        Optional<GraphBundleEntity> bundleOpt = graphBundleRepository.findByProcessId(processId);
        if (bundleOpt.isPresent()) {
            return BundleStatus.COMPLETED.getValue().equals(bundleOpt.get().getStatus());
        }
        return false;
    }
    
    /**
     * Gets the current bundle status for a process.
     * 
     * @param processId The process ID
     * @return The current bundle status, or null if not found
     */
    public BundleStatus getCurrentBundleStatus(String processId) {
        Optional<GraphBundleEntity> bundleOpt = graphBundleRepository.findByProcessId(processId);
        if (bundleOpt.isPresent()) {
            String statusValue = bundleOpt.get().getStatus();
            return BundleStatus.fromValue(statusValue);
        }
        return null;
    }
    
    /**
     * Marks a process as failed with an error message.
     * Uses transaction to ensure atomicity.
     * 
     * @param processId The process ID
     * @param stepName The step that failed
     * @param errorMessage The error message
     */
    @Transactional
    public void markProcessAsFailed(String processId, String stepName, String errorMessage) {
        String correlationId = getOrCreateCorrelationId();
        
        try {
            logger.info("Marking process {} as failed at step {} [correlationId={}]", processId, stepName, correlationId);
            
            // Update the failed step
            updateProcessingStep(processId, stepName, StepStatus.FAILED, errorMessage);
            
            // Update the overall bundle status
            updateBundleStatus(processId, BundleStatus.FAILED, errorMessage);
            
            logger.info("Successfully marked process {} as failed at step {} [correlationId={}]", processId, stepName, correlationId);
            
        } catch (Exception e) {
            logger.error("Failed to mark process {} as failed [correlationId={}]: {}", processId, correlationId, e.getMessage(), e);
            // Re-throw to trigger transaction rollback
            throw new RuntimeException("Failed to mark process as failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Marks a process as completed successfully.
     * Uses transaction to ensure atomicity.
     * 
     * @param processId The process ID
     */
    @Transactional
    public void markProcessAsCompleted(String processId) {
        String correlationId = getOrCreateCorrelationId();
        
        try {
            logger.info("Marking process {} as completed [correlationId={}]", processId, correlationId);
            
            updateBundleStatus(processId, BundleStatus.COMPLETED);
            
            logger.info("Successfully marked process {} as completed [correlationId={}]", processId, correlationId);
            
        } catch (Exception e) {
            logger.error("Failed to mark process {} as completed [correlationId={}]: {}", processId, correlationId, e.getMessage(), e);
            // Re-throw to trigger transaction rollback
            throw new RuntimeException("Failed to mark process as completed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Cleans up processing records for a failed process.
     * This method removes all processing steps and bundle records for cleanup.
     * 
     * @param processId The process ID to clean up
     */
    @Transactional
    public void cleanupProcessingRecords(String processId) {
        String correlationId = getOrCreateCorrelationId();
        
        try {
            logger.info("Cleaning up processing records for process {} [correlationId={}]", processId, correlationId);
            
            // Delete processing steps
            List<ProcessingStepEntity> steps = processingStepRepository.findByProcessIdOrderByStartTimeAsc(processId);
            if (!steps.isEmpty()) {
                processingStepRepository.deleteAll(steps);
                logger.debug("Deleted {} processing steps for process {} [correlationId={}]", steps.size(), processId, correlationId);
            }
            
            // Delete bundle record
            Optional<GraphBundleEntity> bundleOpt = graphBundleRepository.findByProcessId(processId);
            if (bundleOpt.isPresent()) {
                graphBundleRepository.delete(bundleOpt.get());
                logger.debug("Deleted bundle record for process {} [correlationId={}]", processId, correlationId);
            }
            
            logger.info("Successfully cleaned up processing records for process {} [correlationId={}]", processId, correlationId);
            
        } catch (Exception e) {
            logger.error("Failed to cleanup processing records for process {} [correlationId={}]: {}", processId, correlationId, e.getMessage(), e);
            // Re-throw to trigger transaction rollback
            throw new RuntimeException("Failed to cleanup processing records: " + e.getMessage(), e);
        }
    }
    
    /**
     * Converts a ProcessingStepEntity to a ProcessingStep DTO.
     */
    private ProcessingStep convertToProcessingStep(ProcessingStepEntity entity) {
        ProcessingStep step = new ProcessingStep(entity.getStepName(), entity.getStatus());
        step.setStartTime(entity.getStartTime());
        step.setEndTime(entity.getEndTime());
        step.setErrorMessage(entity.getErrorMessage());
        return step;
    }
    
    /**
     * Gets or creates a correlation ID for error tracking.
     */
    private String getOrCreateCorrelationId() {
        String mdcId = MDC.get("correlationId");
        if (mdcId != null && !mdcId.trim().isEmpty()) {
            return mdcId;
        }
        
        String newId = UUID.randomUUID().toString();
        MDC.put("correlationId", newId);
        return newId;
    }
}