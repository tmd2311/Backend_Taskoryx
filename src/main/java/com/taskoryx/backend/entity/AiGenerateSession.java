package com.taskoryx.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_generate_sessions", indexes = {
    @Index(name = "idx_ai_gen_sessions_user", columnList = "user_id"),
    @Index(name = "idx_ai_gen_sessions_status", columnList = "status"),
    @Index(name = "idx_ai_gen_sessions_created_at", columnList = "createdAt")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiGenerateSession {

    public enum SessionStatus { GENERATING, READY, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ai_gen_sessions_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String requirement;

    @Column(nullable = false, length = 10)
    private String language;

    /** JSON-serialized AiProjectPlan — null khi đang GENERATING */
    @Column(columnDefinition = "TEXT")
    private String planJson;

    @Column(name = "total_task_count")
    private Integer totalTaskCount;

    @Column(name = "model_used", length = 100)
    private String modelUsed;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;
}
