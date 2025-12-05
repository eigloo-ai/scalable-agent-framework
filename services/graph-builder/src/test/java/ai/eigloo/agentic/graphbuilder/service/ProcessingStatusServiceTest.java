package ai.eigloo.agentic.graphbuilder.service;

import ai.eigloo.agentic.graphbuilder.dto.ProcessingStatus;
import ai.eigloo.agentic.graphbuilder.dto.ProcessingStep;
import ai.eigloo.agentic.graphbuilder.entity.GraphBundleEntity;
import ai.eigloo.agentic.graphbuilder.entity.ProcessingStepEntity;
import ai.eigloo.agentic.graphbuilder.enums.BundleStatus;
import ai.eigloo.agentic.graphbuilder.enums.StepStatus;
import ai.eigloo.agentic.graphbuilder.repository.GraphBundleRepository;
import ai.eigloo.agentic.graphbuilder.repository.ProcessingStepRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProcessingStatusService.
 */
class ProcessingStatusServiceTest {
    
    @Mock
    private GraphBundleRepository graphBundleRepository;
    
    @Mock
    private ProcessingStepRepository processingStepRepository;
    
    private ProcessingStatusService processingStatusService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        processingStatusService = new ProcessingStatusService(graphBundleRepository, processingStepRepository);
    }
    
    @Test
    void createProcessingStep_ValidStep_ShouldCreateSuccessfully() {
        // Given
        String processId = "test-process-123";
        String stepName = "VALIDATION";
        StepStatus status = StepStatus.IN_PROGRESS;
        
        when(processingStepRepository.save(any(ProcessingStepEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        processingStatusService.createProcessingStep(processId, stepName, status);
        
        // Then
        ArgumentCaptor<ProcessingStepEntity> stepCaptor = ArgumentCaptor.forClass(ProcessingStepEntity.class);
        verify(processingStepRepository).save(stepCaptor.capture());
        
        ProcessingStepEntity savedStep = stepCaptor.getValue();
        assertThat(savedStep.getProcessId()).isEqualTo(processId);
        assertThat(savedStep.getStepName()).isEqualTo(stepName);
        assertThat(savedStep.getStatus()).isEqualTo(status.getValue());
        assertThat(savedStep.getErrorMessage()).isNull();
        assertThat(savedStep.getEndTime()).isNull(); // Should be null for IN_PROGRESS
    }
    
    @Test
    void createProcessingStep_CompletedStep_ShouldSetEndTime() {
        // Given
        String processId = "test-process-123";
        String stepName = "VALIDATION";
        StepStatus status = StepStatus.COMPLETED;
        
        when(processingStepRepository.save(any(ProcessingStepEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        processingStatusService.createProcessingStep(processId, stepName, status);
        
        // Then
        ArgumentCaptor<ProcessingStepEntity> stepCaptor = ArgumentCaptor.forClass(ProcessingStepEntity.class);
        verify(processingStepRepository).save(stepCaptor.capture());
        
        ProcessingStepEntity savedStep = stepCaptor.getValue();
        assertThat(savedStep.getStatus()).isEqualTo(status.getValue());
        assertThat(savedStep.getEndTime()).isNotNull(); // Should be set for COMPLETED
    }
    
    @Test
    void createProcessingStep_FailedStepWithError_ShouldSetErrorMessage() {
        // Given
        String processId = "test-process-123";
        String stepName = "VALIDATION";
        StepStatus status = StepStatus.FAILED;
        String errorMessage = "Validation failed";
        
        when(processingStepRepository.save(any(ProcessingStepEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        processingStatusService.createProcessingStep(processId, stepName, status, errorMessage);
        
        // Then
        ArgumentCaptor<ProcessingStepEntity> stepCaptor = ArgumentCaptor.forClass(ProcessingStepEntity.class);
        verify(processingStepRepository).save(stepCaptor.capture());
        
        ProcessingStepEntity savedStep = stepCaptor.getValue();
        assertThat(savedStep.getStatus()).isEqualTo(status.getValue());
        assertThat(savedStep.getErrorMessage()).isEqualTo(errorMessage);
        assertThat(savedStep.getEndTime()).isNotNull(); // Should be set for FAILED
    }
    
    @Test
    void updateProcessingStep_ExistingStep_ShouldUpdateSuccessfully() {
        // Given
        String processId = "test-process-123";
        String stepName = "VALIDATION";
        StepStatus newStatus = StepStatus.COMPLETED;
        
        ProcessingStepEntity existingStep = new ProcessingStepEntity("step-id", processId, stepName, StepStatus.IN_PROGRESS.getValue());
        when(processingStepRepository.findByProcessIdAndStepName(processId, stepName))
            .thenReturn(Optional.of(existingStep));
        when(processingStepRepository.save(any(ProcessingStepEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        processingStatusService.updateProcessingStep(processId, stepName, newStatus);
        
        // Then
        verify(processingStepRepository).save(existingStep);
        assertThat(existingStep.getStatus()).isEqualTo(newStatus.getValue());
        assertThat(existingStep.getEndTime()).isNotNull();
    }
    
    @Test
    void updateProcessingStep_NonExistentStep_ShouldCreateNewStep() {
        // Given
        String processId = "test-process-123";
        String stepName = "VALIDATION";
        StepStatus status = StepStatus.COMPLETED;
        
        when(processingStepRepository.findByProcessIdAndStepName(processId, stepName))
            .thenReturn(Optional.empty());
        when(processingStepRepository.save(any(ProcessingStepEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        processingStatusService.updateProcessingStep(processId, stepName, status);
        
        // Then
        ArgumentCaptor<ProcessingStepEntity> stepCaptor = ArgumentCaptor.forClass(ProcessingStepEntity.class);
        verify(processingStepRepository).save(stepCaptor.capture());
        
        ProcessingStepEntity savedStep = stepCaptor.getValue();
        assertThat(savedStep.getProcessId()).isEqualTo(processId);
        assertThat(savedStep.getStepName()).isEqualTo(stepName);
        assertThat(savedStep.getStatus()).isEqualTo(status.getValue());
    }
    
    @Test
    void updateBundleStatus_ExistingBundle_ShouldUpdateSuccessfully() {
        // Given
        String processId = "test-process-123";
        BundleStatus newStatus = BundleStatus.COMPLETED;
        
        GraphBundleEntity existingBundle = new GraphBundleEntity("bundle-id", "tenant-1", "test.zip", BundleStatus.EXTRACTING.getValue(), processId);
        when(graphBundleRepository.findByProcessId(processId)).thenReturn(Optional.of(existingBundle));
        when(graphBundleRepository.save(any(GraphBundleEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        processingStatusService.updateBundleStatus(processId, newStatus);
        
        // Then
        verify(graphBundleRepository).save(existingBundle);
        assertThat(existingBundle.getStatus()).isEqualTo(newStatus.getValue());
        assertThat(existingBundle.getCompletionTime()).isNotNull();
    }
    
    @Test
    void updateBundleStatus_NonExistentBundle_ShouldLogWarning() {
        // Given
        String processId = "non-existent-process";
        BundleStatus status = BundleStatus.COMPLETED;
        
        when(graphBundleRepository.findByProcessId(processId)).thenReturn(Optional.empty());
        
        // When
        processingStatusService.updateBundleStatus(processId, status);
        
        // Then
        verify(graphBundleRepository, never()).save(any(GraphBundleEntity.class));
    }
    
    @Test
    void updateBundleStatus_WithErrorMessage_ShouldSetErrorMessage() {
        // Given
        String processId = "test-process-123";
        BundleStatus status = BundleStatus.FAILED;
        String errorMessage = "Processing failed";
        
        GraphBundleEntity existingBundle = new GraphBundleEntity("bundle-id", "tenant-1", "test.zip", BundleStatus.EXTRACTING.getValue(), processId);
        when(graphBundleRepository.findByProcessId(processId)).thenReturn(Optional.of(existingBundle));
        when(graphBundleRepository.save(any(GraphBundleEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        processingStatusService.updateBundleStatus(processId, status, errorMessage);
        
        // Then
        verify(graphBundleRepository).save(existingBundle);
        assertThat(existingBundle.getStatus()).isEqualTo(status.getValue());
        assertThat(existingBundle.getErrorMessage()).isEqualTo(errorMessage);
        assertThat(existingBundle.getCompletionTime()).isNotNull();
    }
    
    @Test
    void getProcessingStatus_ExistingProcess_ShouldReturnStatus() {
        // Given
        String processId = "test-process-123";
        LocalDateTime uploadTime = LocalDateTime.now().minusMinutes(10);
        LocalDateTime completionTime = LocalDateTime.now();
        
        GraphBundleEntity bundle = new GraphBundleEntity("bundle-id", "tenant-1", "test.zip", BundleStatus.COMPLETED.getValue(), processId);
        bundle.setUploadTime(uploadTime);
        bundle.setCompletionTime(completionTime);
        
        List<ProcessingStepEntity> steps = Arrays.asList(
            new ProcessingStepEntity("step1", processId, "VALIDATION", StepStatus.COMPLETED.getValue()),
            new ProcessingStepEntity("step2", processId, "EXTRACTION", StepStatus.COMPLETED.getValue())
        );
        
        when(graphBundleRepository.findByProcessId(processId)).thenReturn(Optional.of(bundle));
        when(processingStepRepository.findByProcessIdOrderByStartTimeAsc(processId)).thenReturn(steps);
        
        // When
        ProcessingStatus result = processingStatusService.getProcessingStatus(processId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProcessId()).isEqualTo(processId);
        assertThat(result.getStatus()).isEqualTo(BundleStatus.COMPLETED.getValue());
        assertThat(result.getStartTime()).isEqualTo(uploadTime);
        assertThat(result.getEndTime()).isEqualTo(completionTime);
        assertThat(result.getSteps()).hasSize(2);
        assertThat(result.getSteps().get(0).getStepName()).isEqualTo("VALIDATION");
        assertThat(result.getSteps().get(1).getStepName()).isEqualTo("EXTRACTION");
    }
    
    @Test
    void getProcessingStatus_NonExistentProcess_ShouldThrowException() {
        // Given
        String processId = "non-existent-process";
        when(graphBundleRepository.findByProcessId(processId)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> processingStatusService.getProcessingStatus(processId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Process ID not found");
    }
    
    @Test
    void createBundleEntity_ValidData_ShouldCreateSuccessfully() {
        // Given
        String tenantId = "test-tenant";
        String fileName = "test.zip";
        String processId = "test-process-123";
        
        when(graphBundleRepository.save(any(GraphBundleEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        GraphBundleEntity result = processingStatusService.createBundleEntity(tenantId, fileName, processId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTenantId()).isEqualTo(tenantId);
        assertThat(result.getFileName()).isEqualTo(fileName);
        assertThat(result.getProcessId()).isEqualTo(processId);
        assertThat(result.getStatus()).isEqualTo(BundleStatus.UPLOADED.getValue());
        
        verify(graphBundleRepository).save(any(GraphBundleEntity.class));
    }
    
    @Test
    void getProcessingSteps_ExistingSteps_ShouldReturnSteps() {
        // Given
        String processId = "test-process-123";
        List<ProcessingStepEntity> stepEntities = Arrays.asList(
            new ProcessingStepEntity("step1", processId, "VALIDATION", StepStatus.COMPLETED.getValue()),
            new ProcessingStepEntity("step2", processId, "EXTRACTION", StepStatus.IN_PROGRESS.getValue())
        );
        
        when(processingStepRepository.findByProcessIdOrderByStartTimeAsc(processId)).thenReturn(stepEntities);
        
        // When
        List<ProcessingStep> result = processingStatusService.getProcessingSteps(processId);
        
        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getStepName()).isEqualTo("VALIDATION");
        assertThat(result.get(0).getStatus()).isEqualTo(StepStatus.COMPLETED.getValue());
        assertThat(result.get(1).getStepName()).isEqualTo("EXTRACTION");
        assertThat(result.get(1).getStatus()).isEqualTo(StepStatus.IN_PROGRESS.getValue());
    }
    
    @Test
    void isProcessFailed_FailedProcess_ShouldReturnTrue() {
        // Given
        String processId = "test-process-123";
        GraphBundleEntity bundle = new GraphBundleEntity("bundle-id", "tenant-1", "test.zip", BundleStatus.FAILED.getValue(), processId);
        when(graphBundleRepository.findByProcessId(processId)).thenReturn(Optional.of(bundle));
        
        // When
        boolean result = processingStatusService.isProcessFailed(processId);
        
        // Then
        assertThat(result).isTrue();
    }
    
    @Test
    void isProcessFailed_CompletedProcess_ShouldReturnFalse() {
        // Given
        String processId = "test-process-123";
        GraphBundleEntity bundle = new GraphBundleEntity("bundle-id", "tenant-1", "test.zip", BundleStatus.COMPLETED.getValue(), processId);
        when(graphBundleRepository.findByProcessId(processId)).thenReturn(Optional.of(bundle));
        
        // When
        boolean result = processingStatusService.isProcessFailed(processId);
        
        // Then
        assertThat(result).isFalse();
    }
    
    @Test
    void isProcessCompleted_CompletedProcess_ShouldReturnTrue() {
        // Given
        String processId = "test-process-123";
        GraphBundleEntity bundle = new GraphBundleEntity("bundle-id", "tenant-1", "test.zip", BundleStatus.COMPLETED.getValue(), processId);
        when(graphBundleRepository.findByProcessId(processId)).thenReturn(Optional.of(bundle));
        
        // When
        boolean result = processingStatusService.isProcessCompleted(processId);
        
        // Then
        assertThat(result).isTrue();
    }
    
    @Test
    void isProcessCompleted_FailedProcess_ShouldReturnFalse() {
        // Given
        String processId = "test-process-123";
        GraphBundleEntity bundle = new GraphBundleEntity("bundle-id", "tenant-1", "test.zip", BundleStatus.FAILED.getValue(), processId);
        when(graphBundleRepository.findByProcessId(processId)).thenReturn(Optional.of(bundle));
        
        // When
        boolean result = processingStatusService.isProcessCompleted(processId);
        
        // Then
        assertThat(result).isFalse();
    }
    
    @Test
    void getCurrentBundleStatus_ExistingBundle_ShouldReturnStatus() {
        // Given
        String processId = "test-process-123";
        GraphBundleEntity bundle = new GraphBundleEntity("bundle-id", "tenant-1", "test.zip", BundleStatus.EXTRACTING.getValue(), processId);
        when(graphBundleRepository.findByProcessId(processId)).thenReturn(Optional.of(bundle));
        
        // When
        BundleStatus result = processingStatusService.getCurrentBundleStatus(processId);
        
        // Then
        assertThat(result).isEqualTo(BundleStatus.EXTRACTING);
    }
    
    @Test
    void getCurrentBundleStatus_NonExistentBundle_ShouldReturnNull() {
        // Given
        String processId = "non-existent-process";
        when(graphBundleRepository.findByProcessId(processId)).thenReturn(Optional.empty());
        
        // When
        BundleStatus result = processingStatusService.getCurrentBundleStatus(processId);
        
        // Then
        assertThat(result).isNull();
    }
    
    @Test
    void markProcessAsFailed_ValidProcess_ShouldUpdateStatusAndStep() {
        // Given
        String processId = "test-process-123";
        String stepName = "VALIDATION";
        String errorMessage = "Validation failed";
        
        ProcessingStepEntity existingStep = new ProcessingStepEntity("step-id", processId, stepName, StepStatus.IN_PROGRESS.getValue());
        GraphBundleEntity bundle = new GraphBundleEntity("bundle-id", "tenant-1", "test.zip", BundleStatus.EXTRACTING.getValue(), processId);
        
        when(processingStepRepository.findByProcessIdAndStepName(processId, stepName))
            .thenReturn(Optional.of(existingStep));
        when(graphBundleRepository.findByProcessId(processId)).thenReturn(Optional.of(bundle));
        when(processingStepRepository.save(any(ProcessingStepEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(graphBundleRepository.save(any(GraphBundleEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        processingStatusService.markProcessAsFailed(processId, stepName, errorMessage);
        
        // Then
        verify(processingStepRepository).save(existingStep);
        verify(graphBundleRepository).save(bundle);
        assertThat(existingStep.getStatus()).isEqualTo(StepStatus.FAILED.getValue());
        assertThat(existingStep.getErrorMessage()).isEqualTo(errorMessage);
        assertThat(bundle.getStatus()).isEqualTo(BundleStatus.FAILED.getValue());
        assertThat(bundle.getErrorMessage()).isEqualTo(errorMessage);
    }
    
    @Test
    void markProcessAsCompleted_ValidProcess_ShouldUpdateStatus() {
        // Given
        String processId = "test-process-123";
        GraphBundleEntity bundle = new GraphBundleEntity("bundle-id", "tenant-1", "test.zip", BundleStatus.EXTRACTING.getValue(), processId);
        
        when(graphBundleRepository.findByProcessId(processId)).thenReturn(Optional.of(bundle));
        when(graphBundleRepository.save(any(GraphBundleEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        processingStatusService.markProcessAsCompleted(processId);
        
        // Then
        verify(graphBundleRepository).save(bundle);
        assertThat(bundle.getStatus()).isEqualTo(BundleStatus.COMPLETED.getValue());
        assertThat(bundle.getCompletionTime()).isNotNull();
    }
    
    @Test
    void cleanupProcessingRecords_ExistingRecords_ShouldDeleteAll() {
        // Given
        String processId = "test-process-123";
        
        List<ProcessingStepEntity> steps = Arrays.asList(
            new ProcessingStepEntity("step1", processId, "VALIDATION", StepStatus.FAILED.getValue()),
            new ProcessingStepEntity("step2", processId, "EXTRACTION", StepStatus.FAILED.getValue())
        );
        GraphBundleEntity bundle = new GraphBundleEntity("bundle-id", "tenant-1", "test.zip", BundleStatus.FAILED.getValue(), processId);
        
        when(processingStepRepository.findByProcessIdOrderByStartTimeAsc(processId)).thenReturn(steps);
        when(graphBundleRepository.findByProcessId(processId)).thenReturn(Optional.of(bundle));
        
        // When
        processingStatusService.cleanupProcessingRecords(processId);
        
        // Then
        verify(processingStepRepository).deleteAll(steps);
        verify(graphBundleRepository).delete(bundle);
    }
}