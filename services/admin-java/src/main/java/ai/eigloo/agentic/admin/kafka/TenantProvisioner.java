package ai.eigloo.agentic.admin.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ai.eigloo.agentic.admin.dto.DeleteTenantRequest;
import ai.eigloo.agentic.admin.dto.DisableTenantRequest;
import ai.eigloo.agentic.admin.dto.ProvisionTenantRequest;
import ai.eigloo.agentic.admin.service.TenantProvisioningOrchestrator;
import ai.eigloo.agentic.admin.validation.TenantValidationUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for tenant provisioning messages.
 * 
 * Listens to tenant provisioning topics and orchestrates tenant lifecycle operations.
 */
@Component
public class TenantProvisioner {
    
    private static final Logger logger = LoggerFactory.getLogger(TenantProvisioner.class);
    
    private final TenantProvisioningOrchestrator orchestrator;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public TenantProvisioner(TenantProvisioningOrchestrator orchestrator, ObjectMapper objectMapper) {
        this.orchestrator = orchestrator;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Handle tenant provisioning messages from Kafka.
     * 
     * @param record the Kafka consumer record
     * @param topic the topic name
     * @param acknowledgment the acknowledgment
     */
    @KafkaListener(
        topics = "agentic-admin-provision-tenant",
        groupId = "admin-tenant-provisioner",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleTenantProvisioning(
            ConsumerRecord<String, String> record,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {
        
        try {
            logger.debug("Received tenant provisioning message from topic: {}", topic);
            
            String messageKey = record.key();
            String messageValue = record.value();
            
            // Validate basic message structure
            if (messageKey == null || messageValue == null) {
                logger.error("Invalid message received: key={}, value={}", messageKey, messageValue);
                acknowledgment.acknowledge();
                return;
            }
            

            
            // Handle different message types based on key
            switch (messageKey) {
                case "provision-tenant":
                    handleProvisionTenant(messageValue);
                    break;
                case "disable-tenant":
                    handleDisableTenant(messageValue);
                    break;
                case "delete-tenant":
                    handleDeleteTenant(messageValue);
                    break;
                default:
                    logger.warn("Unknown message key: {}", messageKey);
            }
            
            // Acknowledge the message
            acknowledgment.acknowledge();
            
        } catch (IllegalArgumentException e) {
            logger.error("Validation error in tenant provisioning message from topic {}: {}", topic, e.getMessage(), e);
            // Acknowledge validation errors to avoid infinite retry
            acknowledgment.acknowledge();
        } catch (RuntimeException e) {
            logger.error("Runtime error processing tenant provisioning message from topic {}: {}", topic, e.getMessage(), e);
            // Don't acknowledge runtime errors to allow retry
        }
    }
    
    /**
     * Handle provision tenant message.
     * 
     * @param messageValue the JSON message value
     */
    private void handleProvisionTenant(String messageValue) {
        try {
            // Deserialize the request
            ProvisionTenantRequest request = objectMapper.readValue(messageValue, ProvisionTenantRequest.class);
            
            // Comprehensive validation
            TenantValidationUtils.ValidationResult validationResult = TenantValidationUtils.validateProvisionTenantRequest(request);
            if (!validationResult.isValid()) {
                logger.error("Invalid provision tenant message: {}", validationResult.getErrorMessage());
                return;
            }
            

            
            orchestrator.provisionTenant(request.getTenantId(), request.getTenantName(), request.getProvisionNotes());
            logger.info("Successfully processed provision tenant message for: {}", request.getTenantId());
            
        } catch (JsonProcessingException e) {
            logger.error("Error deserializing provision tenant message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to deserialize provision tenant message", e);
        } catch (IllegalArgumentException e) {
            logger.error("Validation error in provision tenant message: {}", e.getMessage(), e);
            throw e;
        } catch (RuntimeException e) {
            logger.error("Runtime error handling provision tenant message: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Handle disable tenant message.
     * 
     * @param messageValue the JSON message value
     */
    private void handleDisableTenant(String messageValue) {
        try {
            // Deserialize the request
            DisableTenantRequest request = objectMapper.readValue(messageValue, DisableTenantRequest.class);
            
            // Comprehensive validation
            TenantValidationUtils.ValidationResult validationResult = TenantValidationUtils.validateDisableTenantRequest(request);
            if (!validationResult.isValid()) {
                logger.error("Invalid disable tenant message: {}", validationResult.getErrorMessage());
                return;
            }
            

            
            orchestrator.disableTenant(request.getTenantId(), request.getDisableNotes());
            logger.info("Successfully processed disable tenant message for: {}", request.getTenantId());
            
        } catch (JsonProcessingException e) {
            logger.error("Error deserializing disable tenant message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to deserialize disable tenant message", e);
        } catch (IllegalArgumentException e) {
            logger.error("Validation error in disable tenant message: {}", e.getMessage(), e);
            throw e;
        } catch (RuntimeException e) {
            logger.error("Runtime error handling disable tenant message: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Handle delete tenant message.
     * 
     * @param messageValue the JSON message value
     */
    private void handleDeleteTenant(String messageValue) {
        try {
            // Deserialize the request
            DeleteTenantRequest request = objectMapper.readValue(messageValue, DeleteTenantRequest.class);
            
            // Comprehensive validation
            TenantValidationUtils.ValidationResult validationResult = TenantValidationUtils.validateDeleteTenantRequest(request);
            if (!validationResult.isValid()) {
                logger.error("Invalid delete tenant message: {}", validationResult.getErrorMessage());
                return;
            }
            

            
            orchestrator.deleteTenant(request.getTenantId(), request.getDeletionNotes());
            logger.info("Successfully processed delete tenant message for: {}", request.getTenantId());
            
        } catch (JsonProcessingException e) {
            logger.error("Error deserializing delete tenant message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to deserialize delete tenant message", e);
        } catch (IllegalArgumentException e) {
            logger.error("Validation error in delete tenant message: {}", e.getMessage(), e);
            throw e;
        } catch (RuntimeException e) {
            logger.error("Runtime error handling delete tenant message: {}", e.getMessage(), e);
            throw e;
        }
    }
} 