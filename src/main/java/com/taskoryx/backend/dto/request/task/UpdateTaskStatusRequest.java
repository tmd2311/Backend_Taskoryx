package com.taskoryx.backend.dto.request.task;

import com.taskoryx.backend.entity.Task;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateTaskStatusRequest {

    @NotNull(message = "Trạng thái không được để trống")
    private Task.TaskStatus status;
}
