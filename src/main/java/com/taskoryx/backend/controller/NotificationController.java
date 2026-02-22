package com.taskoryx.backend.controller;

import com.taskoryx.backend.dto.response.ApiResponse;
import com.taskoryx.backend.dto.response.PagedResponse;
import com.taskoryx.backend.dto.response.notification.NotificationResponse;
import com.taskoryx.backend.security.UserPrincipal;
import com.taskoryx.backend.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST Controller cho Notifications (In-app)
 *
 * GET    /api/notifications                        - Danh sách thông báo
 * GET    /api/notifications/unread-count           - Số thông báo chưa đọc
 * PATCH  /api/notifications/{id}/read              - Đánh dấu đã đọc
 * PATCH  /api/notifications/read-all               - Đánh dấu tất cả đã đọc
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Thông báo in-app")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Lấy danh sách thông báo của tôi")
    public ResponseEntity<ApiResponse<PagedResponse<NotificationResponse>>> getNotifications(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var result = notificationService.getNotifications(principal, page, size);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(result)));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Lấy số lượng thông báo chưa đọc")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal principal) {
        long count = notificationService.getUnreadCount(principal);
        return ResponseEntity.ok(ApiResponse.success(Map.of("unreadCount", count)));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Đánh dấu thông báo đã đọc")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.markAsRead(id, principal)));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Đánh dấu tất cả thông báo đã đọc")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal principal) {
        int count = notificationService.markAllAsRead(principal);
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("markedCount", count)));
    }
}
