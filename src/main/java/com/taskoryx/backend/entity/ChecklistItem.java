package com.taskoryx.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity class representing a Checklist Item within a Task
 * Maps to 'checklist_items' table in database
 */
@Entity
@Table(name = "checklist_items", indexes = {
    @Index(name = "idx_checklist_task", columnList = "task_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false, foreignKey = @ForeignKey(name = "fk_checklist_task"))
    private Task task;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(name = "is_checked", nullable = false)
    @Builder.Default
    private boolean isChecked = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checked_by_id", foreignKey = @ForeignKey(name = "fk_checklist_checked_by"))
    private User checkedBy;

    @Column(name = "checked_at")
    private LocalDateTime checkedAt;

    @Column(name = "position", nullable = false)
    @Builder.Default
    private int position = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Mark this item as checked by the given user
     */
    public void check(User user) {
        this.isChecked = true;
        this.checkedBy = user;
        this.checkedAt = LocalDateTime.now();
    }

    /**
     * Mark this item as unchecked
     */
    public void uncheck() {
        this.isChecked = false;
        this.checkedBy = null;
        this.checkedAt = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChecklistItem)) return false;
        return id != null && id.equals(((ChecklistItem) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "ChecklistItem{" +
                "id=" + id +
                ", content='" + content + '\'' +
                ", isChecked=" + isChecked +
                ", position=" + position +
                '}';
    }
}
