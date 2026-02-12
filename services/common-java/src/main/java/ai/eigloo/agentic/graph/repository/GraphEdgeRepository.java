package ai.eigloo.agentic.graph.repository;

import ai.eigloo.agentic.graph.entity.GraphEdgeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for GraphEdgeEntity operations.
 */
@Repository
public interface GraphEdgeRepository extends JpaRepository<GraphEdgeEntity, String> {

    /**
     * Find all edges for a specific agent graph.
     */
    List<GraphEdgeEntity> findByAgentGraphId(String graphId);

    /**
     * Delete all edges for a specific agent graph.
     */
    void deleteByAgentGraphId(String graphId);
}

