package com.taskoryx.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "webhooks", indexes = {
    @Index(name = "idx_webhooks_project", columnList = "project_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Webhook {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, foreignKey = @ForeignKey(name = "fk_webhooks_project"))
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, foreignKey = @ForeignKey(name = "fk_webhooks_user"))
    private User createdBy;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "secret_token", length = 255)
    private String secretToken;

    @Column(nullable = false)
    @Builder.Default
    private boolean isActive = true;

    // JSON array of event types to listen for, e.g. ["TASK_CREATED","TASK_UPDATED"]
    @Column(name = "events", columnDefinition = "TEXT")
    private String events; // stored as comma-separated

    @Column(name = "last_triggered_at")
    private LocalDateTime lastTriggeredAt;

    @Column(name = "success_count")
    @Builder.Default
    private int successCount = 0;

    @Column(name = "failure_count")
    @Builder.Default
    private int failureCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum WebhookEvent {
        TASK_CREATED, TASK_UPDATED, TASK_DELETED, TASK_MOVED, TASK_ASSIGNED,
        COMMENT_CREATED, COMMENT_DELETED,
        MEMBER_ADDED, MEMBER_REMOVED,
        SPRINT_STARTED, SPRINT_COMPLETED
    }
}
