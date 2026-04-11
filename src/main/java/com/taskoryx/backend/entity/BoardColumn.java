package com.taskoryx.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Entity class representing a Column (Status) in Kanban Board
 * Maps to 'columns' table in database
 */
@Entity
@Table(name = "columns", indexes = {
    @Index(name = "idx_columns_board", columnList = "board_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardColumn {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "board_id", nullable = false, foreignKey = @ForeignKey(name = "fk_columns_board"))
    private Board board;

    @NotBlank(message = "{column.name.required}")
    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private Integer position;

    @Column(length = 7)
    private String color;

    @Column(name = "is_completed", nullable = false)
    @Builder.Default
    private Boolean isCompleted = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "mapped_status", length = 20)
    private Task.TaskStatus mappedStatus;

    @Column(name = "task_limit")
    private Integer taskLimit;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "column")
    @OrderBy("position ASC")
    @Builder.Default
    private Set<Task> tasks = new HashSet<>();

    /**
     * Check if column has reached WIP limit
     */
    @Transient
    public boolean hasReachedLimit() {
        return taskLimit != null && tasks.size() >= taskLimit;
    }

    // Helper methods
    public void addTask(Task task) {
        tasks.add(task);
        task.setColumn(this);
    }

    public void removeTask(Task task) {
        tasks.remove(task);
        task.setColumn(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BoardColumn)) return false;
        return id != null && id.equals(((BoardColumn) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "BoardColumn{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", position=" + position +
                ", isCompleted=" + isCompleted +
                '}';
    }
}
