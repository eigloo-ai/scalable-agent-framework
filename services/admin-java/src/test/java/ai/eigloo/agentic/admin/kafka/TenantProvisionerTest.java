package ai.eigloo.agentic.admin.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ai.eigloo.agentic.admin.dto.DeleteTenantRequest;
import ai.eigloo.agentic.admin.dto.DisableTenantRequest;
import ai.eigloo.agentic.admin.dto.ProvisionTenantRequest;
import ai.eigloo.agentic.admin.service.TenantProvisioningOrchestrator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TenantProvisioner Kafka consumer.
 * Tests comprehensive input validation and error handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TenantProvisioner Tests")
class TenantProvisionerTest {
    
    @Mock
    private TenantProvisioningOrchestrator orchestrator;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @Mock
    private Acknowledgment acknowledgment;
    
    private TenantProvisioner tenantProvisioner;
    
    @BeforeEach
    void setUp() {
        tenantProvisioner = new TenantProvisioner(orchestrator, objectMapper);
    }
    
    @Test
    void handleTenantProvisioning_ProvisionTenant_Success() throws Exception {
        // Given
        String topic = "agentic-admin-provision-tenant";
        String key = "provision-tenant";
        String value = "{\"tenant_id\":\"test-tenant\",\"tenant_name\":\"Test Tenant\",\"provision_notes\":\"Test notes\"}";
        
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            topic, 0, 0L, key, value
        );
        
        ProvisionTenantRequest request = new ProvisionTenantRequest("test-tenant", "Test Tenant", "Test notes");
        when(objectMapper.readValue(value, ProvisionTenantRequest.class)).thenReturn(request);
        doNothing().when(orchestrator).provisionTenant(anyString(), anyString(), anyString());
        doNothing().when(acknowledgment).acknowledge();
        
        // When
        tenantProvisioner.handleTenantProvisioning(record, topic, acknowledgment);
        
