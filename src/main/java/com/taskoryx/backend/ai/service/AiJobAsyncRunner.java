package com.taskoryx.backend.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskoryx.backend.ai.dto.response.AiExecuteResult;
import com.taskoryx.backend.ai.dto.response.AiProjectPlan;
import com.taskoryx.backend.ai.skill.AiPlanExecutor;
import com.taskoryx.backend.dto.response.notification.NotificationResponse;
import com.taskoryx.backend.entity.AiJob;
import com.taskoryx.backend.entity.Notification;
import com.taskoryx.backend.repository.AiJobRepository;
import com.taskoryx.backend.repository.NotificationRepository;
import com.taskoryx.backend.security.UserPrincipal;
import com.taskoryx.backend.service.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.Semaphore;

/**
 * Bean riêng để chạy AI confirm job bất đồng bộ — tách khỏi AiJobService
 * để Spring @Async AOP proxy hoạt động đúng.
 * Semaphore giới hạn toàn server: chỉ 1 confirm job chạy cùng lúc.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiJobAsyncRunner {

    private static final Semaphore JOB_SEMAPHORE = new Semaphore(1);

    private final AiJobRepository aiJobRepository;
    private final AiPlanExecutor aiPlanExecutor;
    private final NotificationRepository notificationRepository;
    private final WebSocketNotificationService webSocketNotificationService;
    private final ObjectMapper objectMapper;

    @Async
    public void run(UUID jobId, UserPrincipal principal) {
        boolean acquired = false;
        try {
            acquired = JOB_SEMAPHORE.tryAcquire();
            if (!acquired) {
                log.info("AI job queued (slot busy): jobId={}", jobId);
                JOB_SEMAPHORE.acquire();
                acquired = true;
            }
            doExecute(jobId, principal);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            markFailed(jobId, "Job bị gián đoạn");
        } finally {
            if (acquired) JOB_SEMAPHORE.release();
        }
    }

    private void doExecute(UUID jobId, UserPrincipal principal) {
        AiJob job = aiJobRepository.findById(jobId).orElse(null);
        if (job == null) return;

        job.setStatus(AiJob.JobStatus.RUNNING);
        job.setStartedAt(LocalDateTime.now());
        aiJobRepository.save(job);
        log.info("AI job started: jobId={}", jobId);

        try {
            AiProjectPlan plan = objectMapper.readValue(job.getPlanJson(), AiProjectPlan.class);
            log.info("AI job executing plan: jobId={}, project='{}', sprints={}", jobId,
                    plan.getProjectName(), plan.getSprints() != null ? plan.getSprints().size() : 0);

            long t0 = System.currentTimeMillis();
            AiExecuteResult result = aiPlanExecutor.execute(plan, job.getTargetProjectId(), principal);
            log.info("AI job plan executed: jobId={}, elapsed={}ms, sprints={}, tasks={}, subTasks={}",
                    jobId, System.currentTimeMillis() - t0,
                    result.getSprintsCreated(), result.getTasksCreated(), result.getSubTasksCreated());

            job.setStatus(AiJob.JobStatus.DONE);
            job.setFinishedAt(LocalDateTime.now());
            job.setResultProjectId(result.getProjectId());
            job.setResultProjectKey(result.getProjectKey());
            job.setResultProjectName(result.getProjectName());
            job.setTasksCreated(result.getTasksCreated());
            job.setSubTasksCreated(result.getSubTasksCreated());
            job.setSprintsCreated(result.getSprintsCreated());
            aiJobRepository.save(job);

            log.info("AI job done: jobId={}, projectId={}", jobId, result.getProjectId());
            sendSuccessNotification(job);

        } catch (Exception e) {
            log.error("AI job failed: jobId={}", jobId, e);
            markFailed(jobId, e.getMessage());
            // Re-load để lấy errorMessage đã set rồi gửi notification
            aiJobRepository.findById(jobId).ifPresent(this::sendFailureNotification);
        }
    }

    private void markFailed(UUID jobId, String errorMessage) {
        aiJobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(AiJob.JobStatus.FAILED);
            job.setFinishedAt(LocalDateTime.now());
            job.setErrorMessage(errorMessage != null && errorMessage.length() > 500
                    ? errorMessage.substring(0, 500) : errorMessage);
            aiJobRepository.save(job);
        });
    }

    private void sendSuccessNotification(AiJob job) {
        try {
            int total = (job.getTasksCreated() != null ? job.getTasksCreated() : 0)
                    + (job.getSubTasksCreated() != null ? job.getSubTasksCreated() : 0);
            String message = String.format("Dự án '%s' đã được tạo thành công với %d sprint và %d task.",
                    job.getResultProjectName(),
                    job.getSprintsCreated() != null ? job.getSprintsCreated() : 0,
                    total);

            Notification notification = Notification.builder()
                    .user(job.getUser())
                    .type(Notification.NotificationType.PROJECT_INVITE)
                    .title("Tạo dự án AI thành công")
                    .message(message)
                    .relatedType(Notification.RelatedType.PROJECT)
                    .relatedId(job.getResultProjectId())
                    .build();
            notification = notificationRepository.save(notification);

            webSocketNotificationService.sendNotificationToUser(
                    job.getUser().getId(), NotificationResponse.from(notification));
        } catch (Exception e) {
            log.error("Failed to send success notification: jobId={}", job.getId(), e);
        }
    }

    private void sendFailureNotification(AiJob job) {
        try {
            Notification notification = Notification.builder()
                    .user(job.getUser())
                    .type(Notification.NotificationType.PROJECT_INVITE)
                    .title("Tạo dự án AI thất bại")
                    .message("Có lỗi xảy ra khi tạo dự án. Vui lòng thử lại.")
                    .relatedType(Notification.RelatedType.PROJECT)
                    .relatedId(null)
                    .build();
            notification = notificationRepository.save(notification);

            webSocketNotificationService.sendNotificationToUser(
                    job.getUser().getId(), NotificationResponse.from(notification));
        } catch (Exception e) {
            log.error("Failed to send failure notification: jobId={}", job.getId(), e);
        }
    }
}
