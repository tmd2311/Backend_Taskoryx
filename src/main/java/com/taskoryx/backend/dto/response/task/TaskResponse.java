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
    private Task.TaskStatus status;
    private BigDecimal position;

    // Project info
    private UUID projectId;
    private String projectName;
    private String projectKey;

    // Board info
    private UUID boardId;
    private String boardName;

    // Sprint info
    private UUID sprintId;
    private String sprintName;

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

    // Category info
    private UUID categoryId;
    private String categoryName;

    // Watchers
    private long watcherCount;

    // Hierarchy info
    private int depth;
    private UUID parentTaskId;
    private String parentTaskKey;
    private String parentTaskTitle;

    // Sub tasks
    private List<SubTaskInfo> subTasks;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubTaskInfo {
        private UUID id;
        private String taskKey;
        private String title;
        private Task.TaskStatus status;
        private Task.TaskPriority priority;
        private UUID assigneeId;
        private String assigneeName;
    }

    public static TaskResponse from(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .taskKey(task.getTaskKey())
                .title(task.getTitle())
                .description(task.getDescription())
                .priority(task.getPriority())
                .status(task.getStatus())
                .position(task.getPosition())
                .projectId(task.getProject().getId())
                .projectName(task.getProject().getName())
                .projectKey(task.getProject().getKey())
                .boardId(task.getBoard() != null ? task.getBoard().getId() : null)
                .boardName(task.getBoard() != null ? task.getBoard().getName() : null)
                .sprintId(task.getSprint() != null ? task.getSprint().getId() : null)
                .sprintName(task.getSprint() != null ? task.getSprint().getName() : null)
                .columnId(task.getColumn() != null ? task.getColumn().getId() : null)
                .columnName(task.getColumn() != null ? task.getColumn().getName() : null)
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
                .categoryId(task.getCategory() != null ? task.getCategory().getId() : null)
                .categoryName(task.getCategory() != null ? task.getCategory().getName() : null)
                .watcherCount(task.getWatchers().size())
                .depth(task.getDepth())
                .parentTaskId(task.getParentTask() != null ? task.getParentTask().getId() : null)
                .parentTaskKey(task.getParentTask() != null ? task.getParentTask().getTaskKey() : null)
                .parentTaskTitle(task.getParentTask() != null ? task.getParentTask().getTitle() : null)
                .subTasks(task.getSubTasks().stream()
                        .map(sub -> SubTaskInfo.builder()
                                .id(sub.getId())
                                .taskKey(sub.getTaskKey())
                                .title(sub.getTitle())
                                .status(sub.getStatus())
                                .priority(sub.getPriority())
                                .assigneeId(sub.getAssignee() != null ? sub.getAssignee().getId() : null)
                                .assigneeName(sub.getAssignee() != null ? sub.getAssignee().getFullName() : null)
                                .build())
                        .collect(Collectors.toList()))
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}
