package com.taskoryx.backend.ai.controller;

import com.taskoryx.backend.ai.dto.request.AiConfirmPlanRequest;
import com.taskoryx.backend.ai.dto.request.AiGeneratePlanRequest;
import com.taskoryx.backend.ai.dto.response.AiGenerateSessionResponse;
import com.taskoryx.backend.ai.dto.response.AiJobResponse;
import com.taskoryx.backend.ai.service.AiGenerateService;
import com.taskoryx.backend.ai.service.AiJobService;
import com.taskoryx.backend.dto.response.ApiResponse;
import com.taskoryx.backend.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/ai/projects")
@RequiredArgsConstructor
@Tag(name = "AI Project Planning", description = "Sinh kế hoạch dự án bằng AI từ ngôn ngữ tự nhiên")
public class AiProjectPlanController {

    private final AiJobService aiJobService;
    private final AiGenerateService aiGenerateService;

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAuthority('PROJECT_CREATE')")
    @Operation(
        summary = "Sinh kế hoạch dự án bằng AI (bất đồng bộ)",
        description = "Nhận yêu cầu ngôn ngữ tự nhiên, tạo session và chạy AI ngầm. Trả về sessionId ngay (HTTP 202). " +
                      "Dùng GET /ai/projects/sessions/{sessionId} để poll hoặc nghe WebSocket /queue/ai-plan-ready."
    )
    public ApiResponse<AiGenerateSessionResponse> generatePlan(
            @Valid @RequestBody AiGeneratePlanRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AiGenerateSessionResponse session = aiGenerateService.startGenerate(request, principal);
        return ApiResponse.success("AI đang phân tích yêu cầu, bạn sẽ nhận thông báo khi xong", session);
    }

    @GetMapping("/sessions/{sessionId}")
    @PreAuthorize("hasAuthority('PROJECT_CREATE')")
    @Operation(
        summary = "Lấy kết quả AI generate session",
        description = "Poll trạng thái session. Status: GENERATING → READY | FAILED. Khi READY trả kèm plan."
    )
    public ApiResponse<AiGenerateSessionResponse> getSession(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AiGenerateSessionResponse session = aiGenerateService.getSession(sessionId, principal);
        return ApiResponse.success("Lấy session thành công", session);
    }

    @PostMapping("/confirm")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAuthority('PROJECT_CREATE')")
    @Operation(
        summary = "Xác nhận kế hoạch AI — tạo job ngầm",
        description = "Enqueue job tạo Project + Sprint + Task. Trả về jobId ngay lập tức (HTTP 202). " +
                      "Dùng GET /ai/projects/jobs/{jobId} để poll trạng thái. Chỉ 1 job chạy cùng lúc; " +
                      "các job khác xếp hàng chờ. Khi hoàn thành sẽ gửi notification realtime."
    )
    public ApiResponse<AiJobResponse> confirmPlan(
            @Valid @RequestBody AiConfirmPlanRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AiJobResponse job = aiJobService.enqueueJob(request, principal);
        return ApiResponse.success("Job đã được tạo và đang xử lý", job);
    }

    @GetMapping("/jobs/{jobId}")
    @PreAuthorize("hasAuthority('PROJECT_CREATE')")
    @Operation(
        summary = "Kiểm tra trạng thái AI job",
        description = "Poll trạng thái job tạo dự án. Status: PENDING → RUNNING → DONE | FAILED."
    )
    public ApiResponse<AiJobResponse> getJobStatus(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AiJobResponse job = aiJobService.getJobStatus(jobId, principal);
        return ApiResponse.success("Lấy trạng thái job thành công", job);
    }
}
