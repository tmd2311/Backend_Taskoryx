package com.taskoryx.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

// Roles là chuỗi tự do, admin tự định nghĩa. Hai giá trị đặc biệt: "OWNER", "ADMIN".

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity class representing Project Membership
 * Maps to 'project_members' table in database
 */
@Entity
@Table(name = "project_members",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_project_user", columnNames = {"project_id", "user_id"})
    },
    indexes = {
        @Index(name = "idx_project_members_project", columnList = "project_id"),
        @Index(name = "idx_project_members_user", columnList = "user_id"),
        @Index(name = "idx_project_members_role", columnList = "role")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, foreignKey = @ForeignKey(name = "fk_project_members_project"))
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_project_members_user"))
    private User user;

    @Column(nullable = false, length = 50)
    private String role;

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProjectMember)) return false;
        return id != null && id.equals(((ProjectMember) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "ProjectMember{" +
                "id=" + id +
                ", role=" + role +
                ", joinedAt=" + joinedAt +
                '}';
    }
}
