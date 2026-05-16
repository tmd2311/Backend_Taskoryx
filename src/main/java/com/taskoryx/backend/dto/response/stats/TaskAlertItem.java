package com.taskoryx.backend.dto.response.stats;

import com.taskoryx.backend.entity.Task;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskAlertItem {

    private UUID id;
    private String taskKey;
    private String title;
    private Task.TaskPriority priority;
    private Task.TaskStatus status;
    private LocalDate dueDate;

    /** Số ngày quá hạn (dương) hoặc số ngày còn lại (âm) tính từ hôm nay */
    private long daysFromNow;

    private UUID assigneeId;
    private String assigneeName;
    private String assigneeAvatarUrl;

    public static TaskAlertItem from(Task task, LocalDate today) {
        long days = today.toEpochDay() - task.getDueDate().toEpochDay();
        String assigneeName = task.getAssignee() != null ? task.getAssignee().getFullName() : null;
        String assigneeAvatar = task.getAssignee() != null ? task.getAssignee().getAvatarUrl() : null;
        UUID assigneeId = task.getAssignee() != null ? task.getAssignee().getId() : null;

        return TaskAlertItem.builder()
                .id(task.getId())
                .taskKey(task.getTaskKey())
                .title(task.getTitle())
                .priority(task.getPriority())
                .status(task.getStatus())
                .dueDate(task.getDueDate())
                .daysFromNow(days)
                .assigneeId(assigneeId)
                .assigneeName(assigneeName)
                .assigneeAvatarUrl(assigneeAvatar)
                .build();
    }
}
