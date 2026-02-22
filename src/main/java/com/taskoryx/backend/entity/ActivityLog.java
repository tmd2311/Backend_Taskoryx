package com.taskoryx.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity class representing Activity Log (Audit Trail)
 * Maps to 'activity_logs' table in database
 */
@Entity
@Table(name = "activity_logs", indexes = {
    @Index(name = "idx_activity_logs_user", columnList = "user_id"),
    @Index(name = "idx_activity_logs_project", columnList = "project_id"),
    @Index(name = "idx_activity_logs_entity", columnList = "entityType, entityId"),
    @Index(name = "idx_activity_logs_created_at", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_activity_logs_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", foreignKey = @ForeignKey(name = "fk_activity_logs_project"))
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 50)
    private EntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Action action;

    @Column(name = "old_value", columnDefinition = "jsonb")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "jsonb")
    private String newValue;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Enum for entity types
     */
    public enum EntityType {
        TASK,
        COMMENT,
        PROJECT,
        BOARD,
        COLUMN,
        ATTACHMENT
    }

    /**
     * Enum for actions
     */
    public enum Action {
        CREATE,
        UPDATE,
        DELETE,
        MOVE,
        ASSIGN,
        COMPLETE
    }

    /**
     * Get formatted action description
     */
    @Transient
    public String getActionDescription() {
        return String.format("%s %s %s",
                user != null ? user.getFullName() : "Unknown",
                action.name().toLowerCase(),
                entityType.name().toLowerCase()
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ActivityLog)) return false;
        return id != null && id.equals(((ActivityLog) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "ActivityLog{" +
                "id=" + id +
                ", entityType=" + entityType +
                ", action=" + action +
                ", createdAt=" + createdAt +
                '}';
    }
}
