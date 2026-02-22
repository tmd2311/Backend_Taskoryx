package com.taskoryx.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity class representing Task-Label relationship (Many-to-Many)
 * Maps to 'task_labels' table in database
 */
@Entity
@Table(name = "task_labels",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_task_label", columnNames = {"task_id", "label_id"})
    },
    indexes = {
        @Index(name = "idx_task_labels_task", columnList = "task_id"),
        @Index(name = "idx_task_labels_label", columnList = "label_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskLabel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false, foreignKey = @ForeignKey(name = "fk_task_labels_task"))
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "label_id", nullable = false, foreignKey = @ForeignKey(name = "fk_task_labels_label"))
    private Label label;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskLabel)) return false;
        return id != null && id.equals(((TaskLabel) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "TaskLabel{" +
                "id=" + id +
                ", createdAt=" + createdAt +
                '}';
    }
}
