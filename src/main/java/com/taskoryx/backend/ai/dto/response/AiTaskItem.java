package com.taskoryx.backend.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.taskoryx.backend.entity.Task;
import lombok.Data;

import java.util.List;

@Data
public class AiTaskItem {

    private String title;

    private String description;

    /** LOW / MEDIUM / HIGH / URGENT */
    private Task.TaskPriority priority;

    @JsonProperty("duration_days")
    private Integer durationDays;

    @JsonProperty("start_offset_days")
    private Integer startOffsetDays;

    /** Danh sách sub-task (tối đa 1 cấp con theo giới hạn 3 cấp của hệ thống) */
    @JsonProperty("sub_tasks")
    private List<AiTaskItem> subTasks;
}
