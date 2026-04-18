package com.taskoryx.backend.dto.response.performance;

import com.taskoryx.backend.entity.UserProjectPerformance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPerformanceResponse {

    private UUID id;
    private UUID userId;
    private String username;
    private String fullName;
    private String avatarUrl;

    private UUID projectId;
    private String projectName;

    private BigDecimal onTimeScore;
    private BigDecimal completionScore;
    private BigDecimal timeAccuracyScore;
    private BigDecimal engagementScore;
    private BigDecimal totalScore;

    private Integer rank;
    private Integer taskCount;
    private Integer completedCount;
    private Integer overdueCount;

    private LocalDateTime evaluatedAt;

    private UUID sprintId;
    private String sprintName;

    public static UserPerformanceResponse from(UserProjectPerformance perf) {
        return UserPerformanceResponse.builder()
                .id(perf.getId())
                .userId(perf.getUser().getId())
                .username(perf.getUser().getUsername())
                .fullName(perf.getUser().getFullName())
                .avatarUrl(perf.getUser().getAvatarUrl())
                .projectId(perf.getProject().getId())
                .projectName(perf.getProject().getName())
                .onTimeScore(perf.getOnTimeScore())
                .completionScore(perf.getCompletionScore())
                .timeAccuracyScore(perf.getTimeAccuracyScore())
                .engagementScore(perf.getEngagementScore())
                .totalScore(perf.getTotalScore())
                .rank(perf.getRank())
                .taskCount(perf.getTaskCount())
                .completedCount(perf.getCompletedCount())
                .overdueCount(perf.getOverdueCount())
                .evaluatedAt(perf.getEvaluatedAt())
                .sprintId(perf.getSprint() != null ? perf.getSprint().getId() : null)
                .sprintName(perf.getSprint() != null ? perf.getSprint().getName() : null)
                .build();
    }
}
