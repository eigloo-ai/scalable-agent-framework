package ai.eigloo.agentic.dataplane.repository;

import ai.eigloo.agentic.dataplane.entity.PlanExecutionEntity;
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
 * Spring Data JPA repository for PlanExecution entities.
 * Provides data access methods with tenant-based filtering.
 */
@Repository
public interface PlanExecutionRepository extends JpaRepository<PlanExecutionEntity, String> {
    
    /**
     * Find all plan executions for a specific tenant.
     * 
     * @param tenantId the tenant identifier
     * @return list of plan executions for the tenant
     */
    List<PlanExecutionEntity> findByTenantId(String tenantId);
    
    /**
     * Find all plan executions for a specific tenant with pagination.
     * 
     * @param tenantId the tenant identifier
     * @param pageable pagination parameters
     * @return page of plan executions for the tenant
     */
    Page<PlanExecutionEntity> findByTenantId(String tenantId, Pageable pageable);
    
    /**
     * Find plan executions by tenant and status.
     * 
     * @param tenantId the tenant identifier
     * @param status the execution status
     * @return list of plan executions matching the criteria
     */
    List<PlanExecutionEntity> findByTenantIdAndStatus(String tenantId, PlanExecutionEntity.ExecutionStatus status);
    
    /**
     * Find plan executions by tenant and status with pagination.
     * 
     * @param tenantId the tenant identifier
     * @param status the execution status
     * @param pageable pagination parameters
     * @return page of plan executions matching the criteria
     */
    Page<PlanExecutionEntity> findByTenantIdAndStatus(String tenantId, PlanExecutionEntity.ExecutionStatus status, Pageable pageable);
    
    /**
     * Find plan executions by tenant and lifetime ID.
     * 
     * @param tenantId the tenant identifier
     * @param lifetimeId the lifetime identifier
     * @return list of plan executions for the lifetime
     */
    List<PlanExecutionEntity> findByTenantIdAndLifetimeId(String tenantId, String lifetimeId);
    
    /**
     * Find plan executions by tenant and graph ID.
     * 
     * @param tenantId the tenant identifier
     * @param graphId the graph identifier
     * @return list of plan executions for the graph
     */
    List<PlanExecutionEntity> findByTenantIdAndGraphId(String tenantId, String graphId);
    
    /**
     * Find plan executions by tenant and graph ID with pagination.
     * 
     * @param tenantId the tenant identifier
     * @param graphId the graph identifier
     * @param pageable pagination parameters
     * @return page of plan executions for the graph
     */
    Page<PlanExecutionEntity> findByTenantIdAndGraphId(String tenantId, String graphId, Pageable pageable);
    
    /**
     * Find plan executions by tenant and plan type.
     * 
     * @param tenantId the tenant identifier
     * @param planType the plan type
     * @return list of plan executions for the plan type
     */
    List<PlanExecutionEntity> findByTenantIdAndPlanType(String tenantId, String planType);
    
    /**
     * Find plan executions by tenant and plan type with pagination.
     * 
     * @param tenantId the tenant identifier
     * @param planType the plan type
     * @param pageable pagination parameters
     * @return page of plan executions for the plan type
     */
    Page<PlanExecutionEntity> findByTenantIdAndPlanType(String tenantId, String planType, Pageable pageable);
    
    /**
     * Find plan executions by tenant and input task ID.
     * 
     * @param tenantId the tenant identifier
     * @param inputTaskId the input task identifier
     * @return list of plan executions for the input task
     */
    List<PlanExecutionEntity> findByTenantIdAndInputTaskId(String tenantId, String inputTaskId);
    
    /**
     * Find plan executions by tenant and input task ID with pagination.
     * 
     * @param tenantId the tenant identifier
     * @param inputTaskId the input task identifier
     * @param pageable pagination parameters
     * @return page of plan executions for the input task
     */
    Page<PlanExecutionEntity> findByTenantIdAndInputTaskId(String tenantId, String inputTaskId, Pageable pageable);
    
    /**
     * Find plan executions by tenant and parent ID.
     * 
     * @param tenantId the tenant identifier
     * @param parentId the parent execution identifier
     * @return list of child plan executions
     */
    List<PlanExecutionEntity> findByTenantIdAndParentId(String tenantId, String parentId);
    
