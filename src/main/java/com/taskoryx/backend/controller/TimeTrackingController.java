package com.taskoryx.backend.controller;

import com.taskoryx.backend.dto.request.timetracking.CreateTimeEntryRequest;
import com.taskoryx.backend.dto.request.timetracking.UpdateTimeEntryRequest;
import com.taskoryx.backend.dto.response.ApiResponse;
import com.taskoryx.backend.dto.response.PagedResponse;
import com.taskoryx.backend.dto.response.timetracking.*;
import com.taskoryx.backend.security.UserPrincipal;
import com.taskoryx.backend.service.TimeTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller cho Time Tracking
 *
 * POST   /api/time-entries                               - Ghi nhận thời gian làm việc
 * GET    /api/tasks/{taskId}/time-entries                - Lấy danh sách time entries của task
 * GET    /api/tasks/{taskId}/time-entries/total          - Tổng giờ của task
 * GET    /api/time-entries/my                            - Lấy time entries của tôi (có phân trang)
 * GET    /api/time-entries/range                         - Lấy time entries theo khoảng thời gian
 * PUT    /api/time-entries/{id}                          - Cập nhật time entry
 * DELETE /api/time-entries/{id}                          - Xóa time entry
 *
 * --- THỐNG KÊ ---
 * GET    /api/time-entries/stats/daily?start=&end=       - Thống kê theo ngày
 * GET    /api/time-entries/stats/weekly?start=&end=      - Thống kê theo tuần
 * GET    /api/time-entries/stats/monthly?year=           - Thống kê theo tháng trong năm
 * GET    /api/time-entries/stats/summary?start=&end=     - Tổng hợp (tổng giờ, theo project, theo ngày)
 * GET    /api/projects/{id}/time-entries/stats?start=&end= - Thống kê chi tiết project
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Time Tracking", description = "Theo dõi thời gian làm việc trên task")
public class TimeTrackingController {

    private final TimeTrackingService timeTrackingService;

