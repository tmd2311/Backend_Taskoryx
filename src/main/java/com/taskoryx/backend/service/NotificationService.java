package com.taskoryx.backend.service;

import com.taskoryx.backend.dto.response.notification.NotificationResponse;
import com.taskoryx.backend.entity.*;
import com.taskoryx.backend.exception.ResourceNotFoundException;
import com.taskoryx.backend.repository.NotificationRepository;
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

    @Async
    @Transactional
    public void notifyTaskAssigned(Task task, User assignee, User assigner) {
        try {
            Notification notification = Notification.builder()
                    .user(assignee)
                    .type(Notification.NotificationType.TASK_ASSIGNED)
                    .title("Bạn được giao task mới")
                    .message(String.format("%s đã giao task '%s' cho bạn trong dự án '%s'",
                            assigner.getFullName(), task.getTitle(), task.getProject().getName()))
                    .relatedType(Notification.RelatedType.TASK)
                    .relatedId(task.getId())
                    .build();
            notificationRepository.save(notification);
        } catch (Exception e) {
            log.error("Failed to create task assigned notification", e);
        }
    }

    @Async
    @Transactional
    public void notifyTaskCommented(Task task, User commenter, Comment comment) {
        try {
            // Notify task assignee
            if (task.getAssignee() != null && !task.getAssignee().getId().equals(commenter.getId())) {
                createCommentNotification(task, commenter, task.getAssignee());
            }
            // Notify reporter
            if (!task.getReporter().getId().equals(commenter.getId())) {
                createCommentNotification(task, commenter, task.getReporter());
            }
        } catch (Exception e) {
            log.error("Failed to create comment notification", e);
        }
    }

    @Async
    @Transactional
    public void notifyMention(Task task, Comment comment, User mentioner, User mentioned) {
        try {
            Notification notification = Notification.builder()
                    .user(mentioned)
                    .type(Notification.NotificationType.MENTION)
                    .title("Bạn được nhắc đến")
                    .message(String.format("%s đã nhắc đến bạn trong task '%s'",
                            mentioner.getFullName(), task.getTitle()))
                    .relatedType(Notification.RelatedType.COMMENT)
                    .relatedId(comment.getId())
                    .build();
            notificationRepository.save(notification);
        } catch (Exception e) {
            log.error("Failed to create mention notification", e);
        }
    }

    private void createCommentNotification(Task task, User commenter, User recipient) {
        Notification notification = Notification.builder()
                .user(recipient)
                .type(Notification.NotificationType.TASK_COMMENTED)
                .title("Bình luận mới trong task")
                .message(String.format("%s đã bình luận trong task '%s'",
                        commenter.getFullName(), task.getTitle()))
                .relatedType(Notification.RelatedType.TASK)
                .relatedId(task.getId())
                .build();
        notificationRepository.save(notification);
    }
}
