package ai.eigloo.agentic.graph.repository;

import ai.eigloo.agentic.graph.entity.TaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for TaskEntity operations.
 */
@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, String> {

    /**
     * Find all tasks for a specific agent graph.
     */
    List<TaskEntity> findByAgentGraphId(String graphId);

    /**
     * Find a task by graph ID and task name.
     */
    Optional<TaskEntity> findByAgentGraphIdAndName(String graphId, String name);

    /**
     * Delete all tasks for a specific agent graph.
     */
    void deleteByAgentGraphId(String graphId);
    
    /**
     * Find a task by ID and agent graph ID.
     */
    Optional<TaskEntity> findByIdAndAgentGraphId(String id, String graphId);

    /**
     * Find all tasks for a graph. Files and relationships are eagerly loaded via entity configuration.
     */
    @Query("SELECT t FROM TaskEntity t WHERE t.agentGraph.id = :graphId")
    List<TaskEntity> findByAgentGraphIdWithFiles(@Param("graphId") String graphId);

    /**
     * Find task with all relations. Files and relationships are eagerly loaded via entity configuration.
     */
    @Query("SELECT t FROM TaskEntity t WHERE t.id = :taskId")
    Optional<TaskEntity> findByIdWithAllRelations(@Param("taskId") String taskId);

    /**
     * Batch find tasks by IDs. Files and relationships are eagerly loaded via entity configuration.
     */
    @Query("SELECT t FROM TaskEntity t WHERE t.id IN :taskIds")
    List<TaskEntity> findByIdsWithFiles(@Param("taskIds") List<String> taskIds);

}
