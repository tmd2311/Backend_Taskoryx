package com.taskoryx.backend.ai.controller;

import com.taskoryx.backend.ai.dto.request.AiConfirmPlanRequest;
import com.taskoryx.backend.ai.dto.request.AiGeneratePlanRequest;
import com.taskoryx.backend.ai.dto.response.AiExecuteResult;
import com.taskoryx.backend.ai.dto.response.AiPlanPreviewResponse;
import com.taskoryx.backend.ai.service.AiProjectPlanService;
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

@RestController
@RequestMapping("/ai/projects")
@RequiredArgsConstructor
@Tag(name = "AI Project Planning", description = "Sinh kế hoạch dự án bằng AI từ ngôn ngữ tự nhiên")
public class AiProjectPlanController {

    private final AiProjectPlanService aiProjectPlanService;

    @PostMapping("/generate")
    @PreAuthorize("hasAuthority('PROJECT_CREATE')")
    @Operation(
        summary = "Sinh kế hoạch dự án bằng AI",
        description = "Nhận yêu cầu ngôn ngữ tự nhiên, trả preview kế hoạch (chưa ghi DB). Yêu cầu quyền PROJECT_CREATE (PROJECT_MANAGER hoặc SUPER_ADMIN)."
    )
    public ApiResponse<AiPlanPreviewResponse> generatePlan(
            @Valid @RequestBody AiGeneratePlanRequest request) {
        AiPlanPreviewResponse preview = aiProjectPlanService.generatePlan(request);
        return ApiResponse.success("Sinh kế hoạch thành công", preview);
    }

    @PostMapping("/confirm")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PROJECT_CREATE')")
    @Operation(
        summary = "Xác nhận và tạo dự án từ kế hoạch AI",
        description = "Nhận kế hoạch đã xem trước, thực thi tạo Project + Task thật trong DB. Yêu cầu quyền PROJECT_CREATE (PROJECT_MANAGER hoặc SUPER_ADMIN)."
    )
    public ApiResponse<AiExecuteResult> confirmPlan(
            @Valid @RequestBody AiConfirmPlanRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AiExecuteResult result = aiProjectPlanService.confirmAndExecute(request, principal);
        return ApiResponse.success("Tạo dự án thành công", result);
    }
}
