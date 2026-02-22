package com.taskoryx.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Join entity for User <-> Role relationship
 * Maps to 'user_roles' table in database
 */
@Entity
@Table(name = "user_roles",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_user_role", columnNames = {"user_id", "role_id"})
    },
    indexes = {
        @Index(name = "idx_user_roles_user", columnList = "user_id"),
        @Index(name = "idx_user_roles_role", columnList = "role_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_roles_user"))
    private User user;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "role_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_roles_role"))
    private Role role;

    /**
     * Người gán role (null nếu do hệ thống tự gán)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by", foreignKey = @ForeignKey(name = "fk_user_roles_assigned_by"))
    private User assignedBy;

    @CreationTimestamp
    @Column(name = "assigned_at", nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserRole)) return false;
        return id != null && id.equals(((UserRole) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
