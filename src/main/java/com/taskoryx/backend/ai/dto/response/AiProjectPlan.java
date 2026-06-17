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

    @JsonProperty("project_key")
    private String projectKey;

    @JsonProperty("total_duration_days")
    private Integer totalDurationDays;

    private List<AiSprintItem> sprints;
}
