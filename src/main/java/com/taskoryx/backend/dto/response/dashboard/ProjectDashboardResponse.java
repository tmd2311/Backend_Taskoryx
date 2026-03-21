package com.taskoryx.backend.dto.response.dashboard;

import com.taskoryx.backend.dto.response.activity.ActivityLogResponse;
import com.taskoryx.backend.dto.response.timetracking.TimeTrackingResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDashboardResponse {

    private UUID projectId;
    private String projectName;
    private TaskStatsResponse taskStats;
    private long totalMembers;
    private long totalBoards;
    private BigDecimal totalHoursLogged;
    private List<TimeTrackingResponse> recentTimeEntries;
    private List<ActivityLogResponse> recentActivity;
}
