package com.taskoryx.backend.controller;

import com.taskoryx.backend.dto.request.timetracking.CreateTimeEntryRequest;
import com.taskoryx.backend.dto.request.timetracking.UpdateTimeEntryRequest;
import com.taskoryx.backend.dto.response.ApiResponse;
import com.taskoryx.backend.dto.response.PagedResponse;
import com.taskoryx.backend.dto.response.timetracking.TimeTrackingResponse;
import com.taskoryx.backend.security.UserPrincipal;
import com.taskoryx.backend.service.TimeTrackingService;
import io.swagger.v3.oas.annotations.Operation;
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
 * POST   /api/time-entries                         - Ghi nhận thời gian làm việc
 * GET    /api/tasks/{taskId}/time-entries          - Lấy danh sách time entries của task
 * GET    /api/time-entries/my                      - Lấy time entries của tôi (có phân trang)
 * GET    /api/time-entries/range                   - Lấy time entries theo khoảng thời gian
 * PUT    /api/time-entries/{id}                    - Cập nhật time entry
 * DELETE /api/time-entries/{id}                    - Xóa time entry
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
}
