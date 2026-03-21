package com.taskoryx.backend.controller;

import com.taskoryx.backend.dto.request.webhook.CreateWebhookRequest;
import com.taskoryx.backend.dto.request.webhook.UpdateWebhookRequest;
import com.taskoryx.backend.dto.response.ApiResponse;
import com.taskoryx.backend.dto.response.webhook.WebhookResponse;
import com.taskoryx.backend.security.UserPrincipal;
import com.taskoryx.backend.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Tích hợp webhook với Slack/Discord/hệ thống ngoài")
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping("/projects/{projectId}/webhooks")
    @Operation(summary = "Tạo webhook mới cho project")
    public ResponseEntity<ApiResponse<WebhookResponse>> createWebhook(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateWebhookRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo webhook thành công",
                        webhookService.createWebhook(projectId, request, principal)));
    }

    @GetMapping("/projects/{projectId}/webhooks")
    @Operation(summary = "Lấy danh sách webhook của project")
    public ResponseEntity<ApiResponse<List<WebhookResponse>>> getWebhooks(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                webhookService.getProjectWebhooks(projectId, principal)));
    }

    @PutMapping("/webhooks/{id}")
    @Operation(summary = "Cập nhật webhook")
    public ResponseEntity<ApiResponse<WebhookResponse>> updateWebhook(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UpdateWebhookRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật webhook thành công",
                webhookService.updateWebhook(id, request, principal)));
    }

    @DeleteMapping("/webhooks/{id}")
    @Operation(summary = "Xóa webhook")
    public ResponseEntity<ApiResponse<Void>> deleteWebhook(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        webhookService.deleteWebhook(id, principal);
        return ResponseEntity.ok(ApiResponse.success("Xóa webhook thành công"));
    }

    @PostMapping("/webhooks/{id}/test")
    @Operation(summary = "Kiểm tra webhook bằng cách gửi ping")
    public ResponseEntity<ApiResponse<WebhookResponse>> testWebhook(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Đã gửi test payload",
                webhookService.testWebhook(id, principal)));
    }
}
