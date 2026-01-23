package ai.eigloo.agentic.graphbuilder.repository;

import ai.eigloo.agentic.graphbuilder.entity.GraphBundleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for GraphBundleEntity operations.
 * Provides CRUD operations and custom queries for graph bundle management.
 */
@Repository
public interface GraphBundleRepository extends JpaRepository<GraphBundleEntity, String> {

    /**
     * Find a graph bundle by its unique process ID.
     *
     * @param processId the process ID to search for
     * @return Optional containing the graph bundle if found
     */
    Optional<GraphBundleEntity> findByProcessId(String processId);

    /**
     * Find all graph bundles for a specific tenant.
     *
     * @param tenantId the tenant ID to filter by
     * @return list of graph bundles for the tenant
     */
    List<GraphBundleEntity> findByTenantIdOrderByUploadTimeDesc(String tenantId);

    /**
     * Find all graph bundles with a specific status.
     *
     * @param status the status to filter by
     * @return list of graph bundles with the specified status
     */
    List<GraphBundleEntity> findByStatus(String status);

    /**
     * Find all graph bundles for a tenant with a specific status.
     *
     * @param tenantId the tenant ID to filter by
     * @param status the status to filter by
     * @return list of graph bundles matching both criteria
     */
    List<GraphBundleEntity> findByTenantIdAndStatus(String tenantId, String status);

    /**
     * Find graph bundles uploaded before a specific date (for cleanup operations).
     *
     * @param cutoffDate the date before which to find bundles
     * @return list of graph bundles uploaded before the cutoff date
     */
    List<GraphBundleEntity> findByUploadTimeBefore(LocalDateTime cutoffDate);

    /**
     * Count the number of graph bundles for a specific tenant.
     *
     * @param tenantId the tenant ID to count for
     * @return the number of graph bundles for the tenant
     */
    long countByTenantId(String tenantId);

    /**
     * Find the most recent graph bundle for a tenant with a specific graph name.
     *
     * @param tenantId the tenant ID to filter by
     * @param graphName the graph name to filter by
     * @return Optional containing the most recent graph bundle if found
     */
    Optional<GraphBundleEntity> findTopByTenantIdAndGraphNameOrderByUploadTimeDesc(String tenantId, String graphName);

    /**
     * Check if a graph bundle with the given process ID exists.
     *
     * @param processId the process ID to check
     * @return true if a bundle with the process ID exists
     */
    boolean existsByProcessId(String processId);
}