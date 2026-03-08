package com.taskoryx.backend.service;

import com.taskoryx.backend.dto.response.notification.NotificationResponse;
import com.taskoryx.backend.entity.*;
import com.taskoryx.backend.exception.ResourceNotFoundException;
import com.taskoryx.backend.repository.NotificationRepository;
import com.taskoryx.backend.repository.UserRepository;
import com.taskoryx.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(UserPrincipal principal, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(principal.getId(), pageable)
                .map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UserPrincipal principal) {
        return notificationRepository.countByUserIdAndIsReadFalse(principal.getId());
    }

    @Transactional
    public NotificationResponse markAsRead(UUID notificationId, UserPrincipal principal) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));
        if (!notification.getUser().getId().equals(principal.getId())) {
            throw new ResourceNotFoundException("Notification", "id", notificationId);
        }
        notification.markAsRead();
        return NotificationResponse.from(notificationRepository.save(notification));
    }

    @Transactional
    public int markAllAsRead(UserPrincipal principal) {
        return notificationRepository.markAllAsReadByUserId(principal.getId());
    }

    // ========== INTERNAL NOTIFICATION CREATORS ==========
    // Nhận primitive values để tránh LazyInitializationException khi @Async chạy thread khác

    @Async
    @Transactional
    public void notifyTaskAssigned(UUID assigneeId, String assignerName,
                                   UUID taskId, String taskTitle, String projectName) {
        try {
            User assignee = userRepository.findById(assigneeId).orElse(null);
            if (assignee == null) return;

            Notification notification = Notification.builder()
                    .user(assignee)
                    .type(Notification.NotificationType.TASK_ASSIGNED)
                    .title("Bạn được giao task mới")
                    .message(String.format("%s đã giao task '%s' cho bạn trong dự án '%s'",
                            assignerName, taskTitle, projectName))
                    .relatedType(Notification.RelatedType.TASK)
                    .relatedId(taskId)
                    .build();
            notificationRepository.save(notification);
        } catch (Exception e) {
            log.error("Failed to create task assigned notification", e);
        }
    }

    @Async
    @Transactional
    public void notifyTaskCommented(UUID taskId, String taskTitle,
                                    String commenterName, UUID commenterId,
                                    UUID assigneeId, UUID reporterId) {
        try {
            // Notify assignee
            if (assigneeId != null && !assigneeId.equals(commenterId)) {
                userRepository.findById(assigneeId).ifPresent(assignee ->
                        saveCommentNotification(assignee, commenterName, taskTitle, taskId));
            }
            // Notify reporter
            if (reporterId != null && !reporterId.equals(commenterId)) {
                userRepository.findById(reporterId).ifPresent(reporter ->
                        saveCommentNotification(reporter, commenterName, taskTitle, taskId));
            }
        } catch (Exception e) {
            log.error("Failed to create comment notification", e);
        }
    }

    @Async
    @Transactional
    public void notifyMention(UUID mentionedId, String mentionerName,
                              String taskTitle, UUID commentId) {
        try {
            User mentioned = userRepository.findById(mentionedId).orElse(null);
            if (mentioned == null) return;

            Notification notification = Notification.builder()
                    .user(mentioned)
                    .type(Notification.NotificationType.MENTION)
                    .title("Bạn được nhắc đến")
                    .message(String.format("%s đã nhắc đến bạn trong task '%s'",
                            mentionerName, taskTitle))
                    .relatedType(Notification.RelatedType.COMMENT)
                    .relatedId(commentId)
                    .build();
            notificationRepository.save(notification);
        } catch (Exception e) {
            log.error("Failed to create mention notification", e);
        }
    }

    private void saveCommentNotification(User recipient, String commenterName,
                                         String taskTitle, UUID taskId) {
        Notification notification = Notification.builder()
                .user(recipient)
                .type(Notification.NotificationType.TASK_COMMENTED)
                .title("Bình luận mới trong task")
                .message(String.format("%s đã bình luận trong task '%s'", commenterName, taskTitle))
                .relatedType(Notification.RelatedType.TASK)
                .relatedId(taskId)
                .build();
        notificationRepository.save(notification);
    }
}
