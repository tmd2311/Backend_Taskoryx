package com.taskoryx.backend.dto.response.activity;

import com.taskoryx.backend.entity.ActivityLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogResponse {

    private UUID id;
    private UUID userId;
    private String userName;
    private String userAvatar;
    private UUID projectId;
    private String projectName;
    private String entityType;
    private UUID entityId;
    private String action;
    private String description;
    private String oldValue;
    private String newValue;
    private LocalDateTime createdAt;

    public static ActivityLogResponse from(ActivityLog log) {
        return ActivityLogResponse.builder()
                .id(log.getId())
                .userId(log.getUser() != null ? log.getUser().getId() : null)
                .userName(log.getUser() != null ? log.getUser().getFullName() : null)
                .userAvatar(log.getUser() != null ? log.getUser().getAvatarUrl() : null)
                .projectId(log.getProject() != null ? log.getProject().getId() : null)
                .projectName(log.getProject() != null ? log.getProject().getName() : null)
                .entityType(log.getEntityType() != null ? log.getEntityType().name() : null)
                .entityId(log.getEntityId())
                .action(log.getAction() != null ? log.getAction().name() : null)
                .description(log.getActionDescription())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
