package com.taskoryx.backend.ai.service;

import com.taskoryx.backend.ai.dto.request.AiConfirmPlanRequest;
import com.taskoryx.backend.ai.dto.request.AiGeneratePlanRequest;
import com.taskoryx.backend.ai.dto.response.AiExecuteResult;
import com.taskoryx.backend.ai.dto.response.AiPlanPreviewResponse;
import com.taskoryx.backend.ai.dto.response.AiProjectPlan;
import com.taskoryx.backend.ai.dto.response.AiTaskItem;
import com.taskoryx.backend.ai.parser.AiResponseParser;
import com.taskoryx.backend.ai.prompt.ProjectPlanPrompt;
import com.taskoryx.backend.ai.skill.AiPlanExecutor;
import com.taskoryx.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Điều phối luồng AI: nhận yêu cầu → gọi AI → parse → preview → execute.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiProjectPlanService {

    private final AiChatService aiChatService;
    private final ProjectPlanPrompt projectPlanPrompt;
    private final AiResponseParser aiResponseParser;
    private final AiPlanExecutor aiPlanExecutor;

    /**
     * Bước 1: Sinh kế hoạch dự án từ yêu cầu ngôn ngữ tự nhiên.
     * Chỉ trả preview — chưa ghi DB.
     */
    public AiPlanPreviewResponse generatePlan(AiGeneratePlanRequest request) {
        log.info("Generating AI plan for requirement: {}", request.getRequirement());

        String systemPrompt = projectPlanPrompt.buildSystemPrompt();
        String userPrompt = projectPlanPrompt.buildUserPrompt(request.getRequirement(), request.getLanguage());

        String rawResponse = aiChatService.chat(systemPrompt, userPrompt);
        AiProjectPlan plan = aiResponseParser.parseProjectPlan(rawResponse);

        return AiPlanPreviewResponse.builder()
                .plan(plan)
                .totalTaskCount(countAllTasks(plan.getTasks()))
                .modelUsed(aiChatService.getModelName())
                .build();
    }

    /**
     * Bước 2: Xác nhận và thực thi kế hoạch — ghi DB thật.
     * AI không insert DB trực tiếp; AiPlanExecutor đóng vai trò skill layer.
     */
    public AiExecuteResult confirmAndExecute(AiConfirmPlanRequest request, UserPrincipal principal) {
        log.info("Executing AI plan: projectName={}, tasks={}",
                request.getPlan().getProjectName(),
                request.getPlan().getTasks() != null ? request.getPlan().getTasks().size() : 0);

        return aiPlanExecutor.execute(request.getPlan(), request.getTargetProjectId(), principal);
    }

    private int countAllTasks(List<AiTaskItem> tasks) {
        if (tasks == null) return 0;
        int count = tasks.size();
        for (AiTaskItem task : tasks) {
            if (task.getSubTasks() != null) count += task.getSubTasks().size();
        }
        return count;
    }
}
