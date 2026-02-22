package com.taskoryx.backend.dto.response.task;

import com.taskoryx.backend.dto.response.label.LabelResponse;
import com.taskoryx.backend.entity.Task;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Response đầy đủ cho task detail
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {

    private UUID id;
    private String taskKey;
    private String title;
    private String description;
    private Task.TaskPriority priority;
    private BigDecimal position;

    // Project info
    private UUID projectId;
    private String projectName;
    private String projectKey;

    // Board info
    private UUID boardId;
    private String boardName;

    // Column info
    private UUID columnId;
    private String columnName;

    // Assignee info
    private UUID assigneeId;
    private String assigneeName;
    private String assigneeAvatar;

    // Reporter info
    private UUID reporterId;
    private String reporterName;
    private String reporterAvatar;

    // Dates
    private LocalDate startDate;
    private LocalDate dueDate;
    private LocalDateTime completedAt;

    // Hours
    private BigDecimal estimatedHours;
    private BigDecimal actualHours;

    // Stats
    private int commentCount;
    private int attachmentCount;
    private boolean overdue;
    private boolean completed;

    // Labels
    private List<LabelResponse> labels;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TaskResponse from(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .taskKey(task.getTaskKey())
                .title(task.getTitle())
                .description(task.getDescription())
                .priority(task.getPriority())
                .position(task.getPosition())
                .projectId(task.getProject().getId())
                .projectName(task.getProject().getName())
                .projectKey(task.getProject().getKey())
                .boardId(task.getBoard().getId())
                .boardName(task.getBoard().getName())
                .columnId(task.getColumn().getId())
                .columnName(task.getColumn().getName())
                .assigneeId(task.getAssignee() != null ? task.getAssignee().getId() : null)
                .assigneeName(task.getAssignee() != null ? task.getAssignee().getFullName() : null)
                .assigneeAvatar(task.getAssignee() != null ? task.getAssignee().getAvatarUrl() : null)
                .reporterId(task.getReporter().getId())
                .reporterName(task.getReporter().getFullName())
                .reporterAvatar(task.getReporter().getAvatarUrl())
                .startDate(task.getStartDate())
                .dueDate(task.getDueDate())
                .completedAt(task.getCompletedAt())
                .estimatedHours(task.getEstimatedHours())
                .actualHours(task.getActualHours())
                .commentCount(task.getComments().size())
                .attachmentCount(task.getAttachments().size())
                .overdue(task.isOverdue())
                .completed(task.isCompleted())
                .labels(task.getTaskLabels().stream()
                        .map(tl -> LabelResponse.from(tl.getLabel()))
                        .collect(Collectors.toList()))
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}
