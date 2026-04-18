package com.taskoryx.backend.repository;

import com.taskoryx.backend.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;


@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {

    // Lấy comment gốc (không có parent)
    @Query("SELECT c FROM Comment c WHERE c.task.id = :taskId AND c.parent IS NULL " +
           "ORDER BY c.createdAt ASC")
    List<Comment> findRootCommentsByTaskId(@Param("taskId") UUID taskId);

    long countByTaskId(UUID taskId);

    List<Comment> findByParentId(UUID parentId);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.user.id = :userId AND c.task.project.id = :projectId")
    long countByUserIdAndProjectId(@Param("userId") UUID userId, @Param("projectId") UUID projectId);

    @Query("SELECT c.user.id, COUNT(c) FROM Comment c WHERE c.task.project.id = :projectId GROUP BY c.user.id")
    List<Object[]> countPerUserByProjectId(@Param("projectId") UUID projectId);
}
