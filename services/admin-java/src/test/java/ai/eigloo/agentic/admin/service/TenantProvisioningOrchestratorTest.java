package ai.eigloo.agentic.admin.service;

import ai.eigloo.agentic.admin.entity.TenantInfoEntity;
import ai.eigloo.agentic.admin.repository.TenantInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TenantProvisioningOrchestrator.
 */
@ExtendWith(MockitoExtension.class)
class TenantProvisioningOrchestratorTest {
    
    @Mock
    private TenantInfoRepository tenantInfoRepository;
    
    @Mock
    private KafkaTopicManager kafkaTopicManager;
    
    private TenantProvisioningOrchestrator orchestrator;
    
    @BeforeEach
    void setUp() {
        orchestrator = new TenantProvisioningOrchestrator(tenantInfoRepository, kafkaTopicManager);
    }
    
    @Test
    void provisionTenant_Success() {
        // Given
        String tenantId = "test-tenant";
        String tenantName = "Test Tenant";
        String provisionNotes = "Test provisioning";
        
        when(tenantInfoRepository.existsByTenantId(tenantId)).thenReturn(false);
        doNothing().when(kafkaTopicManager).createTenantTopics(tenantId);
        when(tenantInfoRepository.save(any(TenantInfoEntity.class))).thenAnswer(invocation -> {
            TenantInfoEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID().toString());
            return entity;
        });
        
        // When
        orchestrator.provisionTenant(tenantId, tenantName, provisionNotes);
        
        // Then
        verify(kafkaTopicManager).createTenantTopics(tenantId);
        
        ArgumentCaptor<TenantInfoEntity> entityCaptor = ArgumentCaptor.forClass(TenantInfoEntity.class);
        verify(tenantInfoRepository).save(entityCaptor.capture());
        
