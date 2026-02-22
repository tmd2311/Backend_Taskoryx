package com.taskoryx.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Entity class representing a Project/Workspace
 * Maps to 'projects' table in database
 */
@Entity
@Table(name = "projects", indexes = {
    @Index(name = "idx_projects_owner", columnList = "owner_id"),
    @Index(name = "idx_projects_key", columnList = "key"),
    @Index(name = "idx_projects_is_archived", columnList = "isArchived")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "{project.name.required}")
    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotBlank(message = "{project.key.required}")
    @Pattern(regexp = "^[A-Z0-9]{2,10}$", message = "{project.key.pattern}")
    @Column(unique = true, nullable = false, length = 10)
    private String key;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, foreignKey = @ForeignKey(name = "fk_projects_owner"))
    private User owner;

    @Column(length = 50)
    private String icon;

    @Column(nullable = false, length = 7)
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "{project.color.pattern}")
    @Builder.Default
    private String color = "#1976d2";

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private Boolean isPublic = false;

    @Column(name = "is_archived", nullable = false)
    @Builder.Default
    private Boolean isArchived = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ProjectMember> members = new HashSet<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Board> boards = new HashSet<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Task> tasks = new HashSet<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Label> labels = new HashSet<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ActivityLog> activityLogs = new HashSet<>();

    // Helper methods
    public void addMember(ProjectMember member) {
        members.add(member);
        member.setProject(this);
    }

    public void removeMember(ProjectMember member) {
        members.remove(member);
        member.setProject(null);
    }

    public void addBoard(Board board) {
        boards.add(board);
        board.setProject(this);
    }

    public void removeBoard(Board board) {
        boards.remove(board);
        board.setProject(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Project)) return false;
        return id != null && id.equals(((Project) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Project{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", key='" + key + '\'' +
                ", isArchived=" + isArchived +
                '}';
    }
}
