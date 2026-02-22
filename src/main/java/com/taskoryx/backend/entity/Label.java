package com.taskoryx.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Entity class representing a Label/Tag
 * Maps to 'labels' table in database
 */
@Entity
@Table(name = "labels",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_project_label_name", columnNames = {"project_id", "name"})
    },
    indexes = {
        @Index(name = "idx_labels_project", columnList = "project_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Label {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, foreignKey = @ForeignKey(name = "fk_labels_project"))
    private Project project;

    @NotBlank(message = "{label.name.required}")
    @Column(nullable = false, length = 50)
    private String name;

    @NotBlank(message = "{label.color.required}")
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "{label.color.pattern}")
    @Column(nullable = false, length = 7)
    private String color;

    @Column(length = 200)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Relationships
    @OneToMany(mappedBy = "label", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<TaskLabel> taskLabels = new HashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Label)) return false;
        return id != null && id.equals(((Label) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Label{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", color='" + color + '\'' +
                '}';
    }
}
