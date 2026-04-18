package com.taskoryx.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
    name = "project_roles",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_project_role_name", columnNames = {"project_id", "name"})
    },
    indexes = {
        @Index(name = "idx_project_roles_project", columnList = "project_id")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectRole {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean isDefault = false;

    /**
     * Comma-separated permission keys, e.g. "TASK_VIEW,TASK_CREATE,COMMENT_CREATE"
     * See ProjectPermission for valid values.
     */
    @Column(name = "permissions", columnDefinition = "TEXT")
    private String permissions;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Transient
    public List<String> getPermissionList() {
        if (permissions == null || permissions.isBlank()) return List.of();
        return Arrays.asList(permissions.split(","));
    }

    public void setPermissionList(List<String> perms) {
        this.permissions = (perms != null && !perms.isEmpty())
                ? String.join(",", perms)
                : null;
    }
}
