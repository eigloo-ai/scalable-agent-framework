package ai.eigloo.agentic.graph.repository;

import ai.eigloo.agentic.graph.entity.ExecutorFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ExecutorFileEntity operations.
 */
@Repository
public interface ExecutorFileRepository extends JpaRepository<ExecutorFileEntity, String> {

    /**
     * Find all files for a specific plan, ordered by name.
     */
    List<ExecutorFileEntity> findByPlanId(String planId);
    
    /**
     * Find all files for a specific plan, ordered by name.
     */
    List<ExecutorFileEntity> findByPlanIdOrderByName(String planId);

    /**
     * Find all files for a specific task.
     */
    List<ExecutorFileEntity> findByTaskId(String taskId);
    
    /**
     * Find all files for a specific task, ordered by name.
     */
    List<ExecutorFileEntity> findByTaskIdOrderByName(String taskId);

    /**
     * Find a specific file by plan ID and file name.
     */
    Optional<ExecutorFileEntity> findByPlanIdAndName(String planId, String fileName);

    /**
     * Find a specific file by task ID and file name.
     */
    Optional<ExecutorFileEntity> findByTaskIdAndName(String taskId, String fileName);

    /**
     * Delete all files for a specific plan.
     */
    void deleteByPlanId(String planId);

    /**
     * Delete all files for a specific task.
     */
    void deleteByTaskId(String taskId);

    /**
     * Check if a file exists for a plan with the given name.
     */
    boolean existsByPlanIdAndName(String planId, String fileName);

    /**
     * Check if a file exists for a task with the given name.
     */
    boolean existsByTaskIdAndName(String taskId, String fileName);
}