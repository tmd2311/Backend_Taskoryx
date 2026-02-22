package com.taskoryx.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity class representing User Mention in Comment
 * Maps to 'comment_mentions' table in database
 */
@Entity
@Table(name = "comment_mentions",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_comment_mention", columnNames = {"comment_id", "user_id"})
    },
    indexes = {
        @Index(name = "idx_mentions_comment", columnList = "comment_id"),
        @Index(name = "idx_mentions_user", columnList = "user_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentMention {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id", nullable = false, foreignKey = @ForeignKey(name = "fk_mentions_comment"))
    private Comment comment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_mentions_user"))
    private User user;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommentMention)) return false;
        return id != null && id.equals(((CommentMention) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "CommentMention{" +
                "id=" + id +
                ", createdAt=" + createdAt +
                '}';
    }
}
