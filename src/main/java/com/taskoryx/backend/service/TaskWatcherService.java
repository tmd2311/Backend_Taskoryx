package com.taskoryx.backend.service;

import com.taskoryx.backend.dto.response.watcher.WatcherResponse;
import com.taskoryx.backend.entity.*;
import com.taskoryx.backend.exception.BadRequestException;
import com.taskoryx.backend.exception.ResourceNotFoundException;
import com.taskoryx.backend.repository.*;
import com.taskoryx.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskWatcherService {

    private final TaskWatcherRepository watcherRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    @Transactional
    public WatcherResponse watchTask(UUID taskId, UserPrincipal principal) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        if (watcherRepository.existsByTaskIdAndUserId(taskId, principal.getId())) {
            throw new BadRequestException("Bạn đã theo dõi task này rồi");
        }

        User user = userRepository.findById(principal.getId()).orElseThrow();

        TaskWatcher watcher = TaskWatcher.builder()
                .task(task)
                .user(user)
                .build();

        return WatcherResponse.from(watcherRepository.save(watcher));
    }

    @Transactional
    public void unwatchTask(UUID taskId, UserPrincipal principal) {
        if (!watcherRepository.existsByTaskIdAndUserId(taskId, principal.getId())) {
            throw new BadRequestException("Bạn chưa theo dõi task này");
        }
        watcherRepository.deleteByTaskIdAndUserId(taskId, principal.getId());
    }

    @Transactional(readOnly = true)
    public List<WatcherResponse> getWatchers(UUID taskId, UserPrincipal principal) {
        taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));
        return watcherRepository.findByTaskId(taskId).stream()
                .map(WatcherResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean isWatching(UUID taskId, UserPrincipal principal) {
        return watcherRepository.existsByTaskIdAndUserId(taskId, principal.getId());
    }

    @Async
    @Transactional
    public void notifyWatchers(UUID taskId, UUID actorId, String title, String message) {
        try {
            List<TaskWatcher> watchers = watcherRepository.findByTaskId(taskId);
            for (TaskWatcher watcher : watchers) {
                if (watcher.getUser().getId().equals(actorId)) continue;
                Notification notification = Notification.builder()
                        .user(watcher.getUser())
                        .type(Notification.NotificationType.TASK_UPDATED)
                        .title(title)
                        .message(message)
                        .relatedType(Notification.RelatedType.TASK)
                        .relatedId(taskId)
                        .build();
                notificationRepository.save(notification);
            }
        } catch (Exception e) {
            log.error("Failed to notify watchers for task {}", taskId, e);
        }
    }
}
