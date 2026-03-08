package com.taskoryx.backend.dto.request.task;

import com.taskoryx.backend.entity.Task;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class CreateTaskRequest {

    @NotBlank(message = "Tiêu đề task không được để trống")
    @Size(max = 500, message = "Tiêu đề task không được quá 500 ký tự")
    private String title;

    private String description;

    private UUID boardId;

    private UUID columnId;

    private Task.TaskPriority priority = Task.TaskPriority.MEDIUM;

    private UUID assigneeId;

    private LocalDate startDate;
    private LocalDate dueDate;

    private BigDecimal estimatedHours;

    private List<UUID> labelIds;
}
