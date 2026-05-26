package com.taskoryx.backend.ai.dto.response;

import com.taskoryx.backend.entity.AiGenerateSession;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AiGenerateSessionResponse {

    private UUID sessionId;
    private AiGenerateSession.SessionStatus status;
    private String message;

    private String requirement;

    // Chỉ có khi status = READY
    private AiProjectPlan plan;
    private Integer totalTaskCount;
    private String modelUsed;

    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime finishedAt;

    public static AiGenerateSessionResponse from(AiGenerateSession session, AiProjectPlan plan) {
        return AiGenerateSessionResponse.builder()
                .sessionId(session.getId())
                .status(session.getStatus())
                .message(statusMessage(session.getStatus()))
                .requirement(session.getRequirement())
                .plan(plan)
                .totalTaskCount(session.getTotalTaskCount())
                .modelUsed(session.getModelUsed())
                .errorMessage(session.getErrorMessage())
                .createdAt(session.getCreatedAt())
                .finishedAt(session.getFinishedAt())
                .build();
    }

    public static AiGenerateSessionResponse pending(AiGenerateSession session) {
        return AiGenerateSessionResponse.builder()
                .sessionId(session.getId())
                .status(session.getStatus())
                .message(statusMessage(session.getStatus()))
                .requirement(session.getRequirement())
                .createdAt(session.getCreatedAt())
                .build();
    }

    private static String statusMessage(AiGenerateSession.SessionStatus status) {
        return switch (status) {
            case GENERATING -> "AI đang phân tích yêu cầu...";
            case READY      -> "Kế hoạch đã sẵn sàng";
            case FAILED     -> "Sinh kế hoạch thất bại";
        };
    }
}
