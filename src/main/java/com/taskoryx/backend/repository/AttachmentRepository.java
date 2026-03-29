package com.taskoryx.backend.repository;

import com.taskoryx.backend.entity.Attachment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {

    List<Attachment> findByTaskIdOrderByCreatedAtDesc(UUID taskId);

    /** File đính kèm của một comment cụ thể */
    List<Attachment> findByCommentIdOrderByCreatedAtAsc(UUID commentId);

    long countByTaskId(UUID taskId);

    /** Toàn bộ file đính kèm của một project, hỗ trợ phân trang */
    Page<Attachment> findByTask_Project_IdOrderByCreatedAtDesc(UUID projectId, Pageable pageable);

    /**
     * Thống kê số lượng file theo fileType (MIME) trong một task.
     * FileCategory được suy ra từ fileType trong Java.
     */
    @Query("SELECT a.fileType AS fileType, COUNT(a) AS count FROM Attachment a WHERE a.task.id = :taskId GROUP BY a.fileType")
    List<AttachmentCategoryStatsProjection> getStatsByTaskId(UUID taskId);

    /**
     * Thống kê số lượng file theo fileType (MIME) trong toàn bộ project.
     * FileCategory được suy ra từ fileType trong Java.
     */
    @Query("SELECT a.fileType AS fileType, COUNT(a) AS count FROM Attachment a WHERE a.task.project.id = :projectId GROUP BY a.fileType")
    List<AttachmentCategoryStatsProjection> getStatsByProjectId(UUID projectId);
}
