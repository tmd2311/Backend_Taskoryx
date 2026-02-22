package com.taskoryx.backend.repository;

import com.taskoryx.backend.entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BoardRepository extends JpaRepository<Board, UUID> {

    List<Board> findByProjectIdOrderByPositionAsc(UUID projectId);

    Optional<Board> findByProjectIdAndIsDefaultTrue(UUID projectId);

    @Query("SELECT MAX(b.position) FROM Board b WHERE b.project.id = :projectId")
    Optional<Integer> findMaxPositionByProjectId(@Param("projectId") UUID projectId);
}