    /**
     * Find plan executions created within a time range for a tenant.
     * 
     * @param tenantId the tenant identifier
     * @param startTime the start time (inclusive)
     * @param endTime the end time (inclusive)
     * @return list of plan executions in the time range
     */
    List<PlanExecutionEntity> findByTenantIdAndCreatedAtBetween(String tenantId, Instant startTime, Instant endTime);
    
    /**
     * Find plan executions created within a time range for a tenant with pagination.
     * 
     * @param tenantId the tenant identifier
     * @param startTime the start time (inclusive)
     * @param endTime the end time (inclusive)
     * @param pageable pagination parameters
     * @return page of plan executions in the time range
     */
    Page<PlanExecutionEntity> findByTenantIdAndCreatedAtBetween(String tenantId, Instant startTime, Instant endTime, Pageable pageable);
    
    /**
     * Count plan executions by tenant and status.
     * 
     * @param tenantId the tenant identifier
     * @param status the execution status
     * @return count of plan executions matching the criteria
     */
    long countByTenantIdAndStatus(String tenantId, PlanExecutionEntity.ExecutionStatus status);
    
    /**
     * Count plan executions by tenant.
     * 
     * @param tenantId the tenant identifier
     * @return count of plan executions for the tenant
     */
    long countByTenantId(String tenantId);
    
    /**
     * Find the most recent plan execution for a tenant.
     * 
     * @param tenantId the tenant identifier
     * @return optional containing the most recent plan execution
     */
    @Query("SELECT p FROM PlanExecutionEntity p WHERE p.tenantId = :tenantId ORDER BY p.createdAt DESC")
    Optional<PlanExecutionEntity> findFirstByTenantIdOrderByCreatedAtDesc(@Param("tenantId") String tenantId);
    
    /**
     * Find plan executions with errors for a tenant.
     * 
     * @param tenantId the tenant identifier
     * @return list of plan executions with error messages
     */
    @Query("SELECT p FROM PlanExecutionEntity p WHERE p.tenantId = :tenantId AND p.errorMessage IS NOT NULL")
    List<PlanExecutionEntity> findWithErrorsByTenantId(@Param("tenantId") String tenantId);
    
    /**
     * Find plan executions with errors for a tenant with pagination.
     * 
     * @param tenantId the tenant identifier
     * @param pageable pagination parameters
     * @return page of plan executions with error messages
     */
    @Query("SELECT p FROM PlanExecutionEntity p WHERE p.tenantId = :tenantId AND p.errorMessage IS NOT NULL")
    Page<PlanExecutionEntity> findWithErrorsByTenantId(@Param("tenantId") String tenantId, Pageable pageable);
    
    /**
     * Find plan executions with high confidence for a tenant.
     * 
     * @param tenantId the tenant identifier
     * @param minConfidence minimum confidence threshold
     * @return list of plan executions with high confidence
     */
    @Query("SELECT p FROM PlanExecutionEntity p WHERE p.tenantId = :tenantId AND p.confidence >= :minConfidence")
    List<PlanExecutionEntity> findByTenantIdAndConfidenceGreaterThanEqual(@Param("tenantId") String tenantId, @Param("minConfidence") Double minConfidence);
    
    /**
     * Find plan executions with high confidence for a tenant with pagination.
     * 
     * @param tenantId the tenant identifier
     * @param minConfidence minimum confidence threshold
     * @param pageable pagination parameters
     * @return page of plan executions with high confidence
     */
    @Query("SELECT p FROM PlanExecutionEntity p WHERE p.tenantId = :tenantId AND p.confidence >= :minConfidence")
    Page<PlanExecutionEntity> findByTenantIdAndConfidenceGreaterThanEqual(@Param("tenantId") String tenantId, @Param("minConfidence") Double minConfidence, Pageable pageable);
    
    /**
     * Find plan execution by tenant and execution ID.
     * 
     * @param tenantId the tenant identifier
     * @param execId the execution identifier
     * @return optional containing the plan execution
     */
    Optional<PlanExecutionEntity> findByTenantIdAndExecId(String tenantId, String execId);
} 