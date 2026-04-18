package com.taskoryx.backend.controller;

import com.taskoryx.backend.dto.response.ApiResponse;
import com.taskoryx.backend.dto.response.performance.UserPerformanceResponse;
import com.taskoryx.backend.security.UserPrincipal;
import com.taskoryx.backend.service.PerformanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller cho Performance System
 *
 * GET  /api/projects/{id}/performance                    - Điểm hiệu suất tất cả thành viên
 * GET  /api/projects/{id}/performance/me                 - Điểm hiệu suất của mình
 * POST /api/projects/{id}/performance/calculate          - Tính lại điểm (Admin/Owner)
 * GET  /api/projects/{id}/performance/sprint/{sprintId}  - Điểm theo sprint
 */
@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
@Tag(name = "Performance", description = "Đánh giá năng lực nhân sự")
public class PerformanceController {

    private final PerformanceService performanceService;

    @GetMapping("/{id}/performance")
    @Operation(summary = "Lấy điểm hiệu suất tất cả thành viên của project")
    public ResponseEntity<ApiResponse<List<UserPerformanceResponse>>> getProjectPerformance(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                performanceService.getProjectPerformance(id, principal)));
    }

    @GetMapping("/{id}/performance/me")
    @Operation(summary = "Lấy điểm hiệu suất của bản thân trong project")
    public ResponseEntity<ApiResponse<UserPerformanceResponse>> getMyPerformance(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                performanceService.getMyPerformance(id, principal)));
    }

    @PostMapping("/{id}/performance/calculate")
    @Operation(summary = "Tính lại điểm hiệu suất cho tất cả thành viên (chỉ Owner/Admin)")
    public ResponseEntity<ApiResponse<List<UserPerformanceResponse>>> calculatePerformance(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Tính toán điểm hiệu suất thành công",
                performanceService.calculateProjectPerformance(id, principal)));
    }

    @GetMapping("/{id}/performance/sprint/{sprintId}")
    @Operation(summary = "Lấy điểm hiệu suất theo sprint")
    public ResponseEntity<ApiResponse<List<UserPerformanceResponse>>> getSprintPerformance(
            @PathVariable UUID id,
            @PathVariable UUID sprintId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                performanceService.getSprintPerformance(id, sprintId, principal)));
    }
}
