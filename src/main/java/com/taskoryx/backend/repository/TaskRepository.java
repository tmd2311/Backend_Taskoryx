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
import java.time.LocalDateTime;
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

    /**
     * Product Backlog: tasks chưa có column VÀ không thuộc sprint nào đang PLANNED/ACTIVE
     */
    @Query("SELECT t FROM Task t WHERE t.project.id = :projectId AND t.column IS NULL " +
           "AND t.id NOT IN (SELECT st.id FROM Sprint s JOIN s.tasks st " +
           "WHERE s.project.id = :projectId AND s.status IN ('PLANNED', 'ACTIVE')) " +
           "ORDER BY t.createdAt DESC")
    List<Task> findProductBacklog(@Param("projectId") UUID projectId);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.id = :projectId AND t.status = :status")
    long countByProjectIdAndStatus(@Param("projectId") UUID projectId, @Param("status") Task.TaskStatus status);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.assignee.id = :userId AND t.status NOT IN ('DONE', 'CANCELLED', 'RESOLVED')")
    long countActiveByAssigneeId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.assignee.id = :userId AND t.dueDate = :today AND t.completedAt IS NULL")
    long countDueTodayByAssigneeId(@Param("userId") UUID userId, @Param("today") LocalDate today);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.assignee.id = :userId AND t.completedAt >= :weekStart AND t.completedAt <= :weekEnd")
    long countCompletedByAssigneeIdBetween(@Param("userId") UUID userId,
                                           @Param("weekStart") LocalDateTime weekStart,
                                           @Param("weekEnd") LocalDateTime weekEnd);

    List<Task> findByAssigneeIdOrderByDueDateAsc(UUID assigneeId);

    @Query("SELECT t FROM Task t WHERE t.assignee.id = :userId AND t.dueDate < :today AND t.completedAt IS NULL")
    List<Task> findOverdueTasksByAssigneeId(@Param("userId") UUID userId, @Param("today") LocalDate today);

    @Query("SELECT t FROM Task t WHERE t.project.key = :projectKey AND t.taskNumber = :taskNumber")
    Optional<Task> findByProjectKeyAndTaskNumber(@Param("projectKey") String projectKey,
                                                  @Param("taskNumber") Integer taskNumber);
}
