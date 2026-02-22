package com.taskoryx.backend.dto.request.task;

import com.taskoryx.backend.entity.Task;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class UpdateTaskRequest {

    @Size(max = 500, message = "Tiêu đề task không được quá 500 ký tự")
    private String title;

    private String description;
    private Task.TaskPriority priority;
    private UUID assigneeId;
    private LocalDate startDate;
    private LocalDate dueDate;
    private BigDecimal estimatedHours;
    private BigDecimal actualHours;
    private List<UUID> labelIds;
}
