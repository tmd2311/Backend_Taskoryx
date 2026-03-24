package com.taskoryx.backend.dto.response.timetracking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDetailTimeStatsResponse {

    private UUID projectId;
    private String projectName;
    private String projectKey;
    private LocalDate startDate;
    private LocalDate endDate;

    private BigDecimal totalHours;
    private String formattedTotalHours;
    private int totalEntries;

    private List<MemberTimeStatsResponse> byMember;
    private List<TaskTimeStatsResponse> byTask;
    private List<DailyTimeStatsResponse> byDay;
}
