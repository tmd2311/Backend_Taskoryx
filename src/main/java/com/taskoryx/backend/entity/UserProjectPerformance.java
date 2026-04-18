package com.taskoryx.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "user_project_performance",
    indexes = {
        @Index(name = "idx_perf_project", columnList = "project_id"),
        @Index(name = "idx_perf_user", columnList = "user_id"),
        @Index(name = "idx_perf_sprint", columnList = "sprint_id")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProjectPerformance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id")
    private Sprint sprint; // null = project-level evaluation

    @Column(name = "on_time_score", precision = 5, scale = 2)
    private BigDecimal onTimeScore;

    @Column(name = "completion_score", precision = 5, scale = 2)
    private BigDecimal completionScore;

    @Column(name = "time_accuracy_score", precision = 5, scale = 2)
    private BigDecimal timeAccuracyScore;

    @Column(name = "engagement_score", precision = 5, scale = 2)
    private BigDecimal engagementScore;

    @Column(name = "total_score", precision = 5, scale = 2)
    private BigDecimal totalScore;

    @Column(name = "rank")
    private Integer rank;

    @Column(name = "task_count")
    private Integer taskCount;

    @Column(name = "completed_count")
    private Integer completedCount;

    @Column(name = "overdue_count")
    private Integer overdueCount;

    @Column(name = "evaluated_at")
    private LocalDateTime evaluatedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
