package com.taskoryx.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Entity class representing a Task
 * Maps to 'tasks' table in database
 */
@Entity
@Table(name = "tasks",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_project_task_number", columnNames = {"project_id", "task_number"})
    },
    indexes = {
        @Index(name = "idx_tasks_project", columnList = "project_id"),
        @Index(name = "idx_tasks_board", columnList = "board_id"),
        @Index(name = "idx_tasks_column", columnList = "column_id"),
        @Index(name = "idx_tasks_assignee", columnList = "assignee_id"),
        @Index(name = "idx_tasks_reporter", columnList = "reporter_id"),
        @Index(name = "idx_tasks_due_date", columnList = "dueDate"),
        @Index(name = "idx_tasks_priority", columnList = "priority")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tasks_project"))
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "board_id", nullable = true, foreignKey = @ForeignKey(name = "fk_tasks_board"))
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "column_id", nullable = true, foreignKey = @ForeignKey(name = "fk_tasks_column"))
    private BoardColumn column;

    @Column(name = "task_number", nullable = false)
    private Integer taskNumber;

    @NotBlank(message = "{task.title.required}")
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TaskPriority priority = TaskPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TaskStatus status = TaskStatus.TODO;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal position = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id", foreignKey = @ForeignKey(name = "fk_tasks_assignee"))
    private User assignee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tasks_reporter"))
    private User reporter;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "estimated_hours", precision = 5, scale = 2)
    private BigDecimal estimatedHours;

    @Column(name = "actual_hours", precision = 5, scale = 2)
    private BigDecimal actualHours;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Comment> comments = new HashSet<>();

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Attachment> attachments = new HashSet<>();

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<TaskLabel> taskLabels = new HashSet<>();

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<TimeTracking> timeEntries = new HashSet<>();

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<TaskDependency> dependencies = new HashSet<>();

    @OneToMany(mappedBy = "dependsOnTask", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<TaskDependency> dependents = new HashSet<>();

    /**
     * Enum for task priority
     */
    public enum TaskPriority {
        LOW,
        MEDIUM,
        HIGH,
        URGENT
    }

    /**
     * Enum for task status (độc lập với Kanban column)
     */
    public enum TaskStatus {
        TODO,        // Sẽ làm
        IN_PROGRESS, // Đang làm
        IN_REVIEW,   // Đang xem xét / review
        RESOLVED,    // Đã xử lý
        DONE,        // Đã hoàn thành
        CANCELLED    // Đã hủy
    }

    /**
     * Get task key (e.g., PROJ-123)
     */
    @Transient
    public String getTaskKey() {
        if (project != null && taskNumber != null) {
            return project.getKey() + "-" + taskNumber;
        }
        return null;
    }

    /**
     * Check if task is overdue
     */
    @Transient
    public boolean isOverdue() {
        return dueDate != null &&
               completedAt == null &&
               LocalDate.now().isAfter(dueDate);
    }

    /**
     * Check if task is completed
     */
    @Transient
    public boolean isCompleted() {
        return completedAt != null;
    }

    // Helper methods
    public void addComment(Comment comment) {
        comments.add(comment);
        comment.setTask(this);
    }

    public void removeComment(Comment comment) {
        comments.remove(comment);
        comment.setTask(null);
    }

    public void addAttachment(Attachment attachment) {
        attachments.add(attachment);
        attachment.setTask(this);
    }

    public void removeAttachment(Attachment attachment) {
        attachments.remove(attachment);
        attachment.setTask(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task)) return false;
        return id != null && id.equals(((Task) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", taskKey='" + getTaskKey() + '\'' +
                ", title='" + title + '\'' +
                ", priority=" + priority +
                '}';
    }
}
