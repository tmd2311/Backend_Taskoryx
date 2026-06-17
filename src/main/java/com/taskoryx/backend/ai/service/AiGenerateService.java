package com.taskoryx.backend.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskoryx.backend.ai.dto.request.AiGeneratePlanRequest;
import com.taskoryx.backend.ai.dto.response.AiGenerateSessionResponse;
import com.taskoryx.backend.ai.dto.response.AiProjectPlan;
import com.taskoryx.backend.entity.AiGenerateSession;
import com.taskoryx.backend.entity.User;
import com.taskoryx.backend.exception.ForbiddenException;
import com.taskoryx.backend.exception.ResourceNotFoundException;
import com.taskoryx.backend.repository.AiGenerateSessionRepository;
import com.taskoryx.backend.repository.UserRepository;
import com.taskoryx.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiGenerateService {

    private final AiGenerateSessionRepository sessionRepository;
    private final AiGenerateAsyncRunner asyncRunner;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final AiRateLimiter rateLimiter;
    private final AiInputGuard inputGuard;

    /**
     * Tạo session GENERATING và trả về ngay (HTTP 202).
     * AI chạy ngầm qua AiGenerateAsyncRunner — semaphore giới hạn 1 job cùng lúc.
     */
    @Transactional
    public AiGenerateSessionResponse startGenerate(AiGeneratePlanRequest request, UserPrincipal principal) {
        rateLimiter.checkGenerate(principal.getId());
        inputGuard.validate(request.getRequirement());

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user"));

        AiGenerateSession session = AiGenerateSession.builder()
                .user(user)
                .status(AiGenerateSession.SessionStatus.GENERATING)
                .requirement(inputGuard.sanitize(request.getRequirement()))
                .language(request.getLanguage() != null ? request.getLanguage() : "vi")
                .build();

        session = sessionRepository.save(session);
        log.info("AI generate session created: sessionId={}, userId={}", session.getId(), principal.getId());

        // Chạy sau khi transaction commit để async thread thấy session trong DB
        final UUID sessionId = session.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                asyncRunner.run(sessionId);
            }
        });

        return AiGenerateSessionResponse.pending(session);
    }

    /**
     * Lưu lại plan đã được user chỉnh sửa vào session. Chỉ cho phép khi session ở trạng thái READY.
     */
    @Transactional
    public AiGenerateSessionResponse updateSessionPlan(UUID sessionId, AiProjectPlan editedPlan, UserPrincipal principal) {
        AiGenerateSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy session"));

        if (!session.getUser().getId().equals(principal.getId())) {
            throw new ForbiddenException("Bạn không có quyền chỉnh sửa session này");
        }
        if (session.getStatus() != AiGenerateSession.SessionStatus.READY) {
            throw new com.taskoryx.backend.exception.BadRequestException(
                    "Chỉ có thể chỉnh sửa plan khi session ở trạng thái READY");
        }

        try {
            session.setPlanJson(objectMapper.writeValueAsString(editedPlan));
        } catch (Exception e) {
            throw new com.taskoryx.backend.exception.BadRequestException("Không thể serialize plan");
        }
        session = sessionRepository.save(session);
        return AiGenerateSessionResponse.from(session, editedPlan);
    }

    /**
     * Poll trạng thái session. Khi READY trả kèm plan đã parse.
     */
    @Transactional(readOnly = true)
    public AiGenerateSessionResponse getSession(UUID sessionId, UserPrincipal principal) {
        AiGenerateSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy session"));

        if (!session.getUser().getId().equals(principal.getId())) {
            throw new ForbiddenException("Bạn không có quyền xem session này");
        }

        AiProjectPlan plan = null;
        if (session.getStatus() == AiGenerateSession.SessionStatus.READY && session.getPlanJson() != null) {
            try {
                log.debug("planJson preview: {}", session.getPlanJson().length() > 200
                        ? session.getPlanJson().substring(0, 200) : session.getPlanJson());
                plan = objectMapper.readValue(session.getPlanJson(), AiProjectPlan.class);
                log.debug("plan parsed: projectName={}, sprints={}", plan.getProjectName(),
                        plan.getSprints() != null ? plan.getSprints().size() : null);
            } catch (Exception e) {
                log.error("Failed to parse plan JSON for session {}", sessionId, e);
            }
        }

        return AiGenerateSessionResponse.from(session, plan);
    }
}
