package com.taskoryx.backend.ai.dto.request;

import com.taskoryx.backend.ai.dto.response.AiProjectPlan;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AiConfirmPlanRequest {

    private UUID targetProjectId;

    @NotNull(message = "Kế hoạch AI không được để trống")
    @Valid
    private AiProjectPlan plan;
}
