package com.pcallahan.agentic.graph.repository;

import com.pcallahan.agentic.graph.entity.TaskEntity;
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
     * Find all tasks for a specific upstream plan.
     */
    List<TaskEntity> findByUpstreamPlanId(String planId);

    /**
     * Find all tasks for a specific downstream plan.
     */
    List<TaskEntity> findByDownstreamPlanId(String planId);

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

    /**
     * Find tasks by upstream plan. Files and relationships are eagerly loaded via entity configuration.
     */
    @Query("SELECT t FROM TaskEntity t WHERE t.upstreamPlan.id = :planId")
    List<TaskEntity> findByUpstreamPlanIdWithFiles(@Param("planId") String planId);

    /**
     * Find tasks by downstream plan. Files and relationships are eagerly loaded via entity configuration.
     */
    @Query("SELECT t FROM TaskEntity t WHERE t.downstreamPlan.id = :planId")
    List<TaskEntity> findByDownstreamPlanIdWithFiles(@Param("planId") String planId);
}