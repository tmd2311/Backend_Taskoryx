package com.taskoryx.backend.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskoryx.backend.ai.dto.response.AiProjectPlan;
import com.taskoryx.backend.ai.parser.AiResponseParser;
import com.taskoryx.backend.ai.prompt.ProjectPlanPrompt;
import com.taskoryx.backend.dto.response.notification.NotificationResponse;
import com.taskoryx.backend.entity.AiGenerateSession;
import com.taskoryx.backend.entity.Notification;
import com.taskoryx.backend.exception.BadRequestException;
import com.taskoryx.backend.repository.AiGenerateSessionRepository;
import com.taskoryx.backend.repository.NotificationRepository;
import com.taskoryx.backend.service.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.Semaphore;

/**
 * Bean riêng để chạy AI generate bất đồng bộ — tách khỏi AiGenerateService
 * để Spring @Async AOP proxy hoạt động đúng.
 * Semaphore giới hạn toàn server: chỉ 1 generate chạy cùng lúc.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiGenerateAsyncRunner {

    private static final Semaphore GENERATE_SEMAPHORE = new Semaphore(1);

    private final AiGenerateSessionRepository sessionRepository;
    private final AiChatService aiChatService;
    private final ProjectPlanPrompt projectPlanPrompt;
    private final AiResponseParser aiResponseParser;
    private final NotificationRepository notificationRepository;
    private final WebSocketNotificationService webSocketNotificationService;
    private final ObjectMapper objectMapper;

    @Async
    public void run(UUID sessionId) {
        log.info("AI generate async run started: sessionId={}, semaphorePermits={}",
                sessionId, GENERATE_SEMAPHORE.availablePermits());
        boolean acquired = false;
        try {
            acquired = GENERATE_SEMAPHORE.tryAcquire();
            if (!acquired) {
                log.info("AI generate queued (slot busy): sessionId={}", sessionId);
                GENERATE_SEMAPHORE.acquire();
                acquired = true;
            }
            log.info("AI generate semaphore acquired: sessionId={}", sessionId);
            doGenerate(sessionId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            markFailed(sessionId, "Tác vụ bị gián đoạn");
        } finally {
            if (acquired) GENERATE_SEMAPHORE.release();
        }
    }

    private void doGenerate(UUID sessionId) {
        log.info("AI generate doGenerate started: sessionId={}", sessionId);
        AiGenerateSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            log.warn("AI generate session not found in DB: sessionId={}", sessionId);
            return;
        }

        try {
            log.info("AI generate calling LLM: sessionId={}, model={}", sessionId, aiChatService.getModelName());
            long t0 = System.currentTimeMillis();

            String systemPrompt = projectPlanPrompt.buildSystemPrompt();
            String userPrompt   = projectPlanPrompt.buildUserPrompt(session.getRequirement(), session.getLanguage());

            String rawResponse = aiChatService.chat(systemPrompt, userPrompt);
            log.info("AI generate LLM responded: sessionId={}, elapsed={}ms, responseLength={}",
                    sessionId, System.currentTimeMillis() - t0, rawResponse != null ? rawResponse.length() : 0);

            AiProjectPlan plan = aiResponseParser.parseProjectPlan(rawResponse);
            log.info("AI generate plan parsed: sessionId={}, sprints={}", sessionId,
                    plan.getSprints() != null ? plan.getSprints().size() : 0);

            String planJson = objectMapper.writeValueAsString(plan);
            int totalTaskCount = countAllTasks(plan);

            session.setStatus(AiGenerateSession.SessionStatus.READY);
            session.setPlanJson(planJson);
            session.setTotalTaskCount(totalTaskCount);
            session.setModelUsed(aiChatService.getModelName());
            session.setFinishedAt(LocalDateTime.now());
            sessionRepository.save(session);

            log.info("AI generate session ready: sessionId={}, tasks={}", sessionId, totalTaskCount);
            sendReadyNotification(session);

        } catch (BadRequestException e) {
            // LLM rejected request (safety/validation) hoặc parse error — thông báo rõ lý do
            log.warn("AI generate rejected/invalid: sessionId={}, reason={}", sessionId, e.getMessage());
            session.setStatus(AiGenerateSession.SessionStatus.FAILED);
            session.setErrorMessage(e.getMessage());
            session.setFinishedAt(LocalDateTime.now());
            sessionRepository.save(session);
            sendRejectedNotification(session, e.getMessage());
        } catch (Exception e) {
            log.error("AI generate session failed: sessionId={}", sessionId, e);
            session.setStatus(AiGenerateSession.SessionStatus.FAILED);
            session.setErrorMessage(e.getMessage() != null && e.getMessage().length() > 500
                    ? e.getMessage().substring(0, 500) : e.getMessage());
            session.setFinishedAt(LocalDateTime.now());
            sessionRepository.save(session);
            sendFailedNotification(session);
        }
    }

    private void markFailed(UUID sessionId, String errorMessage) {
        sessionRepository.findById(sessionId).ifPresent(session -> {
            session.setStatus(AiGenerateSession.SessionStatus.FAILED);
            session.setErrorMessage(errorMessage);
            session.setFinishedAt(LocalDateTime.now());
            sessionRepository.save(session);
        });
    }

    private void sendReadyNotification(AiGenerateSession session) {
        try {
            Notification notification = Notification.builder()
                    .user(session.getUser())
                    .type(Notification.NotificationType.TASK_UPDATED)
                    .title("Kế hoạch AI đã sẵn sàng")
                    .message("AI đã sinh xong kế hoạch. Nhấn để xem trước và xác nhận tạo dự án.")
                    .relatedType(Notification.RelatedType.PROJECT)
                    .relatedId(session.getId())
                    .build();
            notification = notificationRepository.save(notification);

            webSocketNotificationService.sendNotificationToUser(
                    session.getUser().getId(), NotificationResponse.from(notification));
            webSocketNotificationService.sendAiPlanReady(
                    session.getUser().getId(), session.getId());
        } catch (Exception e) {
            log.error("Failed to send ready notification: sessionId={}", session.getId(), e);
        }
    }

    private void sendRejectedNotification(AiGenerateSession session, String reason) {
        try {
            Notification notification = Notification.builder()
                    .user(session.getUser())
                    .type(Notification.NotificationType.TASK_UPDATED)
                    .title("Yêu cầu AI bị từ chối")
                    .message(reason != null ? reason : "Yêu cầu không phù hợp để lập kế hoạch dự án.")
                    .relatedType(Notification.RelatedType.PROJECT)
                    .relatedId(null)
                    .build();
            notification = notificationRepository.save(notification);

            webSocketNotificationService.sendNotificationToUser(
                    session.getUser().getId(), NotificationResponse.from(notification));
            webSocketNotificationService.sendAiPlanRejected(
                    session.getUser().getId(), session.getId(), notification.getMessage());
        } catch (Exception e) {
            log.error("Failed to send rejected notification: sessionId={}", session.getId(), e);
        }
    }

    private void sendFailedNotification(AiGenerateSession session) {
        try {
            Notification notification = Notification.builder()
                    .user(session.getUser())
                    .type(Notification.NotificationType.TASK_UPDATED)
                    .title("Sinh kế hoạch AI thất bại")
                    .message("AI không thể sinh kế hoạch. Vui lòng thử lại với mô tả khác.")
                    .relatedType(Notification.RelatedType.PROJECT)
                    .relatedId(null)
                    .build();
            notification = notificationRepository.save(notification);

            webSocketNotificationService.sendNotificationToUser(
                    session.getUser().getId(), NotificationResponse.from(notification));
        } catch (Exception e) {
            log.error("Failed to send failed notification: sessionId={}", session.getId(), e);
        }
    }

    private int countAllTasks(AiProjectPlan plan) {
        if (plan.getSprints() == null) return 0;
        int count = 0;
        for (var sprint : plan.getSprints()) {
            if (sprint.getTasks() == null) continue;
            count += sprint.getTasks().size();
            for (var task : sprint.getTasks()) {
                if (task.getSubTasks() != null) count += task.getSubTasks().size();
            }
        }
        return count;
    }
}
