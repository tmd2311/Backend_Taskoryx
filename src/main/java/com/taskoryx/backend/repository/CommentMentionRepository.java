package com.taskoryx.backend.repository;

import com.taskoryx.backend.entity.CommentMention;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CommentMentionRepository extends JpaRepository<CommentMention, UUID> {

    @Modifying
    @Query("DELETE FROM CommentMention cm WHERE cm.comment.id = :commentId")
    void deleteByCommentId(@Param("commentId") UUID commentId);
}
