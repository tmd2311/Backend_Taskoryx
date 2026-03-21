package com.taskoryx.backend.repository;

import com.taskoryx.backend.entity.ChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, UUID> {

    List<ChecklistItem> findByTaskIdOrderByPositionAsc(UUID taskId);

    long countByTaskId(UUID taskId);

    long countByTaskIdAndIsCheckedTrue(UUID taskId);

    @Query("SELECT MAX(c.position) FROM ChecklistItem c WHERE c.task.id = :taskId")
    Optional<Integer> findMaxPositionByTaskId(@Param("taskId") UUID taskId);

    void deleteByTaskId(UUID taskId);
}
