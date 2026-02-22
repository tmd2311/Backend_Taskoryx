package com.taskoryx.backend.dto.response.task;

import com.taskoryx.backend.entity.Task;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

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
    private BigDecimal position;
    private UUID columnId;
    private UUID assigneeId;
    private String assigneeName;
    private String assigneeAvatar;
    private LocalDate dueDate;
    private boolean overdue;
    private int commentCount;
    private int attachmentCount;

    public static TaskSummaryResponse from(Task task) {
        return TaskSummaryResponse.builder()
                .id(task.getId())
                .taskKey(task.getTaskKey())
                .title(task.getTitle())
                .priority(task.getPriority())
                .position(task.getPosition())
                .columnId(task.getColumn() != null ? task.getColumn().getId() : null)
                .assigneeId(task.getAssignee() != null ? task.getAssignee().getId() : null)
                .assigneeName(task.getAssignee() != null ? task.getAssignee().getFullName() : null)
                .assigneeAvatar(task.getAssignee() != null ? task.getAssignee().getAvatarUrl() : null)
                .dueDate(task.getDueDate())
                .overdue(task.isOverdue())
                .commentCount(task.getComments().size())
                .attachmentCount(task.getAttachments().size())
                .build();
    }
}
