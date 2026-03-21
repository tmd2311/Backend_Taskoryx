package com.taskoryx.backend.dto.response.dashboard;

import com.taskoryx.backend.dto.response.notification.NotificationResponse;
import com.taskoryx.backend.dto.response.task.TaskSummaryResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDashboardResponse {

    private int totalAssignedTasks;
    private int overdueTasksCount;
    private int dueTodayCount;
    private int completedThisWeekCount;
    private BigDecimal hoursLoggedThisWeek;
    private List<TaskSummaryResponse> myTasks;
    private List<NotificationResponse> recentNotifications;
}
