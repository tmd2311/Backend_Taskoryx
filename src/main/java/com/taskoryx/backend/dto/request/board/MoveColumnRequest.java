package com.taskoryx.backend.dto.request.board;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MoveColumnRequest {

    @NotNull(message = "Vị trí mới không được để trống")
    @Min(value = 0, message = "Vị trí phải không âm")
    private Integer newPosition;
}