        // Then
        verify(orchestrator).provisionTenant("test-tenant", "Test Tenant", "Test notes");
        verify(acknowledgment).acknowledge();
    }
    
    @Test
    void handleTenantProvisioning_DisableTenant_Success() throws Exception {
        // Given
        String topic = "agentic-admin-provision-tenant";
        String key = "disable-tenant";
        String value = "{\"tenant_id\":\"test-tenant\",\"disable_notes\":\"Test disable\"}";
        
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            topic, 0, 0L, key, value
        );
        
        DisableTenantRequest request = new DisableTenantRequest("test-tenant", "Test disable");
        when(objectMapper.readValue(value, DisableTenantRequest.class)).thenReturn(request);
        doNothing().when(orchestrator).disableTenant(anyString(), anyString());
        doNothing().when(acknowledgment).acknowledge();
        
        // When
        tenantProvisioner.handleTenantProvisioning(record, topic, acknowledgment);
        
        // Then
        verify(orchestrator).disableTenant("test-tenant", "Test disable");
        verify(acknowledgment).acknowledge();
    }
    
    @Test
    void handleTenantProvisioning_DeleteTenant_Success() throws Exception {
        // Given
        String topic = "agentic-admin-provision-tenant";
        String key = "delete-tenant";
        String value = "{\"tenant_id\":\"test-tenant\",\"deletion_notes\":\"Test deletion\"}";
        
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            topic, 0, 0L, key, value
        );
        
        DeleteTenantRequest request = new DeleteTenantRequest("test-tenant", "Test deletion");
        when(objectMapper.readValue(value, DeleteTenantRequest.class)).thenReturn(request);
        doNothing().when(orchestrator).deleteTenant(anyString(), anyString());
        doNothing().when(acknowledgment).acknowledge();
        
        // When
        tenantProvisioner.handleTenantProvisioning(record, topic, acknowledgment);
        
        // Then
        verify(orchestrator).deleteTenant("test-tenant", "Test deletion");
        verify(acknowledgment).acknowledge();
    }
    
    @Test
    void handleTenantProvisioning_InvalidMessageKey() throws Exception {
        // Given
        String topic = "agentic-admin-provision-tenant";
        String key = "unknown-operation";
        String value = "{\"tenant_id\":\"test-tenant\"}";
        
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            topic, 0, 0L, key, value
        );
        
        doNothing().when(acknowledgment).acknowledge();
        
        // When
        tenantProvisioner.handleTenantProvisioning(record, topic, acknowledgment);
        
        // Then
        verify(orchestrator, never()).provisionTenant(anyString(), anyString(), any());
        verify(orchestrator, never()).disableTenant(anyString(), any());
        verify(orchestrator, never()).deleteTenant(anyString(), any());
        verify(acknowledgment).acknowledge();
    }
    
    @Test
    void handleTenantProvisioning_NullMessageKey() throws Exception {
        // Given
        String topic = "agentic-admin-provision-tenant";
        String key = null;
        String value = "{\"tenant_id\":\"test-tenant\"}";
        
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            topic, 0, 0L, key, value
        );
        
        doNothing().when(acknowledgment).acknowledge();
        
        // When
        tenantProvisioner.handleTenantProvisioning(record, topic, acknowledgment);
        
        // Then
        verify(orchestrator, never()).provisionTenant(anyString(), anyString(), any());
        verify(orchestrator, never()).disableTenant(anyString(), any());
        verify(orchestrator, never()).deleteTenant(anyString(), any());
        verify(acknowledgment).acknowledge();
    }
    
    @Test
    void handleTenantProvisioning_JsonParsingError() throws Exception {
        // Given
        String topic = "agentic-admin-provision-tenant";
        String key = "provision-tenant";
        String value = "invalid-json";
        
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            topic, 0, 0L, key, value
        );
        
        when(objectMapper.readValue(value, ProvisionTenantRequest.class))
            .thenThrow(new JsonProcessingException("Invalid JSON") {});
        
        // When
        tenantProvisioner.handleTenantProvisioning(record, topic, acknowledgment);
        
        // Then
        verify(orchestrator, never()).provisionTenant(anyString(), anyString(), any());
        // Acknowledgment should not be called when exception occurs
        verify(acknowledgment, never()).acknowledge();
    }
    
    @Test
    void handleTenantProvisioning_InvalidMessageFormat() throws Exception {
        // Given
        String topic = "agentic-admin-provision-tenant";
        String key = "provision-tenant";
        String value = "{\"tenant_id\":\"test-tenant\"}"; // Missing required fields
        
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            topic, 0, 0L, key, value
        );
        
        ProvisionTenantRequest request = new ProvisionTenantRequest("test-tenant", null, null);
        when(objectMapper.readValue(value, ProvisionTenantRequest.class)).thenReturn(request);
        doNothing().when(acknowledgment).acknowledge();
        
        // When
        tenantProvisioner.handleTenantProvisioning(record, topic, acknowledgment);
        
        // Then
        verify(orchestrator, never()).provisionTenant(anyString(), anyString(), any());
        verify(acknowledgment).acknowledge();
    }
    

    
    @Test
    @DisplayName("Should handle provision tenant with invalid tenant ID")
    void handleProvisionTenant_InvalidTenantId() throws Exception {
        // Given
        String topic = "agentic-admin-provision-tenant";
        String key = "provision-tenant";
        String value = "{\"tenant_id\":\"@invalid\",\"tenant_name\":\"Test Tenant\",\"provision_notes\":\"Test notes\"}";
        
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            topic, 0, 0L, key, value
        );
        
        ProvisionTenantRequest request = new ProvisionTenantRequest("@invalid", "Test Tenant", "Test notes");
        when(objectMapper.readValue(value, ProvisionTenantRequest.class)).thenReturn(request);
        doNothing().when(acknowledgment).acknowledge();
        
        // When
        tenantProvisioner.handleTenantProvisioning(record, topic, acknowledgment);
        
        // Then
        verify(orchestrator, never()).provisionTenant(anyString(), anyString(), any());
        verify(acknowledgment).acknowledge();
    }
    
    @Test
    @DisplayName("Should handle provision tenant with invalid tenant name")
    void handleProvisionTenant_InvalidTenantName() throws Exception {
        // Given
        String topic = "agentic-admin-provision-tenant";
        String key = "provision-tenant";
        String value = "{\"tenant_id\":\"valid-tenant\",\"tenant_name\":\"@invalid\",\"provision_notes\":\"Test notes\"}";
        
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            topic, 0, 0L, key, value
        );
        
        ProvisionTenantRequest request = new ProvisionTenantRequest("valid-tenant", "@invalid", "Test notes");
        when(objectMapper.readValue(value, ProvisionTenantRequest.class)).thenReturn(request);
        doNothing().when(acknowledgment).acknowledge();
        
        // When
        tenantProvisioner.handleTenantProvisioning(record, topic, acknowledgment);
        
        // Then
        verify(orchestrator, never()).provisionTenant(anyString(), anyString(), any());
        verify(acknowledgment).acknowledge();
    }
    

    
    @Test
    @DisplayName("Should handle provision tenant with too long tenant ID")
    void handleProvisionTenant_TooLongTenantId() throws Exception {
        // Given
        String topic = "agentic-admin-provision-tenant";
        String key = "provision-tenant";
        String longTenantId = "a".repeat(51);
        String value = "{\"tenant_id\":\"" + longTenantId + "\",\"tenant_name\":\"Test Tenant\",\"provision_notes\":\"Test notes\"}";
        
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            topic, 0, 0L, key, value
        );
        
        ProvisionTenantRequest request = new ProvisionTenantRequest(longTenantId, "Test Tenant", "Test notes");
        when(objectMapper.readValue(value, ProvisionTenantRequest.class)).thenReturn(request);
        doNothing().when(acknowledgment).acknowledge();
        
        // When
        tenantProvisioner.handleTenantProvisioning(record, topic, acknowledgment);
        
        // Then
        verify(orchestrator, never()).provisionTenant(anyString(), anyString(), any());
        verify(acknowledgment).acknowledge();
    }
    
    @Test
    @DisplayName("Should handle provision tenant with too long notes")
    void handleProvisionTenant_TooLongNotes() throws Exception {
        // Given
        String topic = "agentic-admin-provision-tenant";
        String key = "provision-tenant";
        String longNotes = "a".repeat(513);
        String value = "{\"tenant_id\":\"valid-tenant\",\"tenant_name\":\"Test Tenant\",\"provision_notes\":\"" + longNotes + "\"}";
        
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            topic, 0, 0L, key, value
        );
        
        ProvisionTenantRequest request = new ProvisionTenantRequest("valid-tenant", "Test Tenant", longNotes);
        when(objectMapper.readValue(value, ProvisionTenantRequest.class)).thenReturn(request);
        doNothing().when(acknowledgment).acknowledge();
        
        // When
        tenantProvisioner.handleTenantProvisioning(record, topic, acknowledgment);
        
        // Then
        verify(orchestrator, never()).provisionTenant(anyString(), anyString(), any());
        verify(acknowledgment).acknowledge();
    }
    
    @Test
    @DisplayName("Should handle null message value")
    void handleTenantProvisioning_NullMessageValue() throws Exception {
        // Given
        String topic = "agentic-admin-provision-tenant";
        String key = "provision-tenant";
        String value = null;
        
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            topic, 0, 0L, key, value
        );
        
        doNothing().when(acknowledgment).acknowledge();
        
        // When
        tenantProvisioner.handleTenantProvisioning(record, topic, acknowledgment);
        
        // Then
        verify(orchestrator, never()).provisionTenant(anyString(), anyString(), any());
        verify(acknowledgment).acknowledge();
    }
    
    @Test
    @DisplayName("Should handle empty message value")
    void handleTenantProvisioning_EmptyMessageValue() throws Exception {
        // Given
        String topic = "agentic-admin-provision-tenant";
        String key = "provision-tenant";
        String value = "";
        
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            topic, 0, 0L, key, value
        );
        
        doNothing().when(acknowledgment).acknowledge();
        
        // When
        tenantProvisioner.handleTenantProvisioning(record, topic, acknowledgment);
        
        // Then
        verify(orchestrator, never()).provisionTenant(anyString(), anyString(), any());
        verify(acknowledgment).acknowledge();
    }
} 