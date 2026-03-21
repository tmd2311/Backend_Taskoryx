package com.taskoryx.backend.controller;

import com.taskoryx.backend.dto.response.ApiResponse;
import com.taskoryx.backend.dto.response.dashboard.ProjectDashboardResponse;
import com.taskoryx.backend.dto.response.dashboard.UserDashboardResponse;
import com.taskoryx.backend.security.UserPrincipal;
import com.taskoryx.backend.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller cho Dashboard
 *
 * GET /api/dashboard/me                       - Dashboard cá nhân
 * GET /api/dashboard/projects/{projectId}     - Dashboard của project
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/dashboard")
@Tag(name = "Dashboard", description = "Thống kê và tổng quan")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/me")
    @Operation(summary = "Lấy dashboard cá nhân của người dùng hiện tại")
    public ResponseEntity<ApiResponse<UserDashboardResponse>> getUserDashboard(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getUserDashboard(principal)));
    }

    @GetMapping("/projects/{projectId}")
    @Operation(summary = "Lấy dashboard tổng quan của project")
    public ResponseEntity<ApiResponse<ProjectDashboardResponse>> getProjectDashboard(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getProjectDashboard(projectId, principal)));
    }
}
