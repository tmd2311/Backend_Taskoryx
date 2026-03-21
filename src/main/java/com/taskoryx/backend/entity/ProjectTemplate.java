package com.taskoryx.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "project_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String category; // e.g., "Software", "Marketing", "Design"

    @Column(length = 10)
    private String icon;

    @Column(length = 7)
    private String color;

    // JSON configuration for board columns, e.g.:
    // [{"name":"To Do","color":"#gray","isCompleted":false},{"name":"Done","color":"#green","isCompleted":true}]
    @Column(name = "columns_config", columnDefinition = "TEXT")
    private String columnsConfig;

    @Column(name = "is_public")
    @Builder.Default
    private boolean isPublic = true; // system templates are public

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", foreignKey = @ForeignKey(name = "fk_templates_user"))
    private User createdBy; // null = system template

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
