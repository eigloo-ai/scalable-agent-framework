package ai.eigloo.agentic.dataplane.repository;

import ai.eigloo.agentic.dataplane.entity.TaskExecutionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for TaskExecution entities.
 * Provides data access methods with tenant-based filtering.
 */
@Repository
public interface TaskExecutionRepository extends JpaRepository<TaskExecutionEntity, String> {
    
    /**
     * Find all task executions for a specific tenant.
     * 
     * @param tenantId the tenant identifier
     * @return list of task executions for the tenant
     */
    List<TaskExecutionEntity> findByTenantId(String tenantId);
    
    /**
     * Find all task executions for a specific tenant with pagination.
     * 
     * @param tenantId the tenant identifier
     * @param pageable pagination parameters
     * @return page of task executions for the tenant
     */
    Page<TaskExecutionEntity> findByTenantId(String tenantId, Pageable pageable);
    
    /**
     * Find task executions by tenant and status.
     * 
     * @param tenantId the tenant identifier
     * @param status the execution status
     * @return list of task executions matching the criteria
     */
    List<TaskExecutionEntity> findByTenantIdAndStatus(String tenantId, TaskExecutionEntity.ExecutionStatus status);
    
    /**
     * Find task executions by tenant and status with pagination.
     * 
     * @param tenantId the tenant identifier
     * @param status the execution status
     * @param pageable pagination parameters
     * @return page of task executions matching the criteria
     */
    Page<TaskExecutionEntity> findByTenantIdAndStatus(String tenantId, TaskExecutionEntity.ExecutionStatus status, Pageable pageable);
    
    /**
     * Find task executions by tenant and lifetime ID.
     * 
     * @param tenantId the tenant identifier
     * @param lifetimeId the lifetime identifier
     * @return list of task executions for the lifetime
     */
    List<TaskExecutionEntity> findByTenantIdAndLifetimeId(String tenantId, String lifetimeId);
    
    /**
     * Find task executions by tenant and graph ID.
     * 
     * @param tenantId the tenant identifier
     * @param graphId the graph identifier
     * @return list of task executions for the graph
     */
    List<TaskExecutionEntity> findByTenantIdAndGraphId(String tenantId, String graphId);
    
    /**
     * Find task executions by tenant and graph ID with pagination.
     * 
     * @param tenantId the tenant identifier
     * @param graphId the graph identifier
     * @param pageable pagination parameters
     * @return page of task executions for the graph
     */
    Page<TaskExecutionEntity> findByTenantIdAndGraphId(String tenantId, String graphId, Pageable pageable);
    
    /**
     * Find task executions by tenant and task type.
     * 
     * @param tenantId the tenant identifier
     * @param taskType the task type
     * @return list of task executions for the task type
     */
    List<TaskExecutionEntity> findByTenantIdAndTaskType(String tenantId, String taskType);
    
    /**
     * Find task executions by tenant and task type with pagination.
     * 
     * @param tenantId the tenant identifier
     * @param taskType the task type
     * @param pageable pagination parameters
     * @return page of task executions for the task type
     */
    Page<TaskExecutionEntity> findByTenantIdAndTaskType(String tenantId, String taskType, Pageable pageable);
    
    /**
     * Find task executions by tenant and parent ID.
     * 
     * @param tenantId the tenant identifier
     * @param parentId the parent execution identifier
     * @return list of child task executions
     */
    List<TaskExecutionEntity> findByTenantIdAndParentId(String tenantId, String parentId);
    
    /**
     * Find task executions created within a time range for a tenant.
     * 
     * @param tenantId the tenant identifier
     * @param startTime the start time (inclusive)
     * @param endTime the end time (inclusive)
     * @return list of task executions in the time range
     */
    List<TaskExecutionEntity> findByTenantIdAndCreatedAtBetween(String tenantId, Instant startTime, Instant endTime);
    
    /**
     * Find task executions created within a time range for a tenant with pagination.
     * 
     * @param tenantId the tenant identifier
     * @param startTime the start time (inclusive)
     * @param endTime the end time (inclusive)
     * @param pageable pagination parameters
     * @return page of task executions in the time range
     */
    Page<TaskExecutionEntity> findByTenantIdAndCreatedAtBetween(String tenantId, Instant startTime, Instant endTime, Pageable pageable);
    
    /**
     * Count task executions by tenant and status.
     * 
     * @param tenantId the tenant identifier
     * @param status the execution status
     * @return count of task executions matching the criteria
     */
    long countByTenantIdAndStatus(String tenantId, TaskExecutionEntity.ExecutionStatus status);
    
    /**
     * Count task executions by tenant.
     * 
     * @param tenantId the tenant identifier
     * @return count of task executions for the tenant
     */
    long countByTenantId(String tenantId);
    
    /**
     * Find the most recent task execution for a tenant.
     * 
     * @param tenantId the tenant identifier
     * @return optional containing the most recent task execution
     */
    @Query("SELECT t FROM TaskExecutionEntity t WHERE t.tenantId = :tenantId ORDER BY t.createdAt DESC")
    Optional<TaskExecutionEntity> findFirstByTenantIdOrderByCreatedAtDesc(@Param("tenantId") String tenantId);
    
    /**
     * Find task executions with errors for a tenant.
     * 
     * @param tenantId the tenant identifier
     * @return list of task executions with error messages
     */
    @Query("SELECT t FROM TaskExecutionEntity t WHERE t.tenantId = :tenantId AND t.errorMessage IS NOT NULL")
    List<TaskExecutionEntity> findWithErrorsByTenantId(@Param("tenantId") String tenantId);
    
    /**
     * Find task executions with errors for a tenant with pagination.
     * 
     * @param tenantId the tenant identifier
     * @param pageable pagination parameters
     * @return page of task executions with error messages
     */
    @Query("SELECT t FROM TaskExecutionEntity t WHERE t.tenantId = :tenantId AND t.errorMessage IS NOT NULL")
    Page<TaskExecutionEntity> findWithErrorsByTenantId(@Param("tenantId") String tenantId, Pageable pageable);
} 