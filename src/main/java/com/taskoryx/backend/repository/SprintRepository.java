package com.taskoryx.backend.repository;

import com.taskoryx.backend.entity.Sprint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SprintRepository extends JpaRepository<Sprint, UUID> {

    List<Sprint> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    Page<Sprint> findByProjectId(UUID projectId, Pageable pageable);

    List<Sprint> findByProjectIdAndStatus(UUID projectId, Sprint.SprintStatus status);

    boolean existsByProjectIdAndStatus(UUID projectId, Sprint.SprintStatus status);

    @Query("SELECT s FROM Sprint s WHERE s.project.id = :projectId AND s.status = 'ACTIVE'")
    Optional<Sprint> findActiveSprintByProjectId(@Param("projectId") UUID projectId);

    Optional<Sprint> findByBoardId(UUID boardId);
}
