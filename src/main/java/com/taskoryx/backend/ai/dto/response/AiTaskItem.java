package com.taskoryx.backend.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.taskoryx.backend.entity.Task;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class AiTaskItem {

    private String title;

    private String description;

    private Task.TaskPriority priority;

    private Task.TaskStatus status;

    @JsonProperty("duration_days")
    private Integer durationDays;

    @JsonProperty("start_offset_days")
    private Integer startOffsetDays;

    @JsonProperty("start_date")
    private LocalDate startDate;

    @JsonProperty("due_date")
    private LocalDate dueDate;

    @JsonProperty("estimated_hours")
    private BigDecimal estimatedHours;

    @JsonProperty("assignee_id")
    private UUID assigneeId;

    @JsonProperty("label_ids")
    private List<UUID> labelIds;

    @JsonProperty("category_id")
    private UUID categoryId;

    @JsonProperty("sub_tasks")
    private List<AiTaskItem> subTasks;
}
