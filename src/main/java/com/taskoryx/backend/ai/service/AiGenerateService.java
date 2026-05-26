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

    /**
     * Tạo session GENERATING và trả về ngay (HTTP 202).
     * AI chạy ngầm qua AiGenerateAsyncRunner — semaphore giới hạn 1 job cùng lúc.
     */
    @Transactional
    public AiGenerateSessionResponse startGenerate(AiGeneratePlanRequest request, UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user"));

        AiGenerateSession session = AiGenerateSession.builder()
                .user(user)
                .status(AiGenerateSession.SessionStatus.GENERATING)
                .requirement(request.getRequirement())
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
                plan = objectMapper.readValue(session.getPlanJson(), AiProjectPlan.class);
            } catch (Exception e) {
                log.error("Failed to parse plan JSON for session {}", sessionId, e);
            }
        }

        return AiGenerateSessionResponse.from(session, plan);
    }
}
