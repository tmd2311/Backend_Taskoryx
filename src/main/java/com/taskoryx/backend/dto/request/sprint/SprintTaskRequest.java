package com.taskoryx.backend.dto.request.sprint;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SprintTaskRequest {

    @NotNull(message = "taskId không được để trống")
    private UUID taskId;
}
