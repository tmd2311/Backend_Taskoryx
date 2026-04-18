package com.taskoryx.backend.repository;

import com.taskoryx.backend.entity.UserProjectPerformance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserProjectPerformanceRepository extends JpaRepository<UserProjectPerformance, UUID> {

    // Project-level (sprint = null)
    List<UserProjectPerformance> findByProjectIdAndSprintIsNullOrderByTotalScoreDesc(UUID projectId);

    Optional<UserProjectPerformance> findByProjectIdAndUserIdAndSprintIsNull(UUID projectId, UUID userId);

    // Sprint-level
    List<UserProjectPerformance> findByProjectIdAndSprintIdOrderByTotalScoreDesc(UUID projectId, UUID sprintId);

    Optional<UserProjectPerformance> findByProjectIdAndUserIdAndSprintId(UUID projectId, UUID userId, UUID sprintId);

    // Cross-project view for a user
    List<UserProjectPerformance> findByUserIdAndSprintIsNullOrderByTotalScoreDesc(UUID userId);
}
