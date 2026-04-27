package com.taskoryx.backend.dto.response.task;

import com.taskoryx.backend.entity.Task;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Response rút gọn cho task - dùng trong Kanban board
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskSummaryResponse {

    private UUID id;
    private String taskKey;
    private String title;
    private Task.TaskPriority priority;
    private Task.TaskStatus status;
    private BigDecimal position;
    private UUID sprintId;
    private String sprintName;
    private UUID columnId;
    private UUID assigneeId;
    private String assigneeName;
    private String assigneeAvatar;
    private LocalDate dueDate;
    private boolean overdue;
    private int commentCount;
    private int attachmentCount;

    private UUID parentTaskId;
    private List<TaskSummaryResponse> subTasks;

    public static TaskSummaryResponse from(Task task) {
        return TaskSummaryResponse.builder()
                .id(task.getId())
                .taskKey(task.getTaskKey())
                .title(task.getTitle())
                .priority(task.getPriority())
                .status(task.getStatus())
                .position(task.getPosition())
                .sprintId(task.getSprint() != null ? task.getSprint().getId() : null)
                .sprintName(task.getSprint() != null ? task.getSprint().getName() : null)
                .columnId(task.getColumn() != null ? task.getColumn().getId() : null)
                .assigneeId(task.getAssignee() != null ? task.getAssignee().getId() : null)
                .assigneeName(task.getAssignee() != null ? task.getAssignee().getFullName() : null)
                .assigneeAvatar(task.getAssignee() != null ? task.getAssignee().getAvatarUrl() : null)
                .dueDate(task.getDueDate())
                .overdue(task.isOverdue())
                .commentCount(task.getComments().size())
                .attachmentCount(task.getAttachments().size())
                .parentTaskId(task.getParentTask() != null ? task.getParentTask().getId() : null)
                .subTasks(task.getSubTasks() != null
                        ? task.getSubTasks().stream()
                                .map(TaskSummaryResponse::from)
                                .collect(Collectors.toList())
                        : List.of())
                .build();
    }
}
