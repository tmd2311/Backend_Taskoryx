package com.taskoryx.backend.repository;

import com.taskoryx.backend.entity.BoardColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BoardColumnRepository extends JpaRepository<BoardColumn, UUID> {

    List<BoardColumn> findByBoardIdOrderByPositionAsc(UUID boardId);

    @Query("SELECT MAX(c.position) FROM BoardColumn c WHERE c.board.id = :boardId")
    Integer findMaxPositionByBoardId(@Param("boardId") UUID boardId);

    long countByBoardId(UUID boardId);
}
