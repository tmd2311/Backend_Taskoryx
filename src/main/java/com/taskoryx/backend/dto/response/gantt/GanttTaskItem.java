package com.taskoryx.backend.dto.response.gantt;

import com.taskoryx.backend.entity.Task;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GanttTaskItem {

    private UUID id;
    private String taskKey;
    private String title;
    private Task.TaskStatus status;
    private Task.TaskPriority priority;
    private LocalDate startDate;
    private LocalDate dueDate;
    private UUID assigneeId;
    private String assigneeName;
    private String assigneeAvatar;
    private int percentComplete;
    private List<UUID> dependsOnTaskIds;
    private UUID categoryId;
    private String categoryName;
    private boolean overdue;

    public static GanttTaskItem from(Task task) {
        int percent = switch (task.getStatus()) {
            case TODO -> 0;
            case IN_PROGRESS -> 50;
            case IN_REVIEW -> 75;
            case RESOLVED, DONE -> 100;
            case CANCELLED -> 0;
        };

        return GanttTaskItem.builder()
                .id(task.getId())
                .taskKey(task.getTaskKey())
                .title(task.getTitle())
                .status(task.getStatus())
                .priority(task.getPriority())
                .startDate(task.getStartDate())
                .dueDate(task.getDueDate())
                .assigneeId(task.getAssignee() != null ? task.getAssignee().getId() : null)
                .assigneeName(task.getAssignee() != null ? task.getAssignee().getFullName() : null)
                .assigneeAvatar(task.getAssignee() != null ? task.getAssignee().getAvatarUrl() : null)
                .percentComplete(percent)
                .dependsOnTaskIds(task.getDependencies().stream()
                        .map(dep -> dep.getDependsOnTask().getId())
                        .collect(Collectors.toList()))
                .categoryId(task.getCategory() != null ? task.getCategory().getId() : null)
                .categoryName(task.getCategory() != null ? task.getCategory().getName() : null)
                .overdue(task.isOverdue())
                .build();
    }
}
