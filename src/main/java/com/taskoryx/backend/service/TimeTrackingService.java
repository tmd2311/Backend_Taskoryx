package com.taskoryx.backend.service;

import com.taskoryx.backend.dto.request.timetracking.CreateTimeEntryRequest;
import com.taskoryx.backend.dto.request.timetracking.UpdateTimeEntryRequest;
import com.taskoryx.backend.dto.response.timetracking.*;
import com.taskoryx.backend.entity.ProjectPermission;
import com.taskoryx.backend.entity.Task;
import com.taskoryx.backend.entity.TimeTracking;
import com.taskoryx.backend.entity.User;
import com.taskoryx.backend.exception.ForbiddenException;
import com.taskoryx.backend.exception.ResourceNotFoundException;
import com.taskoryx.backend.repository.ProjectRepository;
import com.taskoryx.backend.repository.TaskRepository;
import com.taskoryx.backend.repository.TimeTrackingRepository;
import com.taskoryx.backend.repository.UserRepository;
import com.taskoryx.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimeTrackingService {

    private final TimeTrackingRepository timeTrackingRepository;
    private final TaskRepository taskRepository;
    private final ProjectAuthorizationService projectAuthorizationService;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Transactional
    public TimeTrackingResponse logTime(CreateTimeEntryRequest request, UserPrincipal principal) {
        Task task = taskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", request.getTaskId()));

        projectAuthorizationService.requirePermission(task.getProject().getId(), principal.getId(),
                ProjectPermission.TIME_TRACKING_MANAGE);

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        LocalDate workDate = request.getWorkDate() != null ? request.getWorkDate() : LocalDate.now();

        TimeTracking entry = TimeTracking.builder()
                .task(task)
                .user(user)
                .hours(request.getHours())
                .description(request.getDescription())
                .workDate(workDate)
                .build();

        entry = timeTrackingRepository.save(entry);

        // Cập nhật actualHours của task
        updateTaskActualHours(task);

        return TimeTrackingResponse.from(entry);
    }

    @Transactional(readOnly = true)
    public List<TimeTrackingResponse> getTimeEntriesForTask(UUID taskId, UserPrincipal principal) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        projectAuthorizationService.requirePermission(task.getProject().getId(), principal.getId(),
                ProjectPermission.TIME_TRACKING_VIEW);

        return timeTrackingRepository.findByTaskId(taskId)
                .stream()
                .map(TimeTrackingResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<TimeTrackingResponse> getMyTimeEntries(UserPrincipal principal, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("workDate").descending());
        return timeTrackingRepository.findByUserIdOrderByWorkDateDesc(principal.getId(), pageable)
                .map(TimeTrackingResponse::from);
    }

    @Transactional(readOnly = true)
    public List<TimeTrackingResponse> getTimeEntriesByDateRange(UUID userId, LocalDate start, LocalDate end,
                                                                 UserPrincipal principal) {
        UUID targetUserId = userId != null ? userId : principal.getId();
        if (!targetUserId.equals(principal.getId())) {
            throw new ForbiddenException("Bạn chỉ có thể xem dữ liệu time tracking của chính mình");
        }

        return timeTrackingRepository.findByUserIdAndWorkDateBetween(targetUserId, start, end)
                .stream()
                .map(TimeTrackingResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public TimeTrackingResponse updateTimeEntry(UUID entryId, UpdateTimeEntryRequest request,
                                                 UserPrincipal principal) {
        TimeTracking entry = timeTrackingRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("TimeTracking", "id", entryId));
        projectAuthorizationService.requirePermission(entry.getTask().getProject().getId(), principal.getId(),
                ProjectPermission.TIME_TRACKING_MANAGE);

        // Chỉ người tạo entry mới được sửa
        if (!entry.getUser().getId().equals(principal.getId())) {
            throw new ForbiddenException("Bạn không có quyền chỉnh sửa mục ghi thời gian này");
        }

        if (request.getHours() != null) {
            entry.setHours(request.getHours());
        }
        if (request.getDescription() != null) {
            entry.setDescription(request.getDescription());
        }
        if (request.getWorkDate() != null) {
            entry.setWorkDate(request.getWorkDate());
        }

        entry = timeTrackingRepository.save(entry);

        // Cập nhật actualHours của task
        updateTaskActualHours(entry.getTask());

        return TimeTrackingResponse.from(entry);
    }

    @Transactional
    public void deleteTimeEntry(UUID entryId, UserPrincipal principal) {
        TimeTracking entry = timeTrackingRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("TimeTracking", "id", entryId));
        projectAuthorizationService.requirePermission(entry.getTask().getProject().getId(), principal.getId(),
                ProjectPermission.TIME_TRACKING_MANAGE);

        // Chỉ người tạo entry mới được xóa
        if (!entry.getUser().getId().equals(principal.getId())) {
            throw new ForbiddenException("Bạn không có quyền xóa mục ghi thời gian này");
        }

        Task task = entry.getTask();
        timeTrackingRepository.delete(entry);

        // Cập nhật actualHours của task sau khi xóa
        updateTaskActualHours(task);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTaskTotalHours(UUID taskId, UserPrincipal principal) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        projectAuthorizationService.requirePermission(task.getProject().getId(), principal.getId(),
                ProjectPermission.TIME_TRACKING_VIEW);

        return timeTrackingRepository.sumHoursByTaskId(taskId)
                .orElse(BigDecimal.ZERO);
    }

    // ========== STATISTICS ==========

    /**
     * Thống kê theo ngày trong khoảng thời gian cho user hiện tại.
     * Trả về danh sách từng ngày với tổng giờ và danh sách entries.
     */
    @Transactional(readOnly = true)
    public List<DailyTimeStatsResponse> getDailyStats(LocalDate start, LocalDate end,
                                                       UserPrincipal principal) {
        List<TimeTracking> entries = timeTrackingRepository
                .findByUserAndDateRangeWithTask(principal.getId(), start, end);

        // Group by ngày
        Map<LocalDate, List<TimeTracking>> byDate = entries.stream()
                .collect(Collectors.groupingBy(TimeTracking::getWorkDate, TreeMap::new, Collectors.toList()));

        // Lấp đầy các ngày không có entry
        List<DailyTimeStatsResponse> result = new ArrayList<>();
        LocalDate current = start;
        while (!current.isAfter(end)) {
            List<TimeTracking> dayEntries = byDate.getOrDefault(current, Collections.emptyList());
            BigDecimal total = dayEntries.stream()
                    .map(TimeTracking::getHours)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            result.add(DailyTimeStatsResponse.builder()
                    .date(current)
                    .dayOfWeek(current.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("vi")))
                    .totalHours(total)
                    .formattedHours(formatHours(total))
                    .entryCount(dayEntries.size())
                    .entries(dayEntries.stream().map(TimeTrackingResponse::from).collect(Collectors.toList()))
                    .build());
            current = current.plusDays(1);
        }
        return result;
    }

    /**
     * Thống kê theo tuần trong khoảng thời gian cho user hiện tại.
     */
    @Transactional(readOnly = true)
    public List<WeeklyTimeStatsResponse> getWeeklyStats(LocalDate start, LocalDate end,
                                                         UserPrincipal principal) {
        // Căn start về đầu tuần (Monday)
        LocalDate weekStart = start.with(DayOfWeek.MONDAY);
        if (weekStart.isAfter(start)) weekStart = weekStart.minusWeeks(1);

        List<TimeTracking> entries = timeTrackingRepository
                .findByUserAndDateRangeWithTask(principal.getId(), start, end);

        Map<LocalDate, List<TimeTracking>> byDate = entries.stream()
                .collect(Collectors.groupingBy(TimeTracking::getWorkDate));

        List<WeeklyTimeStatsResponse> result = new ArrayList<>();
        LocalDate ws = weekStart;
        while (!ws.isAfter(end)) {
            LocalDate we = ws.plusDays(6);
            LocalDate effectiveStart = ws.isBefore(start) ? start : ws;
            LocalDate effectiveEnd = we.isAfter(end) ? end : we;

            // Lấy entries trong tuần này
            List<DailyTimeStatsResponse> days = new ArrayList<>();
            BigDecimal weekTotal = BigDecimal.ZERO;
            int weekEntryCount = 0;
            LocalDate d = effectiveStart;
            while (!d.isAfter(effectiveEnd)) {
                List<TimeTracking> dayList = byDate.getOrDefault(d, Collections.emptyList());
                BigDecimal dayTotal = dayList.stream()
                        .map(TimeTracking::getHours).reduce(BigDecimal.ZERO, BigDecimal::add);
                days.add(DailyTimeStatsResponse.builder()
                        .date(d)
                        .dayOfWeek(d.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("vi")))
                        .totalHours(dayTotal)
                        .formattedHours(formatHours(dayTotal))
                        .entryCount(dayList.size())
                        .entries(dayList.stream().map(TimeTrackingResponse::from).collect(Collectors.toList()))
                        .build());
                weekTotal = weekTotal.add(dayTotal);
                weekEntryCount += dayList.size();
                d = d.plusDays(1);
            }

            result.add(WeeklyTimeStatsResponse.builder()
                    .year(ws.getYear())
                    .weekOfYear(ws.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR))
                    .weekStart(ws)
                    .weekEnd(we)
                    .weekLabel("Tuần " + ws.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
                            + " (" + ws + " - " + we + ")")
                    .totalHours(weekTotal)
                    .formattedHours(formatHours(weekTotal))
                    .entryCount(weekEntryCount)
                    .days(days)
                    .build());
            ws = ws.plusWeeks(1);
        }
        return result;
    }

    /**
     * Thống kê theo tháng cho user trong một năm cụ thể.
     */
    @Transactional(readOnly = true)
    public List<MonthlyTimeStatsResponse> getMonthlyStats(int year, UserPrincipal principal) {
        List<Object[]> monthRows = timeTrackingRepository
                .sumHoursByMonthForUser(principal.getId(), year);

        // Build map month -> (totalHours, entryCount)
        Map<Integer, Object[]> monthMap = new HashMap<>();
        for (Object[] row : monthRows) {
            int month = ((Number) row[0]).intValue();
            monthMap.put(month, row);
        }

        List<MonthlyTimeStatsResponse> result = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            BigDecimal total = BigDecimal.ZERO;
            int entryCount = 0;
            if (monthMap.containsKey(m)) {
                Object[] row = monthMap.get(m);
                total = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
                entryCount = row[2] != null ? ((Number) row[2]).intValue() : 0;
            }

            // Lấy daily detail cho tháng này (chỉ load nếu có entries)
            List<DailyTimeStatsResponse> days = Collections.emptyList();
            if (entryCount > 0) {
                LocalDate monthStart = LocalDate.of(year, m, 1);
                LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
                days = getDailyStats(monthStart, monthEnd, principal);
            }

            int activeDays = (int) days.stream()
                    .filter(d -> d.getTotalHours().compareTo(BigDecimal.ZERO) > 0).count();

            result.add(MonthlyTimeStatsResponse.builder()
                    .year(year)
                    .month(m)
                    .monthName(Month.of(m).getDisplayName(TextStyle.FULL, Locale.forLanguageTag("vi")))
                    .totalHours(total)
                    .formattedHours(formatHours(total))
                    .entryCount(entryCount)
                    .activeDays(activeDays)
                    .days(entryCount > 0 ? days : null)
                    .build());
        }
        return result;
    }

    /**
     * Tổng hợp thống kê của user trong khoảng ngày:
     * tổng giờ, trung bình/ngày, breakdown theo project, breakdown theo ngày.
     */
    @Transactional(readOnly = true)
    public TimeStatsSummaryResponse getSummary(LocalDate start, LocalDate end,
                                                UserPrincipal principal) {
        UUID userId = principal.getId();

        // By-project rows
        List<Object[]> projectRows = timeTrackingRepository
                .sumHoursByProjectForUser(userId, start, end);
        List<ProjectTimeStatsResponse> byProject = projectRows.stream()
                .map(row -> {
                    BigDecimal hours = row[3] != null ? (BigDecimal) row[3] : BigDecimal.ZERO;
                    return ProjectTimeStatsResponse.builder()
                            .projectId((UUID) row[0])
                            .projectName((String) row[1])
                            .projectKey((String) row[2])
                            .totalHours(hours)
                            .formattedHours(formatHours(hours))
                            .entryCount(row[4] != null ? ((Number) row[4]).intValue() : 0)
                            .build();
                }).collect(Collectors.toList());

        // By-day
        List<DailyTimeStatsResponse> byDay = getDailyStats(start, end, principal);

        BigDecimal totalHours = byDay.stream()
                .map(DailyTimeStatsResponse::getTotalHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalEntries = byDay.stream().mapToInt(DailyTimeStatsResponse::getEntryCount).sum();
        int activeDays = (int) byDay.stream()
                .filter(d -> d.getTotalHours().compareTo(BigDecimal.ZERO) > 0).count();
        long totalDays = ChronoUnit.DAYS.between(start, end) + 1;

        BigDecimal avgPerActiveDay = activeDays > 0
                ? totalHours.divide(BigDecimal.valueOf(activeDays), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal avgPerDay = totalDays > 0
                ? totalHours.divide(BigDecimal.valueOf(totalDays), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return TimeStatsSummaryResponse.builder()
                .startDate(start)
                .endDate(end)
                .totalHours(totalHours)
                .formattedTotalHours(formatHours(totalHours))
                .totalEntries(totalEntries)
                .activeDays(activeDays)
                .avgHoursPerActiveDay(avgPerActiveDay)
                .avgHoursPerDay(avgPerDay)
                .byProject(byProject)
                .byDay(byDay)
                .build();
    }

    /**
     * Thống kê chi tiết của project: theo member, theo task, theo ngày.
     */
    @Transactional(readOnly = true)
    public ProjectDetailTimeStatsResponse getProjectStats(UUID projectId, LocalDate start,
                                                           LocalDate end, UserPrincipal principal) {
        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
        projectAuthorizationService.requirePermission(projectId, principal.getId(), ProjectPermission.REPORT_VIEW);

        // By-member
        List<Object[]> memberRows = timeTrackingRepository
                .sumHoursByMemberForProject(projectId, start, end);
        List<MemberTimeStatsResponse> byMember = memberRows.stream()
                .map(row -> {
                    BigDecimal hours = row[3] != null ? (BigDecimal) row[3] : BigDecimal.ZERO;
                    return MemberTimeStatsResponse.builder()
                            .userId((UUID) row[0])
                            .userName((String) row[1])
                            .userAvatar((String) row[2])
                            .totalHours(hours)
                            .formattedHours(formatHours(hours))
                            .entryCount(row[4] != null ? ((Number) row[4]).intValue() : 0)
                            .build();
                }).collect(Collectors.toList());

        // By-task
        List<Object[]> taskRows = timeTrackingRepository
                .sumHoursByTaskForProject(projectId, start, end);
        List<TaskTimeStatsResponse> byTask = taskRows.stream()
                .map(row -> {
                    BigDecimal logged = row[5] != null ? (BigDecimal) row[5] : BigDecimal.ZERO;
                    BigDecimal estimated = (BigDecimal) row[4];
                    Double progress = null;
                    if (estimated != null && estimated.compareTo(BigDecimal.ZERO) > 0) {
                        progress = logged.divide(estimated, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                                .setScale(1, RoundingMode.HALF_UP)
                                .doubleValue();
                    }
                    return TaskTimeStatsResponse.builder()
                            .taskId((UUID) row[0])
                            .taskKey(project.getKey() + "-" + row[1])
                            .taskTitle((String) row[2])
                            .taskStatus(row[3] != null ? row[3].toString() : null)
                            .estimatedHours(estimated)
                            .loggedHours(logged)
                            .formattedLoggedHours(formatHours(logged))
                            .entryCount(row[6] != null ? ((Number) row[6]).intValue() : 0)
                            .progressPercent(progress)
                            .build();
                }).collect(Collectors.toList());

        // By-day
        List<Object[]> dayRows = timeTrackingRepository
                .sumHoursByDayForProject(projectId, start, end);
        Map<LocalDate, Object[]> dayMap = new LinkedHashMap<>();
        for (Object[] row : dayRows) {
            dayMap.put((LocalDate) row[0], row);
        }

        List<DailyTimeStatsResponse> byDay = new ArrayList<>();
        LocalDate cur = start;
        while (!cur.isAfter(end)) {
            Object[] row = dayMap.get(cur);
            BigDecimal total = row != null && row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            int count = row != null && row[2] != null ? ((Number) row[2]).intValue() : 0;
            byDay.add(DailyTimeStatsResponse.builder()
                    .date(cur)
                    .dayOfWeek(cur.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("vi")))
                    .totalHours(total)
                    .formattedHours(formatHours(total))
                    .entryCount(count)
                    .build());
            cur = cur.plusDays(1);
        }

        BigDecimal totalHours = byMember.stream()
                .map(MemberTimeStatsResponse::getTotalHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalEntries = byMember.stream().mapToInt(MemberTimeStatsResponse::getEntryCount).sum();

        return ProjectDetailTimeStatsResponse.builder()
                .projectId(projectId)
                .projectName(project.getName())
                .projectKey(project.getKey())
                .startDate(start)
                .endDate(end)
                .totalHours(totalHours)
                .formattedTotalHours(formatHours(totalHours))
                .totalEntries(totalEntries)
                .byMember(byMember)
                .byTask(byTask)
                .byDay(byDay)
                .build();
    }

    // ========== HELPERS ==========

    private void updateTaskActualHours(Task task) {
        BigDecimal totalHours = timeTrackingRepository.sumHoursByTaskId(task.getId())
                .orElse(BigDecimal.ZERO);
        task.setActualHours(totalHours);
        taskRepository.save(task);
    }

    public static String formatHours(BigDecimal hours) {
        if (hours == null || hours.compareTo(BigDecimal.ZERO) == 0) return "0h";
        double total = hours.doubleValue();
        int h = (int) total;
        int m = (int) Math.round((total - h) * 60);
        if (m == 0) return h + "h";
        if (h == 0) return m + "m";
        return h + "h " + m + "m";
    }
}
