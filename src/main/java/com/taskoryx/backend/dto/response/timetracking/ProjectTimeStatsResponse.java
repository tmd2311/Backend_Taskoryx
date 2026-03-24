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
public class ProjectTimeStatsResponse {

    private UUID projectId;
    private String projectName;
    private String projectKey;
    private BigDecimal totalHours;
    private String formattedHours;
    private int entryCount;
    private int taskCount;
}
