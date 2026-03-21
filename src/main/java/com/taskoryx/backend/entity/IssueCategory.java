package com.taskoryx.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "issue_categories", indexes = {
    @Index(name = "idx_categories_project", columnList = "project_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IssueCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, foreignKey = @ForeignKey(name = "fk_categories_project"))
    private Project project;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_assignee_id", foreignKey = @ForeignKey(name = "fk_categories_assignee"))
    private User defaultAssignee;

    @OneToMany(mappedBy = "category")
    @Builder.Default
    private Set<Task> tasks = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IssueCategory)) return false;
        return id != null && id.equals(((IssueCategory) o).getId());
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }

    @Override
    public String toString() {
        return "IssueCategory{id=" + id + ", name='" + name + "'}";
    }
}
