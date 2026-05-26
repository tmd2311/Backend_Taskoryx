package com.taskoryx.backend.repository;

import com.taskoryx.backend.entity.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID> {

    Page<ActivityLog> findByProjectIdOrderByCreatedAtDesc(UUID projectId, Pageable pageable);

    Page<ActivityLog> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<ActivityLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            ActivityLog.EntityType entityType, UUID entityId);

    /**
     * Lấy toàn bộ activity liên quan đến một task:
     * - Hành động trực tiếp trên task (entityType=TASK, entityId=taskId)
     * - Comment/Attachment thuộc task này (entityType=COMMENT/ATTACHMENT, lưu taskId trong newValue/oldValue)
     *   → dùng entityTitle chứa taskKey để join, hoặc dùng project + description LIKE
     * Cách đơn giản nhất: lưu taskId vào một cột riêng khi log comment — nhưng hiện tại
     * dùng JPQL lấy tất cả log trong project có entityId = taskId HOẶC description chứa taskKey.
     * Thực tế gọn hơn: thêm cột task_id nullable vào activity_logs.
     *
     * Tạm thời: query theo project + (entityType=TASK,entityId=taskId) OR (entityType IN (COMMENT,ATTACHMENT) AND newValue/oldValue chứa taskId)
     */
    @Query("""
            SELECT a FROM ActivityLog a
            WHERE a.project.id = :projectId
              AND (
                (a.entityType = 'TASK' AND a.entityId = :taskId)
                OR (a.entityType IN ('COMMENT', 'ATTACHMENT') AND (
                    a.newValue LIKE %:taskIdStr%
                    OR a.oldValue LIKE %:taskIdStr%
                ))
              )
            ORDER BY a.createdAt DESC
            """)
    List<ActivityLog> findAllByTaskId(
            @Param("projectId") UUID projectId,
            @Param("taskId") UUID taskId,
            @Param("taskIdStr") String taskIdStr);

    @Query("SELECT COUNT(a) FROM ActivityLog a WHERE a.user.id = :userId AND a.project.id = :projectId")
    long countByUserIdAndProjectId(@Param("userId") UUID userId, @Param("projectId") UUID projectId);

    @Query("SELECT a.user.id, COUNT(a) FROM ActivityLog a WHERE a.project.id = :projectId GROUP BY a.user.id")
    List<Object[]> countPerUserByProjectId(@Param("projectId") UUID projectId);

    /** Đếm số lần hoàn thành task (action = COMPLETE) theo user trong project — dùng cho performance scoring */
    @Query("SELECT a.user.id, COUNT(a) FROM ActivityLog a WHERE a.project.id = :projectId AND a.action = 'COMPLETE' AND a.entityType = 'TASK' GROUP BY a.user.id")
    List<Object[]> countCompletionsByUserInProject(@Param("projectId") UUID projectId);
}
