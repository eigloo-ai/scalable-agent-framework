package ai.eigloo.agentic.dataplane.repository;

import ai.eigloo.agentic.dataplane.entity.TaskResultEntity;
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
 * Spring Data JPA repository for TaskResult entities.
 * Provides data access methods with tenant-based filtering.
 */
@Repository
public interface TaskResultRepository extends JpaRepository<TaskResultEntity, String> {
    
    /**
     * Find all task results for a specific tenant.
     * 
     * @param tenantId the tenant identifier
     * @return list of task results for the tenant
     */
    List<TaskResultEntity> findByTenantId(String tenantId);
    
    /**
     * Find all task results for a specific tenant with pagination.
     * 
     * @param tenantId the tenant identifier
     * @param pageable pagination parameters
     * @return page of task results for the tenant
     */
    Page<TaskResultEntity> findByTenantId(String tenantId, Pageable pageable);
    
    /**
     * Find task result by tenant and ID.
     * 
     * @param tenantId the tenant identifier
     * @param id the task result identifier
     * @return optional containing the task result
     */
    Optional<TaskResultEntity> findByTenantIdAndId(String tenantId, String id);
    
    /**
     * Find task results created within a time range for a tenant.
     * 
     * @param tenantId the tenant identifier
     * @param startTime the start time (inclusive)
     * @param endTime the end time (inclusive)
     * @return list of task results in the time range
     */
    List<TaskResultEntity> findByTenantIdAndDbCreatedAtBetween(String tenantId, Instant startTime, Instant endTime);
    
    /**
     * Find task results created within a time range for a tenant with pagination.
     * 
     * @param tenantId the tenant identifier
     * @param startTime the start time (inclusive)
     * @param endTime the end time (inclusive)
     * @param pageable pagination parameters
     * @return page of task results in the time range
     */
    Page<TaskResultEntity> findByTenantIdAndDbCreatedAtBetween(String tenantId, Instant startTime, Instant endTime, Pageable pageable);
    
    /**
     * Count task results by tenant.
     * 
     * @param tenantId the tenant identifier
     * @return count of task results for the tenant
     */
    long countByTenantId(String tenantId);
    
    /**
     * Find the most recent task result for a tenant.
     * 
     * @param tenantId the tenant identifier
     * @return optional containing the most recent task result
     */
    @Query("SELECT t FROM TaskResultEntity t WHERE t.tenantId = :tenantId ORDER BY t.dbCreatedAt DESC")
    Optional<TaskResultEntity> findFirstByTenantIdOrderByDbCreatedAtDesc(@Param("tenantId") String tenantId);
    
    /**
     * Find task results with errors for a tenant.
     * 
     * @param tenantId the tenant identifier
     * @return list of task results with error messages
     */
    @Query("SELECT t FROM TaskResultEntity t WHERE t.tenantId = :tenantId AND t.errorMessage IS NOT NULL")
    List<TaskResultEntity> findWithErrorsByTenantId(@Param("tenantId") String tenantId);
    
    /**
     * Find task results with errors for a tenant with pagination.
     * 
     * @param tenantId the tenant identifier
     * @param pageable pagination parameters
     * @return page of task results with error messages
     */
    @Query("SELECT t FROM TaskResultEntity t WHERE t.tenantId = :tenantId AND t.errorMessage IS NOT NULL")
    Page<TaskResultEntity> findWithErrorsByTenantId(@Param("tenantId") String tenantId, Pageable pageable);
} 