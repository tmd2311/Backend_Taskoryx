package com.taskoryx.backend.dto.response.timetracking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskTimeStatsResponse {

    private UUID taskId;
    private String taskKey;
    private String taskTitle;
    private String taskStatus;
    private BigDecimal estimatedHours;
    private BigDecimal loggedHours;
    private String formattedLoggedHours;
    private int entryCount;
    /** Phần trăm so với estimated (null nếu không có estimated) */
    private Double progressPercent;
}
