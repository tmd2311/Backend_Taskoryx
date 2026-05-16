package com.taskoryx.backend.dto.response.stats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectStatsResponse {

    private UUID projectId;
    private String projectName;

    /** Tổng quan task (count theo status + priority) */
    private TaskOverviewStats taskOverview;

    /** Biểu đồ hoàn thành theo ngày (30 ngày qua) */
    private List<DailyCompletionStats> completionTrend;

    /** Phân bổ task cho từng thành viên */
    private List<MemberTaskStats> memberStats;

    /** Thống kê sprint đang ACTIVE (null nếu không có) */
    private ActiveSprintStats activeSprint;

    /** Danh sách task quá hạn */
    private List<TaskAlertItem> overdueTasks;

    /** Danh sách task sắp đến hạn trong 7 ngày tới */
    private List<TaskAlertItem> upcomingTasks;
}
