package com.taskoryx.backend.dto.response.version;

import com.taskoryx.backend.entity.Task;
import com.taskoryx.backend.entity.Version;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RoadmapVersionItem {

    private UUID id;
    private String name;
    private String description;
    private Version.VersionStatus status;
    private LocalDate dueDate;
    private LocalDate releaseDate;
    private int totalTasks;
    private int completedTasks;
    private int completionPercent;
    private List<TaskBrief> tasks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TaskBrief {
        private UUID id;
        private String taskKey;
        private String title;
        private Task.TaskStatus status;
        private Task.TaskPriority priority;
        private LocalDate dueDate;
        private UUID assigneeId;
        private String assigneeName;
        private String assigneeAvatar;
        private boolean overdue;
    }

    public static RoadmapVersionItem from(Version v) {
        List<TaskBrief> taskBriefs = v.getTasks().stream()
                .map(t -> TaskBrief.builder()
                        .id(t.getId())
                        .taskKey(t.getTaskKey())
                        .title(t.getTitle())
                        .status(t.getStatus())
                        .priority(t.getPriority())
                        .dueDate(t.getDueDate())
                        .assigneeId(t.getAssignee() != null ? t.getAssignee().getId() : null)
                        .assigneeName(t.getAssignee() != null ? t.getAssignee().getFullName() : null)
                        .assigneeAvatar(t.getAssignee() != null ? t.getAssignee().getAvatarUrl() : null)
                        .overdue(t.isOverdue())
                        .build())
                .collect(Collectors.toList());

        return RoadmapVersionItem.builder()
                .id(v.getId())
                .name(v.getName())
                .description(v.getDescription())
                .status(v.getStatus())
                .dueDate(v.getDueDate())
                .releaseDate(v.getReleaseDate())
                .totalTasks(v.getTotalTasks())
                .completedTasks(v.getCompletedTasks())
                .completionPercent(v.getCompletionPercent())
                .tasks(taskBriefs)
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .build();
    }
}
