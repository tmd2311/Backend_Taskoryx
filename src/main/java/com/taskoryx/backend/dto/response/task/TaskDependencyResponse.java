package com.taskoryx.backend.dto.response.task;

import com.taskoryx.backend.entity.TaskDependency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDependencyResponse {

    private UUID id;

    // Task chính (task này phụ thuộc vào dependsOnTask)
    private UUID taskId;
    private String taskKey;
    private String taskTitle;

    // Task bị phụ thuộc
    private UUID dependsOnTaskId;
    private String dependsOnTaskKey;
    private String dependsOnTaskTitle;

    private TaskDependency.DependencyType type;
    private LocalDateTime createdAt;

    public static TaskDependencyResponse from(TaskDependency dep) {
        return TaskDependencyResponse.builder()
                .id(dep.getId())
                .taskId(dep.getTask().getId())
                .taskKey(dep.getTask().getTaskKey())
                .taskTitle(dep.getTask().getTitle())
                .dependsOnTaskId(dep.getDependsOnTask().getId())
                .dependsOnTaskKey(dep.getDependsOnTask().getTaskKey())
                .dependsOnTaskTitle(dep.getDependsOnTask().getTitle())
                .type(dep.getType())
                .createdAt(dep.getCreatedAt())
                .build();
    }
}
