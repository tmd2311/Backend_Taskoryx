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

    // ---------------------------------------------------------------------------
    // MOCK MODE — bật qua application.yaml: application.ai.mock=true
    // ---------------------------------------------------------------------------
    @org.springframework.beans.factory.annotation.Value("${application.ai.mock:false}")
    private boolean mockMode;

    private void doGenerate(UUID sessionId) {
        log.info("AI generate doGenerate started: sessionId={}, mockMode={}", sessionId, mockMode);
        AiGenerateSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            log.warn("AI generate session not found in DB: sessionId={}", sessionId);
            return;
        }

        try {
            AiProjectPlan plan;
            String modelUsed;

            if (mockMode) {
                log.info("AI generate MOCK: skipping LLM call, returning hardcoded plan: sessionId={}", sessionId);
                // Giả lập delay nhỏ cho thực tế (optional)
                Thread.sleep(800);
                plan = buildMockPlan(session.getRequirement());
                modelUsed = "mock/no-api-call";
            } else {
                log.info("AI generate calling LLM: sessionId={}, model={}", sessionId, aiChatService.getModelName());
                long t0 = System.currentTimeMillis();

                String systemPrompt = projectPlanPrompt.buildSystemPrompt();
                String userPrompt   = projectPlanPrompt.buildUserPrompt(session.getRequirement(), session.getLanguage());

                String rawResponse = aiChatService.chat(systemPrompt, userPrompt);
                log.info("AI generate LLM responded: sessionId={}, elapsed={}ms, responseLength={}",
                        sessionId, System.currentTimeMillis() - t0, rawResponse != null ? rawResponse.length() : 0);

                plan = aiResponseParser.parseProjectPlan(rawResponse);
                modelUsed = aiChatService.getModelName();
            }

            log.info("AI generate plan ready: sessionId={}, sprints={}", sessionId,
                    plan.getSprints() != null ? plan.getSprints().size() : 0);

            String planJson = objectMapper.writeValueAsString(plan);
            int totalTaskCount = countAllTasks(plan);

            session.setStatus(AiGenerateSession.SessionStatus.READY);
            session.setPlanJson(planJson);
            session.setTotalTaskCount(totalTaskCount);
            session.setModelUsed(modelUsed);
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

    // ---------------------------------------------------------------------------
    // MOCK PLAN — dữ liệu giả để test FE mà không tốn quota API
    // Mô phỏng dự án "Website Quản Lý Công Việc" với 3 sprint, 15 task
    // ---------------------------------------------------------------------------
    private AiProjectPlan buildMockPlan(String requirement) {
        com.taskoryx.backend.ai.dto.response.AiProjectPlan plan =
                new com.taskoryx.backend.ai.dto.response.AiProjectPlan();
        plan.setProjectName("Website Quản Lý Công Việc");
        plan.setProjectDescription(
                "Hệ thống quản lý công việc nhóm theo mô hình Agile/Scrum, "
                + "hỗ trợ kanban board, sprint planning, báo cáo tiến độ. "
                + "(Mock từ yêu cầu: " + truncate(requirement, 80) + ")");
        plan.setProjectKey("QLCV");
        plan.setTotalDurationDays(42);

        // ── Sprint 1: Thiết lập nền tảng ──────────────────────────────────────
        com.taskoryx.backend.ai.dto.response.AiSprintItem sprint1 =
                new com.taskoryx.backend.ai.dto.response.AiSprintItem();
        sprint1.setName("Sprint 1 — Nền tảng & Auth");
        sprint1.setGoal("Thiết lập kiến trúc, database schema, xác thực người dùng");
        sprint1.setSprintNumber(1);
        sprint1.setDurationDays(14);
        sprint1.setStartOffsetDays(0);
        sprint1.setTasks(java.util.List.of(
                mockTask("Thiết kế database schema",
                        "Vẽ ERD, xác định bảng và quan hệ cho toàn bộ hệ thống",
                        com.taskoryx.backend.entity.Task.TaskPriority.HIGH, 3, 0,
                        java.util.List.of(
                                mockTask("ERD module User & Auth", null,
                                        com.taskoryx.backend.entity.Task.TaskPriority.HIGH, 1, 0, null),
                                mockTask("ERD module Task & Project", null,
                                        com.taskoryx.backend.entity.Task.TaskPriority.HIGH, 1, 1, null)
                        )),
                mockTask("Cài đặt Spring Boot & cấu hình project",
                        "Khởi tạo project, cấu hình Hibernate, HikariCP, Swagger",
                        com.taskoryx.backend.entity.Task.TaskPriority.HIGH, 2, 0, null),
                mockTask("Implement JWT Authentication",
                        "Login, refresh token, logout, JWT filter",
                        com.taskoryx.backend.entity.Task.TaskPriority.HIGH, 4, 2,
                        java.util.List.of(
                                mockTask("API đăng nhập / đăng ký", null,
                                        com.taskoryx.backend.entity.Task.TaskPriority.HIGH, 2, 2, null),
                                mockTask("JWT filter & SecurityConfig", null,
                                        com.taskoryx.backend.entity.Task.TaskPriority.MEDIUM, 1, 4, null)
                        )),
                mockTask("Thiết lập CI/CD pipeline",
                        "GitHub Actions: build, test, deploy lên staging",
                        com.taskoryx.backend.entity.Task.TaskPriority.MEDIUM, 2, 6, null)
        ));

        // ── Sprint 2: Core Features ────────────────────────────────────────────
        com.taskoryx.backend.ai.dto.response.AiSprintItem sprint2 =
                new com.taskoryx.backend.ai.dto.response.AiSprintItem();
        sprint2.setName("Sprint 2 — Quản lý Project & Task");
        sprint2.setGoal("CRUD project, board kanban, task hierarchy, drag & drop");
        sprint2.setSprintNumber(2);
        sprint2.setDurationDays(14);
        sprint2.setStartOffsetDays(14);
        sprint2.setTasks(java.util.List.of(
                mockTask("CRUD Project & thành viên",
                        "Tạo, sửa, xóa project; mời thành viên; phân quyền",
                        com.taskoryx.backend.entity.Task.TaskPriority.HIGH, 4, 0,
                        java.util.List.of(
                                mockTask("API tạo/sửa/xóa project", null,
                                        com.taskoryx.backend.entity.Task.TaskPriority.HIGH, 2, 0, null),
                                mockTask("Quản lý thành viên & roles", null,
                                        com.taskoryx.backend.entity.Task.TaskPriority.MEDIUM, 2, 2, null)
                        )),
                mockTask("Kanban Board & Columns",
                        "Tạo board, quản lý cột, mapping status",
                        com.taskoryx.backend.entity.Task.TaskPriority.HIGH, 3, 4, null),
                mockTask("CRUD Task & Sub-task",
                        "Tạo task, phân cấp tối đa 3 cấp, assign, label, dueDate",
                        com.taskoryx.backend.entity.Task.TaskPriority.HIGH, 5, 4,
                        java.util.List.of(
                                mockTask("API tạo/cập nhật task", null,
                                        com.taskoryx.backend.entity.Task.TaskPriority.HIGH, 2, 4, null),
                                mockTask("Drag & drop — PATCH /tasks/{id}/move", null,
                                        com.taskoryx.backend.entity.Task.TaskPriority.HIGH, 2, 6, null)
                        )),
                mockTask("Comment & @Mention",
                        "Bình luận, reply lồng nhau, @mention gửi notification",
                        com.taskoryx.backend.entity.Task.TaskPriority.MEDIUM, 3, 9, null)
        ));

        // ── Sprint 3: Advanced & Polish ────────────────────────────────────────
        com.taskoryx.backend.ai.dto.response.AiSprintItem sprint3 =
                new com.taskoryx.backend.ai.dto.response.AiSprintItem();
        sprint3.setName("Sprint 3 — Nâng cao & Hoàn thiện");
        sprint3.setGoal("Sprint management, thông báo realtime, báo cáo, time tracking");
        sprint3.setSprintNumber(3);
        sprint3.setDurationDays(14);
        sprint3.setStartOffsetDays(28);
        sprint3.setTasks(java.util.List.of(
                mockTask("Sprint Planning & Scrum Board",
                        "Tạo sprint, thêm task vào sprint, start/complete sprint",
                        com.taskoryx.backend.entity.Task.TaskPriority.HIGH, 4, 0, null),
                mockTask("WebSocket Notifications",
                        "Thông báo realtime: assign task, @mention, due date reminder",
                        com.taskoryx.backend.entity.Task.TaskPriority.HIGH, 3, 4,
                        java.util.List.of(
                                mockTask("Cấu hình STOMP WebSocket", null,
                                        com.taskoryx.backend.entity.Task.TaskPriority.MEDIUM, 1, 4, null),
                                mockTask("Push notification khi assign & mention", null,
                                        com.taskoryx.backend.entity.Task.TaskPriority.MEDIUM, 1, 5, null)
                        )),
                mockTask("Time Tracking",
                        "Log giờ làm, thống kê theo ngày/tuần/tháng, export Excel",
                        com.taskoryx.backend.entity.Task.TaskPriority.MEDIUM, 3, 7, null),
                mockTask("Dashboard & Báo cáo",
                        "Dashboard cá nhân, burndown chart, performance score",
                        com.taskoryx.backend.entity.Task.TaskPriority.LOW, 4, 10, null)
        ));

        plan.setSprints(java.util.List.of(sprint1, sprint2, sprint3));
        return plan;
    }

    private com.taskoryx.backend.ai.dto.response.AiTaskItem mockTask(
            String title, String description,
            com.taskoryx.backend.entity.Task.TaskPriority priority,
            int durationDays, int startOffsetDays,
            java.util.List<com.taskoryx.backend.ai.dto.response.AiTaskItem> subTasks) {
        com.taskoryx.backend.ai.dto.response.AiTaskItem item =
                new com.taskoryx.backend.ai.dto.response.AiTaskItem();
        item.setTitle(title);
        item.setDescription(description);
        item.setPriority(priority);
        item.setDurationDays(durationDays);
        item.setStartOffsetDays(startOffsetDays);
        item.setSubTasks(subTasks != null ? subTasks : java.util.List.of());
        return item;
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