    @PostMapping("/time-entries")
    @Operation(summary = "Ghi nhận thời gian làm việc trên task")
    public ResponseEntity<ApiResponse<TimeTrackingResponse>> logTime(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateTimeEntryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Ghi nhận thời gian thành công",
                        timeTrackingService.logTime(request, principal)));
    }

    @GetMapping("/tasks/{taskId}/time-entries")
    @Operation(summary = "Lấy danh sách time entries của task")
    public ResponseEntity<ApiResponse<List<TimeTrackingResponse>>> getTimeEntriesForTask(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                timeTrackingService.getTimeEntriesForTask(taskId, principal)));
    }

    @GetMapping("/time-entries/my")
    @Operation(summary = "Lấy danh sách time entries của tôi")
    public ResponseEntity<ApiResponse<PagedResponse<TimeTrackingResponse>>> getMyTimeEntries(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var result = timeTrackingService.getMyTimeEntries(principal, page, size);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(result)));
    }

    @GetMapping("/time-entries/range")
    @Operation(summary = "Lấy time entries theo khoảng thời gian")
    public ResponseEntity<ApiResponse<List<TimeTrackingResponse>>> getTimeEntriesByDateRange(
            @RequestParam(required = false) UUID userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                timeTrackingService.getTimeEntriesByDateRange(userId, start, end, principal)));
    }

    @PutMapping("/time-entries/{id}")
    @Operation(summary = "Cập nhật time entry")
    public ResponseEntity<ApiResponse<TimeTrackingResponse>> updateTimeEntry(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateTimeEntryRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thành công",
                timeTrackingService.updateTimeEntry(id, request, principal)));
    }

    @DeleteMapping("/time-entries/{id}")
    @Operation(summary = "Xóa time entry")
    public ResponseEntity<ApiResponse<Void>> deleteTimeEntry(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        timeTrackingService.deleteTimeEntry(id, principal);
        return ResponseEntity.ok(ApiResponse.success("Xóa time entry thành công"));
    }

    @GetMapping("/tasks/{taskId}/time-entries/total")
    @Operation(summary = "Lấy tổng số giờ đã log của task")
    public ResponseEntity<ApiResponse<BigDecimal>> getTaskTotalHours(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                timeTrackingService.getTaskTotalHours(taskId, principal)));
    }

    // ========== STATISTICS ENDPOINTS ==========

    @GetMapping("/time-entries/stats/daily")
    @Operation(summary = "Thống kê thời gian theo từng ngày",
               description = "Trả về danh sách ngày với tổng giờ và chi tiết entries. " +
                             "Mặc định: 30 ngày gần nhất.")
    public ResponseEntity<ApiResponse<List<DailyTimeStatsResponse>>> getDailyStats(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Ngày bắt đầu (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @Parameter(description = "Ngày kết thúc (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        LocalDate effectiveEnd = end != null ? end : LocalDate.now();
        LocalDate effectiveStart = start != null ? start : effectiveEnd.minusDays(29);
        return ResponseEntity.ok(ApiResponse.success(
                timeTrackingService.getDailyStats(effectiveStart, effectiveEnd, principal)));
    }

    @GetMapping("/time-entries/stats/weekly")
    @Operation(summary = "Thống kê thời gian theo tuần",
               description = "Mỗi tuần bao gồm breakdown theo ngày. Mặc định: 12 tuần gần nhất.")
    public ResponseEntity<ApiResponse<List<WeeklyTimeStatsResponse>>> getWeeklyStats(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        LocalDate effectiveEnd = end != null ? end : LocalDate.now();
        LocalDate effectiveStart = start != null ? start : effectiveEnd.minusWeeks(11);
        return ResponseEntity.ok(ApiResponse.success(
                timeTrackingService.getWeeklyStats(effectiveStart, effectiveEnd, principal)));
    }

    @GetMapping("/time-entries/stats/monthly")
    @Operation(summary = "Thống kê thời gian theo tháng trong năm",
               description = "Trả về 12 tháng của năm chỉ định, kèm breakdown theo ngày cho tháng có entries.")
    public ResponseEntity<ApiResponse<List<MonthlyTimeStatsResponse>>> getMonthlyStats(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Năm cần thống kê (mặc định: năm hiện tại)")
            @RequestParam(required = false) Integer year) {
        int effectiveYear = year != null ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(ApiResponse.success(
                timeTrackingService.getMonthlyStats(effectiveYear, principal)));
    }

    @GetMapping("/time-entries/stats/summary")
    @Operation(summary = "Tổng hợp thống kê thời gian",
               description = "Tổng giờ, trung bình/ngày, breakdown theo project và theo ngày. " +
                             "Mặc định: 30 ngày gần nhất.")
    public ResponseEntity<ApiResponse<TimeStatsSummaryResponse>> getSummary(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        LocalDate effectiveEnd = end != null ? end : LocalDate.now();
        LocalDate effectiveStart = start != null ? start : effectiveEnd.minusDays(29);
        return ResponseEntity.ok(ApiResponse.success(
                timeTrackingService.getSummary(effectiveStart, effectiveEnd, principal)));
    }

    @GetMapping("/projects/{projectId}/time-entries/stats")
    @Operation(summary = "Thống kê thời gian chi tiết của project",
               description = "Breakdown theo member, theo task và theo ngày cho project. " +
                             "Mặc định: 30 ngày gần nhất.")
    public ResponseEntity<ApiResponse<ProjectDetailTimeStatsResponse>> getProjectStats(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        LocalDate effectiveEnd = end != null ? end : LocalDate.now();
        LocalDate effectiveStart = start != null ? start : effectiveEnd.minusDays(29);
        return ResponseEntity.ok(ApiResponse.success(
                timeTrackingService.getProjectStats(projectId, effectiveStart, effectiveEnd, principal)));
    }
}
