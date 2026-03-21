package com.taskoryx.backend.service;

import com.taskoryx.backend.dto.response.activity.ActivityLogResponse;
import com.taskoryx.backend.dto.response.dashboard.ProjectDashboardResponse;
import com.taskoryx.backend.dto.response.dashboard.TaskStatsResponse;
import com.taskoryx.backend.dto.response.dashboard.UserDashboardResponse;
import com.taskoryx.backend.dto.response.notification.NotificationResponse;
import com.taskoryx.backend.dto.response.task.TaskSummaryResponse;
import com.taskoryx.backend.dto.response.timetracking.TimeTrackingResponse;
import com.taskoryx.backend.entity.Task;
import com.taskoryx.backend.repository.ActivityLogRepository;
import com.taskoryx.backend.repository.BoardRepository;
import com.taskoryx.backend.repository.NotificationRepository;
import com.taskoryx.backend.repository.ProjectMemberRepository;
import com.taskoryx.backend.repository.TaskRepository;
import com.taskoryx.backend.repository.TimeTrackingRepository;
import com.taskoryx.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TaskRepository taskRepository;
    private final ProjectService projectService;
    private final TimeTrackingRepository timeTrackingRepository;
    private final ActivityLogRepository activityLogRepository;
    private final NotificationRepository notificationRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final BoardRepository boardRepository;

    @Transactional(readOnly = true)
    public ProjectDashboardResponse getProjectDashboard(UUID projectId, UserPrincipal principal) {
        var project = projectService.findProjectWithAccess(projectId, principal.getId());

        // Đếm tasks theo status
        long totalTasks = taskRepository.countByProjectId(projectId);
        long todoCount = taskRepository.countByProjectIdAndStatus(projectId, Task.TaskStatus.TODO);
        long inProgressCount = taskRepository.countByProjectIdAndStatus(projectId, Task.TaskStatus.IN_PROGRESS);
        long inReviewCount = taskRepository.countByProjectIdAndStatus(projectId, Task.TaskStatus.IN_REVIEW);
        long resolvedCount = taskRepository.countByProjectIdAndStatus(projectId, Task.TaskStatus.RESOLVED);
        long doneCount = taskRepository.countByProjectIdAndStatus(projectId, Task.TaskStatus.DONE);
        long cancelledCount = taskRepository.countByProjectIdAndStatus(projectId, Task.TaskStatus.CANCELLED);

        // Đếm task quá hạn
        long overdueCount = taskRepository.findOverdueTasks(LocalDate.now()).stream()
                .filter(t -> t.getProject().getId().equals(projectId))
                .count();

        // Đếm task hoàn thành tuần này
        LocalDateTime weekStart = LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime weekEnd = LocalDate.now().with(DayOfWeek.SUNDAY).atTime(LocalTime.MAX);
        // Tính completed this week từ tasks done trong tuần
        long completedThisWeek = taskRepository.findByProjectId(projectId, PageRequest.of(0, Integer.MAX_VALUE))
                .getContent()
                .stream()
                .filter(t -> t.getCompletedAt() != null
                        && !t.getCompletedAt().isBefore(weekStart)
                        && !t.getCompletedAt().isAfter(weekEnd))
                .count();

        TaskStatsResponse taskStats = TaskStatsResponse.builder()
                .total(totalTasks)
                .todo(todoCount)
                .inProgress(inProgressCount)
                .inReview(inReviewCount)
                .resolved(resolvedCount)
                .done(doneCount)
                .cancelled(cancelledCount)
                .overdue(overdueCount)
                .completedThisWeek(completedThisWeek)
                .build();

        // Thành viên và boards
        long totalMembers = projectMemberRepository.findByProjectId(projectId).size();
        long totalBoards = boardRepository.findByProjectIdOrderByPositionAsc(projectId).size();

        // Tổng giờ đã log
        BigDecimal totalHours = timeTrackingRepository.sumHoursByProjectId(projectId)
                .orElse(BigDecimal.ZERO);

        // 5 time entries gần nhất
        List<TimeTrackingResponse> recentTimeEntries = timeTrackingRepository
                .findByProjectIdOrderByCreatedAtDesc(projectId, PageRequest.of(0, 5))
                .getContent()
                .stream()
                .map(TimeTrackingResponse::from)
                .collect(Collectors.toList());

        // 10 activity logs gần nhất
        List<ActivityLogResponse> recentActivity = activityLogRepository
                .findByProjectIdOrderByCreatedAtDesc(projectId, PageRequest.of(0, 10))
                .stream()
                .map(ActivityLogResponse::from)
                .collect(Collectors.toList());

        return ProjectDashboardResponse.builder()
                .projectId(project.getId())
                .projectName(project.getName())
                .taskStats(taskStats)
                .totalMembers(totalMembers)
                .totalBoards(totalBoards)
                .totalHoursLogged(totalHours)
                .recentTimeEntries(recentTimeEntries)
                .recentActivity(recentActivity)
                .build();
    }

    @Transactional(readOnly = true)
    public UserDashboardResponse getUserDashboard(UserPrincipal principal) {
        UUID userId = principal.getId();
        LocalDate today = LocalDate.now();

        // Tasks được giao cho tôi (active)
        long totalAssigned = taskRepository.countActiveByAssigneeId(userId);

        // Tasks quá hạn
        List<Task> overdueTasks = taskRepository.findOverdueTasksByAssigneeId(userId, today);
        int overdueCount = overdueTasks.size();

        // Tasks due today
        long dueTodayCount = taskRepository.countDueTodayByAssigneeId(userId, today);

        // Tasks hoàn thành tuần này
        LocalDateTime weekStart = today.with(DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime weekEnd = today.with(DayOfWeek.SUNDAY).atTime(LocalTime.MAX);
        long completedThisWeek = taskRepository.countCompletedByAssigneeIdBetween(userId, weekStart, weekEnd);

        // Số giờ đã log tuần này
        LocalDate weekStartDate = today.with(DayOfWeek.MONDAY);
        LocalDate weekEndDate = today.with(DayOfWeek.SUNDAY);
        BigDecimal hoursLoggedThisWeek = timeTrackingRepository
                .sumHoursByUserIdAndWorkDateBetween(userId, weekStartDate, weekEndDate)
                .orElse(BigDecimal.ZERO);

        // Top 10 tasks được giao, sort by dueDate
        List<TaskSummaryResponse> myTasks = taskRepository
                .findByAssigneeIdOrderByDueDateAsc(userId)
                .stream()
                .filter(t -> t.getStatus() != Task.TaskStatus.DONE
                        && t.getStatus() != Task.TaskStatus.CANCELLED)
                .limit(10)
                .map(TaskSummaryResponse::from)
                .collect(Collectors.toList());

        // 5 notifications unread gần nhất
        List<NotificationResponse> recentNotifications = notificationRepository
                .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId, PageRequest.of(0, 5))
                .stream()
                .map(NotificationResponse::from)
                .collect(Collectors.toList());

        return UserDashboardResponse.builder()
                .totalAssignedTasks((int) totalAssigned)
                .overdueTasksCount(overdueCount)
                .dueTodayCount((int) dueTodayCount)
                .completedThisWeekCount((int) completedThisWeek)
                .hoursLoggedThisWeek(hoursLoggedThisWeek)
                .myTasks(myTasks)
                .recentNotifications(recentNotifications)
                .build();
    }
}
