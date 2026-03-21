package com.taskoryx.backend.service;

import com.taskoryx.backend.dto.response.activity.ActivityLogResponse;
import com.taskoryx.backend.entity.ActivityLog;
import com.taskoryx.backend.entity.Project;
import com.taskoryx.backend.entity.User;
import com.taskoryx.backend.repository.ActivityLogRepository;
import com.taskoryx.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final ProjectService projectService;

    @Transactional(readOnly = true)
    public Page<ActivityLogResponse> getProjectActivityFeed(UUID projectId, int page, int size,
                                                             UserPrincipal principal) {
        // Kiểm tra quyền truy cập project
        projectService.findProjectWithAccess(projectId, principal.getId());

        PageRequest pageable = PageRequest.of(page, size);
        return activityLogRepository.findByProjectIdOrderByCreatedAtDesc(projectId, pageable)
                .map(ActivityLogResponse::from);
    }

    @Transactional(readOnly = true)
    public List<ActivityLogResponse> getTaskActivityFeed(UUID taskId, UserPrincipal principal) {
        return activityLogRepository
                .findByEntityTypeAndEntityIdOrderByCreatedAtDesc(ActivityLog.EntityType.TASK, taskId)
                .stream()
                .map(ActivityLogResponse::from)
                .collect(Collectors.toList());
    }

    @Async
    public void logActivity(User user, Project project, ActivityLog.EntityType entityType,
                             UUID entityId, ActivityLog.Action action,
                             String oldValue, String newValue) {
        try {
            ActivityLog activityLog = ActivityLog.builder()
                    .user(user)
                    .project(project)
                    .entityType(entityType)
                    .entityId(entityId)
                    .action(action)
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .build();
            activityLogRepository.save(activityLog);
        } catch (Exception e) {
            log.error("Lỗi khi ghi activity log: {}", e.getMessage(), e);
        }
    }
}
