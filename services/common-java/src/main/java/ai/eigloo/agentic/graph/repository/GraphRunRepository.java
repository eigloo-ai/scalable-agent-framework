package ai.eigloo.agentic.graph.repository;

import ai.eigloo.agentic.graph.entity.GraphRunEntity;
import ai.eigloo.agentic.graph.entity.GraphRunStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for graph runtime execution records.
 */
@Repository
public interface GraphRunRepository extends JpaRepository<GraphRunEntity, String> {

    Optional<GraphRunEntity> findByLifetimeIdAndTenantId(String lifetimeId, String tenantId);

    List<GraphRunEntity> findByTenantIdAndGraphIdOrderByCreatedAtDesc(String tenantId, String graphId);

    Optional<GraphRunEntity> findTopByTenantIdAndGraphIdOrderByCreatedAtDesc(String tenantId, String graphId);

    long countByTenantIdAndGraphIdAndStatus(String tenantId, String graphId, GraphRunStatus status);
}
