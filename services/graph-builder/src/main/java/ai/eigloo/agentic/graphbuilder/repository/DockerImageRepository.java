package ai.eigloo.agentic.graphbuilder.repository;

import ai.eigloo.agentic.graphbuilder.entity.DockerImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for DockerImageEntity operations.
 * Provides CRUD operations and custom queries for Docker image management.
 */
@Repository
public interface DockerImageRepository extends JpaRepository<DockerImageEntity, String> {

    /**
     * Find all Docker images for a specific process ID.
     *
     * @param processId the process ID to filter by
     * @return list of Docker images for the process
     */
    List<DockerImageEntity> findByProcessId(String processId);

    /**
     * Find all Docker images of a specific executor type.
     *
     * @param executorType the executor type to filter by (TASK or PLAN)
     * @return list of Docker images of the specified type
     */
    List<DockerImageEntity> findByExecutorType(String executorType);

    /**
     * Find all Docker images for a specific process ID and executor type.
     *
     * @param processId the process ID to filter by
     * @param executorType the executor type to filter by
     * @return list of Docker images matching both criteria
     */
    List<DockerImageEntity> findByProcessIdAndExecutorType(String processId, String executorType);

    /**
     * Find a Docker image by process ID and executor name.
     *
     * @param processId the process ID to filter by
     * @param executorName the executor name to filter by
     * @return Optional containing the Docker image if found
     */
    Optional<DockerImageEntity> findByProcessIdAndExecutorName(String processId, String executorName);

    /**
     * Find all Docker images with a specific image name.
     *
     * @param imageName the image name to filter by
     * @return list of Docker images with the specified name
     */
    List<DockerImageEntity> findByImageName(String imageName);

    /**
     * Find all Docker images built before a specific date (for cleanup operations).
     *
     * @param cutoffDate the date before which to find images
     * @return list of Docker images built before the cutoff date
     */
    List<DockerImageEntity> findByBuildTimeBefore(LocalDateTime cutoffDate);

    /**
     * Count the number of Docker images for a specific process ID.
     *
     * @param processId the process ID to count for
     * @return the number of Docker images for the process
     */
    long countByProcessId(String processId);

    /**
     * Count the number of Docker images by executor type.
     *
     * @param executorType the executor type to count for
     * @return the number of Docker images of the specified type
     */
    long countByExecutorType(String executorType);

    /**
     * Find all Docker images for a specific process ID, ordered by build time.
     *
     * @param processId the process ID to filter by
     * @return list of Docker images for the process, ordered by build time
     */
    List<DockerImageEntity> findByProcessIdOrderByBuildTimeAsc(String processId);

    /**
     * Find the latest Docker image for a specific executor name.
     *
     * @param executorName the executor name to filter by
     * @return Optional containing the latest Docker image if found
     */
    @Query("SELECT di FROM DockerImageEntity di WHERE di.executorName = :executorName ORDER BY di.buildTime DESC LIMIT 1")
    Optional<DockerImageEntity> findLatestByExecutorName(@Param("executorName") String executorName);

    /**
     * Delete all Docker images for a specific process ID.
     *
     * @param processId the process ID to delete images for
     */
    void deleteByProcessId(String processId);

    /**
     * Check if a Docker image exists for a specific process ID and executor name.
     *
     * @param processId the process ID to check
     * @param executorName the executor name to check
     * @return true if a Docker image exists for the process and executor
     */
    boolean existsByProcessIdAndExecutorName(String processId, String executorName);
}