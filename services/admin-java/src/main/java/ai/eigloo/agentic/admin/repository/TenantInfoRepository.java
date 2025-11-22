package ai.eigloo.agentic.admin.repository;

import ai.eigloo.agentic.admin.entity.TenantInfoEntity;
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
 * Spring Data JPA repository for TenantInfo entities.
 * Provides data access methods for tenant management.
 */
@Repository
public interface TenantInfoRepository extends JpaRepository<TenantInfoEntity, String> {
    
    /**
     * Find tenant by tenant ID.
     * 
     * @param tenantId the tenant identifier
     * @return optional containing the tenant info
     */
    Optional<TenantInfoEntity> findByTenantId(String tenantId);
    
    /**
     * Find all enabled tenants.
     * 
     * @return list of enabled tenants
     */
    List<TenantInfoEntity> findByEnabledTrue();
    
    /**
     * Find all disabled tenants.
     * 
     * @return list of disabled tenants
     */
    List<TenantInfoEntity> findByEnabledFalse();
    
    /**
     * Find tenants created within a time range.
     * 
     * @param startTime the start time (inclusive)
     * @param endTime the end time (inclusive)
     * @return list of tenants created in the time range
     */
    List<TenantInfoEntity> findByCreationTimeBetween(Instant startTime, Instant endTime);
    
    /**
     * Find tenants created within a time range with pagination.
     * 
     * @param startTime the start time (inclusive)
     * @param endTime the end time (inclusive)
     * @param pageable pagination parameters
     * @return page of tenants created in the time range
     */
    Page<TenantInfoEntity> findByCreationTimeBetween(Instant startTime, Instant endTime, Pageable pageable);
    
    /**
     * Check if tenant exists by tenant ID.
     * 
     * @param tenantId the tenant identifier
     * @return true if tenant exists, false otherwise
     */
    boolean existsByTenantId(String tenantId);
    
    /**
     * Find all tenants with pagination.
     * 
     * @param pageable pagination parameters
     * @return page of all tenants
     */
    Page<TenantInfoEntity> findAll(Pageable pageable);
    
    /**
     * Find enabled tenants with pagination.
     * 
     * @param pageable pagination parameters
     * @return page of enabled tenants
     */
    Page<TenantInfoEntity> findByEnabledTrue(Pageable pageable);
    
    /**
     * Find disabled tenants with pagination.
     * 
     * @param pageable pagination parameters
     * @return page of disabled tenants
     */
    Page<TenantInfoEntity> findByEnabledFalse(Pageable pageable);
    
    /**
     * Count enabled tenants.
     * 
     * @return count of enabled tenants
     */
    long countByEnabledTrue();
    
    /**
     * Count disabled tenants.
     * 
     * @return count of disabled tenants
     */
    long countByEnabledFalse();
    
    /**
     * Find tenants by name containing the given string (case-insensitive).
     * 
     * @param tenantName the tenant name to search for
     * @return list of tenants with matching names
     */
    @Query("SELECT t FROM TenantInfoEntity t WHERE LOWER(t.tenantName) LIKE LOWER(CONCAT('%', :tenantName, '%'))")
    List<TenantInfoEntity> findByTenantNameContainingIgnoreCase(@Param("tenantName") String tenantName);
    
    /**
     * Find tenants by name containing the given string with pagination.
     * 
     * @param tenantName the tenant name to search for
     * @param pageable pagination parameters
     * @return page of tenants with matching names
     */
    @Query("SELECT t FROM TenantInfoEntity t WHERE LOWER(t.tenantName) LIKE LOWER(CONCAT('%', :tenantName, '%'))")
    Page<TenantInfoEntity> findByTenantNameContainingIgnoreCase(@Param("tenantName") String tenantName, Pageable pageable);
} 