package com.taskoryx.backend.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class AiSprintItem {

    private String name;

    private String goal;

    /** Thứ tự sprint trong dự án (bắt đầu từ 1). */
    @JsonProperty("sprint_number")
    private Integer sprintNumber;

    /** Thời lượng sprint (ngày). */
    @JsonProperty("duration_days")
    private Integer durationDays;

    /** Số ngày kể từ ngày bắt đầu dự án đến khi sprint này bắt đầu. */
    @JsonProperty("start_offset_days")
    private Integer startOffsetDays;

    /** Danh sách task thuộc sprint này. */
    private List<AiTaskItem> tasks;
}
