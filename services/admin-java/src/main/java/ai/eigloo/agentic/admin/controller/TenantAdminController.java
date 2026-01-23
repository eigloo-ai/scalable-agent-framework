package ai.eigloo.agentic.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ai.eigloo.agentic.admin.dto.*;
import ai.eigloo.agentic.admin.dto.DeleteTenantRequest;
import ai.eigloo.agentic.admin.dto.DisableTenantRequest;
import ai.eigloo.agentic.admin.dto.ProvisionTenantRequest;
import ai.eigloo.agentic.admin.dto.TenantInfoResponse;
import ai.eigloo.agentic.admin.entity.TenantInfoEntity;
import ai.eigloo.agentic.admin.repository.TenantInfoRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for tenant management operations.
 * Provides endpoints for provisioning, disabling, and deleting tenants.
 */
@RestController
@RequestMapping("/admin/tenant")
public class TenantAdminController {
    
    private static final Logger logger = LoggerFactory.getLogger(TenantAdminController.class);
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TenantInfoRepository tenantInfoRepository;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public TenantAdminController(KafkaTemplate<String, String> kafkaTemplate,
                               TenantInfoRepository tenantInfoRepository,
                               ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.tenantInfoRepository = tenantInfoRepository;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Provision a new tenant.
     * 
     * @param request the provision tenant request
     * @return response indicating success
     */
    @PostMapping("/provision")
    public ResponseEntity<String> provisionTenant(@Valid @RequestBody ProvisionTenantRequest request) {
        try {
            logger.info("Received provision tenant request: {}", request.getTenantId());
            
            // Check if tenant already exists
            if (tenantInfoRepository.existsByTenantId(request.getTenantId())) {
                return ResponseEntity.badRequest()
                    .body("Tenant already exists: " + request.getTenantId());
            }
            
            // Create JSON message for Kafka
            String message = objectMapper.writeValueAsString(request);
            
            // Publish to Kafka topic
            kafkaTemplate.send("agentic-admin-provision-tenant", "provision-tenant", message);
            
            logger.info("Published provision tenant message for: {}", request.getTenantId());
            return ResponseEntity.ok("Tenant provisioning initiated: " + request.getTenantId());
            
        } catch (Exception e) {
            logger.error("Error processing provision tenant request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error processing request: " + e.getMessage());
        }
    }
    
    /**
     * Disable an existing tenant.
     * 
     * @param request the disable tenant request
     * @return response indicating success
     */
    @PutMapping("/disable")
    public ResponseEntity<String> disableTenant(@Valid @RequestBody DisableTenantRequest request) {
        try {
            logger.info("Received disable tenant request: {}", request.getTenantId());
            
            // Check if tenant exists
            if (!tenantInfoRepository.existsByTenantId(request.getTenantId())) {
                return ResponseEntity.notFound().build();
            }
            
            // Create JSON message for Kafka
            String message = objectMapper.writeValueAsString(request);
            
            // Publish to Kafka topic
            kafkaTemplate.send("agentic-admin-provision-tenant", "disable-tenant", message);
            
            logger.info("Published disable tenant message for: {}", request.getTenantId());
            return ResponseEntity.ok("Tenant disable initiated: " + request.getTenantId());
            
        } catch (Exception e) {
            logger.error("Error processing disable tenant request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error processing request: " + e.getMessage());
        }
    }
    
    /**
     * Delete an existing tenant.
     * 
     * @param tenantId the tenant ID to delete
     * @param request the delete tenant request (optional notes)
     * @return response indicating success
     */
    @DeleteMapping("/{tenantId}")
    public ResponseEntity<String> deleteTenant(@PathVariable String tenantId,
                                             @RequestBody(required = false) DeleteTenantRequest request) {
        try {
            logger.info("Received delete tenant request: {}", tenantId);
            
            // Check if tenant exists
            if (!tenantInfoRepository.existsByTenantId(tenantId)) {
                return ResponseEntity.notFound().build();
            }
            
            // Create proper DTO with tenant ID
            DeleteTenantRequest deleteRequest = request != null ? request : new DeleteTenantRequest();
            deleteRequest.setTenantId(tenantId);
            
            // Create JSON message for Kafka
            String message = objectMapper.writeValueAsString(deleteRequest);
            
            // Publish to Kafka topic
            kafkaTemplate.send("agentic-admin-provision-tenant", "delete-tenant", message);
            
            logger.info("Published delete tenant message for: {}", tenantId);
            return ResponseEntity.ok("Tenant deletion initiated: " + tenantId);
            
        } catch (Exception e) {
            logger.error("Error processing delete tenant request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error processing request: " + e.getMessage());
        }
    }
    
    /**
     * Get all tenants for the web interface.
     * 
     * @return list of all tenants
     */
    @GetMapping
    public ResponseEntity<List<TenantInfoResponse>> getAllTenants() {
        try {
            logger.debug("Received request for all tenants");
            
            List<TenantInfoEntity> tenants = tenantInfoRepository.findAll();
            List<TenantInfoResponse> responses = tenants.stream()
                .map(TenantInfoResponse::fromEntity)
                .collect(Collectors.toList());
            
            logger.debug("Returning {} tenants", responses.size());
            return ResponseEntity.ok(responses);
            
        } catch (Exception e) {
            logger.error("Error retrieving tenants: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get a specific tenant by ID.
     * 
     * @param tenantId the tenant ID
     * @return the tenant information
     */
    @GetMapping("/{tenantId}")
    public ResponseEntity<TenantInfoResponse> getTenant(@PathVariable String tenantId) {
        try {
            logger.debug("Received request for tenant: {}", tenantId);
            
            TenantInfoEntity tenant = tenantInfoRepository.findByTenantId(tenantId)
                .orElse(null);
            
            if (tenant == null) {
                return ResponseEntity.notFound().build();
            }
            
            TenantInfoResponse response = TenantInfoResponse.fromEntity(tenant);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving tenant {}: {}", tenantId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
} 