        TenantInfoEntity savedEntity = entityCaptor.getValue();
        assertEquals(tenantId, savedEntity.getTenantId());
        assertEquals(tenantName, savedEntity.getTenantName());
        assertEquals(provisionNotes, savedEntity.getProvisionNotes());
        assertTrue(savedEntity.getEnabled());
        assertNotNull(savedEntity.getCreationTime());
    }
    
    @Test
    void provisionTenant_TenantAlreadyExists() {
        // Given
        String tenantId = "existing-tenant";
        String tenantName = "Existing Tenant";
        
        when(tenantInfoRepository.existsByTenantId(tenantId)).thenReturn(true);
        
        // When
        orchestrator.provisionTenant(tenantId, tenantName, null);
        
        // Then
        verify(kafkaTopicManager, never()).createTenantTopics(anyString());
        verify(tenantInfoRepository, never()).save(any(TenantInfoEntity.class));
    }
    
    @Test
    void provisionTenant_KafkaFailure() {
        // Given
        String tenantId = "test-tenant";
        String tenantName = "Test Tenant";
        
        when(tenantInfoRepository.existsByTenantId(tenantId)).thenReturn(false);
        doThrow(new RuntimeException("Kafka error")).when(kafkaTopicManager).createTenantTopics(tenantId);
        
        // When & Then
        assertThrows(RuntimeException.class, () -> 
            orchestrator.provisionTenant(tenantId, tenantName, null));
        
        verify(tenantInfoRepository, never()).save(any(TenantInfoEntity.class));
    }
    
    @Test
    void disableTenant_Success() {
        // Given
        String tenantId = "test-tenant";
        String disableNotes = "Test disable";
        
        TenantInfoEntity existingEntity = new TenantInfoEntity();
        existingEntity.setId(UUID.randomUUID().toString());
        existingEntity.setTenantId(tenantId);
        existingEntity.setTenantName("Test Tenant");
        existingEntity.setEnabled(true);
        existingEntity.setCreationTime(Instant.now());
        
        when(tenantInfoRepository.findByTenantId(tenantId)).thenReturn(Optional.of(existingEntity));
        when(tenantInfoRepository.save(any(TenantInfoEntity.class))).thenReturn(existingEntity);
        
        // When
        orchestrator.disableTenant(tenantId, disableNotes);
        
        // Then
        ArgumentCaptor<TenantInfoEntity> entityCaptor = ArgumentCaptor.forClass(TenantInfoEntity.class);
        verify(tenantInfoRepository).save(entityCaptor.capture());
        
        TenantInfoEntity savedEntity = entityCaptor.getValue();
        assertFalse(savedEntity.getEnabled());
        assertEquals(disableNotes, savedEntity.getDisableNotes());
        assertNotNull(savedEntity.getDisableTime());
    }
    
    @Test
    void disableTenant_TenantNotFound() {
        // Given
        String tenantId = "non-existent-tenant";
        
        when(tenantInfoRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(RuntimeException.class, () -> 
            orchestrator.disableTenant(tenantId, null));
        
        verify(tenantInfoRepository, never()).save(any(TenantInfoEntity.class));
    }
    
    @Test
    void disableTenant_AlreadyDisabled() {
        // Given
        String tenantId = "disabled-tenant";
        
        TenantInfoEntity existingEntity = new TenantInfoEntity();
        existingEntity.setId(UUID.randomUUID().toString());
        existingEntity.setTenantId(tenantId);
        existingEntity.setEnabled(false);
        existingEntity.setDisableTime(Instant.now());
        
        when(tenantInfoRepository.findByTenantId(tenantId)).thenReturn(Optional.of(existingEntity));
        
        // When
        orchestrator.disableTenant(tenantId, null);
        
        // Then
        verify(tenantInfoRepository, never()).save(any(TenantInfoEntity.class));
    }
    
    @Test
    void deleteTenant_Success() {
        // Given
        String tenantId = "test-tenant";
        String deletionNotes = "Test deletion";
        
        TenantInfoEntity existingEntity = new TenantInfoEntity();
        existingEntity.setId(UUID.randomUUID().toString());
        existingEntity.setTenantId(tenantId);
        existingEntity.setTenantName("Test Tenant");
        
        when(tenantInfoRepository.findByTenantId(tenantId)).thenReturn(Optional.of(existingEntity));
        doNothing().when(kafkaTopicManager).deleteTenantTopics(tenantId);
        doNothing().when(tenantInfoRepository).delete(existingEntity);
        
        // When
        orchestrator.deleteTenant(tenantId, deletionNotes);
        
        // Then
        verify(kafkaTopicManager).deleteTenantTopics(tenantId);
        verify(tenantInfoRepository).delete(existingEntity);
    }
    
    @Test
    void deleteTenant_TenantNotFound() {
        // Given
        String tenantId = "non-existent-tenant";
        
        when(tenantInfoRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(RuntimeException.class, () -> 
            orchestrator.deleteTenant(tenantId, null));
        
        verify(kafkaTopicManager, never()).deleteTenantTopics(anyString());
        verify(tenantInfoRepository, never()).delete(any(TenantInfoEntity.class));
    }
    
    @Test
    void deleteTenant_KafkaFailure() {
        // Given
        String tenantId = "test-tenant";
        
        TenantInfoEntity existingEntity = new TenantInfoEntity();
        existingEntity.setId(UUID.randomUUID().toString());
        existingEntity.setTenantId(tenantId);
        
        when(tenantInfoRepository.findByTenantId(tenantId)).thenReturn(Optional.of(existingEntity));
        doThrow(new RuntimeException("Kafka error")).when(kafkaTopicManager).deleteTenantTopics(tenantId);
        
        // When & Then
        assertThrows(RuntimeException.class, () -> 
            orchestrator.deleteTenant(tenantId, null));
        
        verify(tenantInfoRepository, never()).delete(any(TenantInfoEntity.class));
    }
    
    @Test
    void enableTenant_Success() {
        // Given
        String tenantId = "disabled-tenant";
        String enableNotes = "Test enable";
        
        TenantInfoEntity existingEntity = new TenantInfoEntity();
        existingEntity.setId(UUID.randomUUID().toString());
        existingEntity.setTenantId(tenantId);
        existingEntity.setEnabled(false);
        existingEntity.setDisableTime(Instant.now());
        existingEntity.setDisableNotes("Previous disable");
        
        when(tenantInfoRepository.findByTenantId(tenantId)).thenReturn(Optional.of(existingEntity));
        when(kafkaTopicManager.allTenantTopicsExist(tenantId)).thenReturn(true);
        when(tenantInfoRepository.save(any(TenantInfoEntity.class))).thenReturn(existingEntity);
        
        // When
        orchestrator.enableTenant(tenantId, enableNotes);
        
        // Then
        ArgumentCaptor<TenantInfoEntity> entityCaptor = ArgumentCaptor.forClass(TenantInfoEntity.class);
        verify(tenantInfoRepository).save(entityCaptor.capture());
        
        TenantInfoEntity savedEntity = entityCaptor.getValue();
        assertTrue(savedEntity.getEnabled());
        assertNull(savedEntity.getDisableTime());
        assertNull(savedEntity.getDisableNotes());
    }
    
    @Test
    void enableTenant_AlreadyEnabled() {
        // Given
        String tenantId = "enabled-tenant";
        
        TenantInfoEntity existingEntity = new TenantInfoEntity();
        existingEntity.setId(UUID.randomUUID().toString());
        existingEntity.setTenantId(tenantId);
        existingEntity.setEnabled(true);
        
        when(tenantInfoRepository.findByTenantId(tenantId)).thenReturn(Optional.of(existingEntity));
        
        // When
        orchestrator.enableTenant(tenantId, null);
        
        // Then
        verify(tenantInfoRepository, never()).save(any(TenantInfoEntity.class));
    }
} 