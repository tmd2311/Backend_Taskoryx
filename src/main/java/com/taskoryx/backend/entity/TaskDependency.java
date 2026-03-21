package com.taskoryx.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity class representing Task Dependencies
 * Maps to 'task_dependencies' table in database
 */
@Entity
@Table(name = "task_dependencies",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_task_dependency", columnNames = {"task_id", "depends_on_task_id"})
    },
    indexes = {
        @Index(name = "idx_dependencies_task", columnList = "task_id"),
        @Index(name = "idx_dependencies_depends", columnList = "depends_on_task_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false, foreignKey = @ForeignKey(name = "fk_dependencies_task"))
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "depends_on_task_id", nullable = false, foreignKey = @ForeignKey(name = "fk_dependencies_depends"))
    private Task dependsOnTask;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DependencyType type = DependencyType.BLOCKS;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Enum for dependency types
     */
    public enum DependencyType {
        BLOCKS,      // task chặn dependsOnTask (B không thể bắt đầu khi A chưa xong)
        DEPENDS_ON,  // task phụ thuộc vào dependsOnTask (A không thể bắt đầu khi B chưa xong)
        RELATES_TO,  // task liên quan đến dependsOnTask
        DUPLICATES,  // task trùng với dependsOnTask
        PRECEDES,    // task đứng trước dependsOnTask
        FOLLOWS      // task đứng sau dependsOnTask
    }

    /**
     * Check if dependency is blocking type
     */
    @Transient
    public boolean isBlocking() {
        return type == DependencyType.BLOCKS || type == DependencyType.DEPENDS_ON;
    }

    /**
     * Get dependency description
     */
    @Transient
    public String getDescription() {
        if (task == null || dependsOnTask == null) return "";
        String a = task.getTaskKey();
        String b = dependsOnTask.getTaskKey();
        return switch (type) {
            case BLOCKS -> a + " blocks " + b;
            case DEPENDS_ON -> a + " depends on " + b;
            case RELATES_TO -> a + " relates to " + b;
            case DUPLICATES -> a + " duplicates " + b;
            case PRECEDES -> a + " precedes " + b;
            case FOLLOWS -> a + " follows " + b;
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskDependency)) return false;
        return id != null && id.equals(((TaskDependency) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "TaskDependency{" +
                "id=" + id +
                ", type=" + type +
                ", createdAt=" + createdAt +
                '}';
    }
}
