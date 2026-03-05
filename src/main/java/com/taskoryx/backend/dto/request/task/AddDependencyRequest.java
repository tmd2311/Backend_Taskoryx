package com.taskoryx.backend.dto.request.task;

import com.taskoryx.backend.entity.TaskDependency;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AddDependencyRequest {

    @NotNull(message = "Task phụ thuộc không được để trống")
    private UUID dependsOnTaskId;

    private TaskDependency.DependencyType type = TaskDependency.DependencyType.BLOCKS;
}
