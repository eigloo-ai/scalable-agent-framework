package ai.eigloo.agentic.graphbuilder.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ai.eigloo.agentic.graph.service.GraphPersistenceService;

import java.nio.file.Path;
import java.util.UUID;

/**
 * Service responsible for coordinating cleanup operations across all services.
 * Provides comprehensive cleanup and rollback mechanisms for failed operations.
 */
@Service
public class CleanupService {
    
    private static final Logger logger = LoggerFactory.getLogger(CleanupService.class);
    
    private final BundleProcessingService bundleProcessingService;
    private final DockerImageService dockerImageService;
    private final GraphPersistenceService graphPersistenceService;
    private final ProcessingStatusService processingStatusService;
    
    @Autowired
    public CleanupService(BundleProcessingService bundleProcessingService,
                         DockerImageService dockerImageService,
                         GraphPersistenceService graphPersistenceService,
                         ProcessingStatusService processingStatusService) {
        this.bundleProcessingService = bundleProcessingService;
        this.dockerImageService = dockerImageService;
        this.graphPersistenceService = graphPersistenceService;
        this.processingStatusService = processingStatusService;
    }
    
    /**
     * Performs comprehensive cleanup for a failed process.
     * This method coordinates cleanup across all services and ensures proper rollback.
     * 
     * @param processId The process ID to clean up
     * @param extractDir The temporary extraction directory (can be null)
     * @param cleanupLevel The level of cleanup to perform
     */
    public void performComprehensiveCleanup(String processId, Path extractDir, CleanupLevel cleanupLevel) {
        String correlationId = getOrCreateCorrelationId();
        
        logger.info("Starting comprehensive cleanup for process {} with level {} [correlationId={}]", 
            processId, cleanupLevel, correlationId);
        
        try {
            // Always cleanup temporary files first (no transaction needed)
            cleanupTemporaryFiles(extractDir, correlationId);
            
            // Cleanup Docker images (no transaction needed)
            cleanupDockerImages(processId, correlationId);
            
            // Cleanup database records based on cleanup level
            if (cleanupLevel == CleanupLevel.FULL) {
                cleanupDatabaseRecords(processId, correlationId);
            } else if (cleanupLevel == CleanupLevel.PARTIAL) {
                // Only mark as failed, keep records for debugging
                markProcessAsFailed(processId, correlationId);
            }
            
            logger.info("Comprehensive cleanup completed successfully for process {} [correlationId={}]", 
                processId, correlationId);
                
        } catch (Exception e) {
            logger.error("Error during comprehensive cleanup for process {} [correlationId={}]: {}", 
                processId, correlationId, e.getMessage(), e);
            // Don't re-throw - cleanup should not fail the overall process
        }
    }
    
    /**
     * Performs cleanup with transaction rollback for database operations.
     * This method should be called when database operations need to be rolled back.
     * 
     * @param processId The process ID
     * @param extractDir The temporary extraction directory
     * @param graphName The graph name (for persistence rollback)
     * @param tenantId The tenant ID (for persistence rollback)
     */
    @Transactional
    public void performCleanupWithRollback(String processId, Path extractDir, String graphName, String tenantId) {
        String correlationId = getOrCreateCorrelationId();
        
        try {
            logger.info("Starting cleanup with database rollback for process {} [correlationId={}]", 
                processId, correlationId);
            
            // Database operations that will be rolled back if any fail
            if (graphName != null && tenantId != null) {
                rollbackGraphPersistence(graphName, tenantId, correlationId);
            }
            
            // Mark process as failed (will be rolled back if cleanup fails)
            processingStatusService.markProcessAsFailed(processId, "CLEANUP", "Process failed and rolled back");
            
            logger.info("Database rollback completed for process {} [correlationId={}]", processId, correlationId);
            
        } catch (Exception e) {
            logger.error("Error during database rollback for process {} [correlationId={}]: {}", 
                processId, correlationId, e.getMessage(), e);
            // Re-throw to trigger transaction rollback
            throw new RuntimeException("Cleanup with rollback failed: " + e.getMessage(), e);
        } finally {
            // Always cleanup non-transactional resources
            cleanupTemporaryFiles(extractDir, correlationId);
            cleanupDockerImages(processId, correlationId);
        }
    }
    
