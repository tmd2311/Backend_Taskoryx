package com.taskoryx.backend.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class AiProjectPlan {

    @NotBlank
    @JsonProperty("project_name")
    private String projectName;

    @JsonProperty("project_description")
    private String projectDescription;

    /** Mã dự án đề xuất (2-10 ký tự hoa). AI sinh, backend sẽ validate/điều chỉnh nếu trùng. */
    @JsonProperty("project_key")
    private String projectKey;

    /** Tổng thời gian dự kiến (ngày). */
    @JsonProperty("total_duration_days")
    private Integer totalDurationDays;

    private List<AiTaskItem> tasks;
}
