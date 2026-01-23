package ai.eigloo.agentic.graph.repository;

import ai.eigloo.agentic.graph.entity.AgentGraphEntity;
import ai.eigloo.agentic.graph.entity.GraphStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for AgentGraphEntity operations.
 */
@Repository
public interface AgentGraphRepository extends JpaRepository<AgentGraphEntity, String> {

    /**
     * Find all agent graphs for a specific tenant.
     */
    List<AgentGraphEntity> findByTenantIdOrderByCreatedAtDesc(String tenantId);
    
    /**
     * Find all agent graphs for a specific tenant, ordered by updated date.
     */
    List<AgentGraphEntity> findByTenantIdOrderByUpdatedAtDesc(String tenantId);
    
    /**
     * Find an agent graph by ID and tenant ID.
     */
    Optional<AgentGraphEntity> findByIdAndTenantId(String id, String tenantId);

    /**
     * Find an agent graph by tenant ID and name.
     */
    Optional<AgentGraphEntity> findByTenantIdAndName(String tenantId, String name);

    /**
     * Find the latest agent graph for a tenant and name.
     */
    Optional<AgentGraphEntity> findTopByTenantIdAndNameOrderByCreatedAtDesc(String tenantId, String name);


    /**
     * Check if an agent graph exists for tenant and name.
     */
    boolean existsByTenantIdAndName(String tenantId, String name);

    /**
     * Count agent graphs for a tenant.
     */
    long countByTenantId(String tenantId);

    /**
     * Find all agent graphs for a specific tenant with a specific status.
     */
    List<AgentGraphEntity> findByTenantIdAndStatusOrderByCreatedAtDesc(String tenantId, GraphStatus status);

    /**
     * Find all agent graphs with a specific status.
     */
    List<AgentGraphEntity> findByStatusOrderByCreatedAtDesc(GraphStatus status);

    /**
     * Find agent graph with all relations. Plans, tasks, and files are eagerly loaded via entity configuration.
     */
    @Query("SELECT g FROM AgentGraphEntity g WHERE g.id = :graphId AND g.tenantId = :tenantId")
    Optional<AgentGraphEntity> findByIdAndTenantIdWithAllRelations(@Param("graphId") String graphId, 
                                                                   @Param("tenantId") String tenantId);

    /**
     * Find agent graphs for tenant with basic info only (optimized for listing).
     */
    @Query("SELECT g FROM AgentGraphEntity g WHERE g.tenantId = :tenantId ORDER BY g.updatedAt DESC")
    List<AgentGraphEntity> findByTenantIdOptimized(@Param("tenantId") String tenantId);

    /**
     * Batch find agent graphs by IDs for a tenant (optimized for bulk operations).
     */
    @Query("SELECT g FROM AgentGraphEntity g WHERE g.id IN :graphIds AND g.tenantId = :tenantId")
    List<AgentGraphEntity> findByIdsAndTenantId(@Param("graphIds") List<String> graphIds, 
                                                @Param("tenantId") String tenantId);
}