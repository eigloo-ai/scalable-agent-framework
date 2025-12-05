package ai.eigloo.agentic.graphbuilder.repository;

import ai.eigloo.agentic.graphbuilder.entity.ProcessingStepEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ProcessingStepEntity operations.
 * Provides CRUD operations and custom queries for processing step management.
 */
@Repository
public interface ProcessingStepRepository extends JpaRepository<ProcessingStepEntity, String> {

    /**
     * Find all processing steps for a specific process ID, ordered by start time.
     *
     * @param processId the process ID to filter by
     * @return list of processing steps for the process, ordered by start time
     */
    List<ProcessingStepEntity> findByProcessIdOrderByStartTimeAsc(String processId);

    /**
     * Find all processing steps with a specific status.
     *
     * @param status the status to filter by
     * @return list of processing steps with the specified status
     */
    List<ProcessingStepEntity> findByStatus(String status);

    /**
     * Find processing steps for a specific process ID and status.
     *
     * @param processId the process ID to filter by
     * @param status the status to filter by
     * @return list of processing steps matching both criteria
     */
    List<ProcessingStepEntity> findByProcessIdAndStatus(String processId, String status);

    /**
     * Find the latest processing step for a specific process ID.
     *
     * @param processId the process ID to filter by
     * @return Optional containing the latest processing step if found
     */
    Optional<ProcessingStepEntity> findTopByProcessIdOrderByStartTimeDesc(String processId);

    /**
     * Find a specific processing step by process ID and step name.
     *
     * @param processId the process ID to filter by
     * @param stepName the step name to filter by
     * @return Optional containing the processing step if found
     */
    Optional<ProcessingStepEntity> findByProcessIdAndStepName(String processId, String stepName);

    /**
     * Find all failed processing steps (status = 'FAILED').
     *
     * @return list of all failed processing steps
     */
    @Query("SELECT ps FROM ProcessingStepEntity ps WHERE ps.status = 'FAILED'")
    List<ProcessingStepEntity> findAllFailedSteps();

    /**
     * Find all processing steps that started before a specific date (for cleanup operations).
     *
     * @param cutoffDate the date before which to find steps
     * @return list of processing steps started before the cutoff date
     */
    List<ProcessingStepEntity> findByStartTimeBefore(LocalDateTime cutoffDate);

    /**
     * Count the number of processing steps for a specific process ID.
     *
     * @param processId the process ID to count for
     * @return the number of processing steps for the process
     */
    long countByProcessId(String processId);

    /**
     * Count the number of completed processing steps for a specific process ID.
     *
     * @param processId the process ID to count for
     * @return the number of completed processing steps for the process
     */
    @Query("SELECT COUNT(ps) FROM ProcessingStepEntity ps WHERE ps.processId = :processId AND ps.status = 'COMPLETED'")
    long countCompletedByProcessId(@Param("processId") String processId);

    /**
     * Find all processing steps that are currently in progress (status = 'IN_PROGRESS').
     *
     * @return list of all in-progress processing steps
     */
    @Query("SELECT ps FROM ProcessingStepEntity ps WHERE ps.status = 'IN_PROGRESS'")
    List<ProcessingStepEntity> findAllInProgressSteps();

    /**
     * Delete all processing steps for a specific process ID.
     *
     * @param processId the process ID to delete steps for
     */
    void deleteByProcessId(String processId);
}