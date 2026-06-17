package com.taskoryx.backend.ai.dto.request;

import com.taskoryx.backend.ai.dto.response.AiProjectPlan;
import lombok.Data;

@Data
public class UpdateSessionPlanRequest {
    private AiProjectPlan plan;
}
