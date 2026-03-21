package com.taskoryx.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "versions", indexes = {
    @Index(name = "idx_versions_project", columnList = "project_id"),
    @Index(name = "idx_versions_status", columnList = "status")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Version {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, foreignKey = @ForeignKey(name = "fk_versions_project"))
    private Project project;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private VersionStatus status = VersionStatus.OPEN;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @OneToMany(mappedBy = "version")
    @Builder.Default
    private Set<Task> tasks = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum VersionStatus {
        OPEN, LOCKED, CLOSED
    }

    @Transient
    public int getTotalTasks() { return tasks.size(); }

    @Transient
    public int getCompletedTasks() {
        return (int) tasks.stream().filter(Task::isCompleted).count();
    }

    @Transient
    public int getCompletionPercent() {
        int total = getTotalTasks();
        if (total == 0) return 0;
        return (int) Math.round((double) getCompletedTasks() / total * 100);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Version)) return false;
        return id != null && id.equals(((Version) o).getId());
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }

    @Override
    public String toString() {
        return "Version{id=" + id + ", name='" + name + "', status=" + status + '}';
    }
}
