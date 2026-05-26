package com.taskoryx.backend.ai.dto.response;

import com.taskoryx.backend.entity.AiJob;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AiJobResponse {

    private UUID jobId;
    private AiJob.JobStatus status;
    private String message;

    private UUID resultProjectId;
    private String resultProjectKey;
    private String resultProjectName;
    private Integer tasksCreated;
    private Integer subTasksCreated;
    private Integer sprintsCreated;

    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    public static AiJobResponse from(AiJob job) {
        return AiJobResponse.builder()
                .jobId(job.getId())
                .status(job.getStatus())
                .message(statusMessage(job.getStatus()))
                .resultProjectId(job.getResultProjectId())
                .resultProjectKey(job.getResultProjectKey())
                .resultProjectName(job.getResultProjectName())
                .tasksCreated(job.getTasksCreated())
                .subTasksCreated(job.getSubTasksCreated())
                .sprintsCreated(job.getSprintsCreated())
                .errorMessage(job.getErrorMessage())
                .createdAt(job.getCreatedAt())
                .startedAt(job.getStartedAt())
                .finishedAt(job.getFinishedAt())
                .build();
    }

    private static String statusMessage(AiJob.JobStatus status) {
        return switch (status) {
            case PENDING -> "Job đang chờ xử lý";
            case RUNNING -> "Đang tạo dự án...";
            case DONE -> "Tạo dự án thành công";
            case FAILED -> "Tạo dự án thất bại";
        };
    }
}
