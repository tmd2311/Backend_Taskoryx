package com.taskoryx.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity class representing a Notification
 * Maps to 'notifications' table in database
 */
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notifications_user", columnList = "user_id"),
    @Index(name = "idx_notifications_is_read", columnList = "user_id, isRead"),
    @Index(name = "idx_notifications_created_at", columnList = "createdAt"),
    @Index(name = "idx_notifications_type", columnList = "type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_notifications_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @NotBlank(message = "{notification.title.required}")
    @Column(nullable = false)
    private String title;

    @NotBlank(message = "{notification.message.required}")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "related_type", length = 50)
    private RelatedType relatedType;

    @Column(name = "related_id")
    private UUID relatedId;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "sent_email", nullable = false)
    @Builder.Default
    private Boolean sentEmail = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    /**
     * Enum for notification types
     */
    public enum NotificationType {
        TASK_ASSIGNED,
        TASK_UPDATED,
        TASK_COMMENTED,
        MENTION,
        DUE_DATE_REMINDER,
        PROJECT_INVITE
    }

    /**
     * Enum for related entity types
     */
    public enum RelatedType {
        TASK,
        COMMENT,
        PROJECT
    }

    /**
     * Mark notification as read
     */
    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }

    /**
     * Check if notification is unread
     */
    @Transient
    public boolean isUnread() {
        return !isRead;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Notification)) return false;
        return id != null && id.equals(((Notification) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", type=" + type +
                ", title='" + title + '\'' +
                ", isRead=" + isRead +
                '}';
    }
}