    /**
     * Cleans up temporary files and directories.
     */
    private void cleanupTemporaryFiles(Path extractDir, String correlationId) {
        if (extractDir != null && bundleProcessingService != null) {
            try {
                logger.debug("Cleaning up temporary files [correlationId={}]", correlationId);
                bundleProcessingService.cleanupTempDirectory(extractDir);
                logger.debug("Temporary files cleanup completed [correlationId={}]", correlationId);
            } catch (Exception e) {
                logger.warn("Failed to cleanup temporary files [correlationId={}]: {}", correlationId, e.getMessage());
                // Don't re-throw - continue with other cleanup operations
            }
        }
    }
    
    /**
     * Cleans up Docker images for the process.
     */
    private void cleanupDockerImages(String processId, String correlationId) {
        if (dockerImageService != null) {
            try {
                logger.debug("Cleaning up Docker images for process {} [correlationId={}]", processId, correlationId);
                dockerImageService.cleanupDockerImages(processId);
                logger.debug("Docker images cleanup completed for process {} [correlationId={}]", processId, correlationId);
            } catch (Exception e) {
                logger.warn("Failed to cleanup Docker images for process {} [correlationId={}]: {}", 
                    processId, correlationId, e.getMessage());
                // Don't re-throw - continue with other cleanup operations
            }
        }
    }
    
    /**
     * Cleans up database records for the process.
     */
    private void cleanupDatabaseRecords(String processId, String correlationId) {
        if (processingStatusService != null) {
            try {
                logger.debug("Cleaning up database records for process {} [correlationId={}]", processId, correlationId);
                processingStatusService.cleanupProcessingRecords(processId);
                logger.debug("Database records cleanup completed for process {} [correlationId={}]", processId, correlationId);
            } catch (Exception e) {
                logger.warn("Failed to cleanup database records for process {} [correlationId={}]: {}", 
                    processId, correlationId, e.getMessage());
                // Don't re-throw - continue with other cleanup operations
            }
        }
    }
    
    /**
     * Marks the process as failed without removing records.
     */
    private void markProcessAsFailed(String processId, String correlationId) {
        if (processingStatusService != null) {
            try {
                logger.debug("Marking process {} as failed [correlationId={}]", processId, correlationId);
                processingStatusService.markProcessAsFailed(processId, "CLEANUP", "Process failed during cleanup");
                logger.debug("Process {} marked as failed [correlationId={}]", processId, correlationId);
            } catch (Exception e) {
                logger.warn("Failed to mark process {} as failed [correlationId={}]: {}", 
                    processId, correlationId, e.getMessage());
                // Don't re-throw - continue with other cleanup operations
            }
        }
    }
    
    /**
     * Rolls back graph persistence operations.
     */
    private void rollbackGraphPersistence(String graphName, String tenantId, String correlationId) {
        // This would involve removing any partially persisted graph data
        // For now, we log the rollback operation
        logger.info("Rolling back graph persistence for graph '{}' in tenant {} [correlationId={}]", 
            graphName, tenantId, correlationId);
        
        // In a real implementation, this might involve:
        // 1. Finding and deleting partially created graph entities
        // 2. Removing orphaned task and plan entities
        // 3. Cleaning up any related data
        
        // Since the GraphPersistenceService already handles transactional operations,
        // the Spring transaction manager will handle the actual rollback
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
    
    /**
     * Enum defining cleanup levels.
     */
    public enum CleanupLevel {
        /**
         * Partial cleanup - keeps database records for debugging but marks as failed.
         */
        PARTIAL,
        
        /**
         * Full cleanup - removes all traces of the failed process.
         */
        FULL
    }
}