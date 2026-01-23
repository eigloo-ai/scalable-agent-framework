package ai.eigloo.agentic.admin.service;

import ai.eigloo.agentic.admin.entity.TenantInfoEntity;
import ai.eigloo.agentic.admin.repository.TenantInfoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service responsible for the core business logic of tenant provisioning.
 * Handles both Kafka topic operations and database operations for tenant lifecycle management.
 */
@Service
public class TenantProvisioningOrchestrator {
    
    private static final Logger logger = LoggerFactory.getLogger(TenantProvisioningOrchestrator.class);
    
    private final TenantInfoRepository tenantInfoRepository;
    private final KafkaTopicManager kafkaTopicManager;
    
    @Autowired
    public TenantProvisioningOrchestrator(TenantInfoRepository tenantInfoRepository, 
                                        KafkaTopicManager kafkaTopicManager) {
        this.tenantInfoRepository = tenantInfoRepository;
        this.kafkaTopicManager = kafkaTopicManager;
    }
    
    /**
     * Provision a new tenant with Kafka topics and database record.
     * 
     * @param tenantId the tenant identifier
     * @param tenantName the tenant name
     * @param provisionNotes optional notes about the provisioning
     * @throws RuntimeException if provisioning fails
     */
    @Transactional
    public void provisionTenant(String tenantId, String tenantName, String provisionNotes) {
        logger.info("Provisioning tenant: {} with name: {}", tenantId, tenantName);
        
        // Check if tenant already exists
        if (tenantInfoRepository.existsByTenantId(tenantId)) {
            logger.warn("Tenant {} already exists, skipping provisioning", tenantId);
            return;
        }
        
        try {
            // Create Kafka topics first
            kafkaTopicManager.createTenantTopics(tenantId);
            logger.debug("Created Kafka topics for tenant: {}", tenantId);
            
            // Create database record
            TenantInfoEntity tenantInfo = new TenantInfoEntity();
            tenantInfo.setId(UUID.randomUUID().toString());
            tenantInfo.setTenantId(tenantId);
            tenantInfo.setTenantName(tenantName);
            tenantInfo.setProvisionNotes(provisionNotes);
            tenantInfo.setEnabled(true);
            tenantInfo.setCreationTime(Instant.now());
            
            tenantInfoRepository.save(tenantInfo);
            logger.info("Successfully provisioned tenant: {} with name: {}", tenantId, tenantName);
            
        } catch (Exception e) {
            logger.error("XX Failed to provision tenant {}: {}", tenantId, e.getMessage(), e);
            throw new RuntimeException("Failed to provision tenant: " + tenantId, e);
        }
    }
    
    /**
     * Disable an existing tenant.
     * 
     * @param tenantId the tenant identifier
     * @param disableNotes optional notes about the disable operation
     * @throws RuntimeException if disable operation fails
     */
    @Transactional
    public void disableTenant(String tenantId, String disableNotes) {
        logger.info("Disabling tenant: {}", tenantId);
        
        TenantInfoEntity tenantInfo = tenantInfoRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantId));
        
        if (!tenantInfo.getEnabled()) {
            logger.warn("Tenant {} is already disabled", tenantId);
            return;
        }
        
        try {
            // Update database record
            tenantInfo.setEnabled(false);
            tenantInfo.setDisableTime(Instant.now());
            tenantInfo.setDisableNotes(disableNotes);
            
            tenantInfoRepository.save(tenantInfo);
            logger.info("Successfully disabled tenant: {}", tenantId);
            
        } catch (Exception e) {
            logger.error("Failed to disable tenant {}: {}", tenantId, e.getMessage(), e);
            throw new RuntimeException("Failed to disable tenant: " + tenantId, e);
        }
    }
    
    /**
     * Delete a tenant completely (Kafka topics and database record).
     * 
     * @param tenantId the tenant identifier
     * @param deletionNotes optional notes about the deletion
     * @throws RuntimeException if deletion fails
     */
    @Transactional
    public void deleteTenant(String tenantId, String deletionNotes) {
        logger.info("Deleting tenant: {}", tenantId);
        
        TenantInfoEntity tenantInfo = tenantInfoRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantId));
        
        try {
            // Delete Kafka topics first
            kafkaTopicManager.deleteTenantTopics(tenantId);
            logger.debug("Deleted Kafka topics for tenant: {}", tenantId);
            
            // Delete database record
            tenantInfoRepository.delete(tenantInfo);
            logger.info("Successfully deleted tenant: {}", tenantId);
            
        } catch (Exception e) {
            logger.error("Failed to delete tenant {}: {}", tenantId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete tenant: " + tenantId, e);
        }
    }
    
    /**
     * Re-enable a disabled tenant.
     * 
     * @param tenantId the tenant identifier
     * @param enableNotes optional notes about the enable operation
     * @throws RuntimeException if enable operation fails
     */
    @Transactional
    public void enableTenant(String tenantId, String enableNotes) {
        logger.info("Enabling tenant: {}", tenantId);
        
        TenantInfoEntity tenantInfo = tenantInfoRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantId));
        
        if (tenantInfo.getEnabled()) {
            logger.warn("Tenant {} is already enabled", tenantId);
            return;
        }
        
        try {
            // Ensure Kafka topics exist
            if (!kafkaTopicManager.allTenantTopicsExist(tenantId)) {
                kafkaTopicManager.createTenantTopics(tenantId);
                logger.debug("Recreated Kafka topics for tenant: {}", tenantId);
            }
            
            // Update database record
            tenantInfo.setEnabled(true);
            tenantInfo.setDisableTime(null);
            tenantInfo.setDisableNotes(null);
            
            tenantInfoRepository.save(tenantInfo);
            logger.info("Successfully enabled tenant: {}", tenantId);
            
        } catch (Exception e) {
            logger.error("Failed to enable tenant {}: {}", tenantId, e.getMessage(), e);
            throw new RuntimeException("Failed to enable tenant: " + tenantId, e);
        }
    }
} 