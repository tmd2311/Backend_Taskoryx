package com.taskoryx.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_jobs", indexes = {
    @Index(name = "idx_ai_jobs_user", columnList = "user_id"),
    @Index(name = "idx_ai_jobs_status", columnList = "status"),
    @Index(name = "idx_ai_jobs_created_at", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiJob {

    public enum JobStatus { PENDING, RUNNING, DONE, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ai_jobs_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status;

    /** JSON-serialized AiProjectPlan. */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String planJson;

    /** UUID của project đích (null = tạo mới). */
    @Column(name = "target_project_id")
    private UUID targetProjectId;

    /** UUID project đã tạo sau khi job hoàn thành. */
    @Column(name = "result_project_id")
    private UUID resultProjectId;

    @Column(name = "result_project_key", length = 20)
    private String resultProjectKey;

    @Column(name = "result_project_name")
    private String resultProjectName;

    @Column(name = "tasks_created")
    private Integer tasksCreated;

    @Column(name = "sub_tasks_created")
    private Integer subTasksCreated;

    @Column(name = "sprints_created")
    private Integer sprintsCreated;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;
}
