package com.taskoryx.backend.repository;

import com.taskoryx.backend.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {

    List<Task> findByColumnIdOrderByPositionAsc(UUID columnId);

    Page<Task> findByProjectId(UUID projectId, Pageable pageable);

    Page<Task> findByAssigneeId(UUID assigneeId, Pageable pageable);

    @Query("SELECT MAX(t.taskNumber) FROM Task t WHERE t.project.id = :projectId")
    Optional<Integer> findMaxTaskNumberByProjectId(@Param("projectId") UUID projectId);

    @Query("SELECT MAX(t.position) FROM Task t WHERE t.column.id = :columnId")
    Optional<java.math.BigDecimal> findMaxPositionByColumnId(@Param("columnId") UUID columnId);

    // Tìm task sắp đến hạn (dùng cho reminder)
    @Query("SELECT t FROM Task t WHERE t.dueDate = :dueDate AND t.completedAt IS NULL")
    List<Task> findTasksDueOn(@Param("dueDate") LocalDate dueDate);

    // Tìm task quá hạn
    @Query("SELECT t FROM Task t WHERE t.dueDate < :today AND t.completedAt IS NULL")
    List<Task> findOverdueTasks(@Param("today") LocalDate today);

    // Tìm kiếm task trong project
    @Query("SELECT t FROM Task t WHERE t.project.id = :projectId AND " +
           "(LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Task> searchByProjectId(@Param("projectId") UUID projectId,
                                  @Param("keyword") String keyword,
                                  Pageable pageable);

    long countByProjectIdAndColumnId(UUID projectId, UUID columnId);

    long countByProjectId(UUID projectId);

    List<Task> findByProjectIdAndColumnIsNullOrderByCreatedAtDesc(UUID projectId);
}
