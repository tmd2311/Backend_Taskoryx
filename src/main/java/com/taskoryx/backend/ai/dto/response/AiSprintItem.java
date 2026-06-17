package com.taskoryx.backend.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class AiSprintItem {

    private String name;

    private String goal;

    @JsonProperty("sprint_number")
    private Integer sprintNumber;

    @JsonProperty("duration_days")
    private Integer durationDays;

    @JsonProperty("start_offset_days")
    private Integer startOffsetDays;

    @JsonProperty("start_date")
    private LocalDate startDate;

    @JsonProperty("end_date")
    private LocalDate endDate;

    private List<AiTaskItem> tasks;
}
