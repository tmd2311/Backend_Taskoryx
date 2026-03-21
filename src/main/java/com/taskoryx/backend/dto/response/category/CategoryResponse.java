package com.taskoryx.backend.dto.response.category;

import com.taskoryx.backend.entity.IssueCategory;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CategoryResponse {

    private UUID id;
    private UUID projectId;
    private String name;
    private UUID defaultAssigneeId;
    private String defaultAssigneeName;
    private int taskCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CategoryResponse from(IssueCategory c) {
        return CategoryResponse.builder()
                .id(c.getId())
                .projectId(c.getProject().getId())
                .name(c.getName())
                .defaultAssigneeId(c.getDefaultAssignee() != null ? c.getDefaultAssignee().getId() : null)
                .defaultAssigneeName(c.getDefaultAssignee() != null ? c.getDefaultAssignee().getFullName() : null)
                .taskCount(c.getTasks().size())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
