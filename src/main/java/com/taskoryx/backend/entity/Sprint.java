package com.taskoryx.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Entity class representing a Sprint
 * Maps to 'sprints' table in database
 */
@Entity
@Table(name = "sprints",
    indexes = {
        @Index(name = "idx_sprints_project", columnList = "project_id"),
        @Index(name = "idx_sprints_status", columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sprint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, foreignKey = @ForeignKey(name = "fk_sprints_project"))
    private Project project;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String goal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SprintStatus status = SprintStatus.PLANNED;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", foreignKey = @ForeignKey(name = "fk_sprints_board"))
    private Board board;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "sprint_tasks",
        joinColumns = @JoinColumn(name = "sprint_id"),
        inverseJoinColumns = @JoinColumn(name = "task_id"))
    @Builder.Default
    private Set<Task> tasks = new HashSet<>();

    /**
     * Enum for sprint status
     */
    public enum SprintStatus {
        PLANNED,
        ACTIVE,
        COMPLETED,
        CANCELLED
    }

    // Helper methods

    @Transient
    public boolean isActive() {
        return SprintStatus.ACTIVE == this.status;
    }

    @Transient
    public boolean isCompleted() {
        return SprintStatus.COMPLETED == this.status;
    }

    /**
     * Get duration in days between startDate and endDate
     * Returns -1 if either date is null
     */
    @Transient
    public long getDuration() {
        if (startDate == null || endDate == null) {
            return -1;
        }
        return ChronoUnit.DAYS.between(startDate, endDate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Sprint)) return false;
        return id != null && id.equals(((Sprint) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Sprint{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", status=" + status +
                '}';
    }
}
