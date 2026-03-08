package com.taskoryx.backend.dto.request.task;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request để di chuyển task trong Kanban board (drag & drop)
 */
@Data
public class MoveTaskRequest {

    // null = chuyển task về Backlog
    private UUID targetColumnId;

    @NotNull(message = "Vị trí mới không được để trống")
    private BigDecimal newPosition;
}
