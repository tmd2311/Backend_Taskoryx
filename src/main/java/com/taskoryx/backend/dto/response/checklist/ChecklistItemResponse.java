package com.taskoryx.backend.dto.response.checklist;

import com.taskoryx.backend.entity.ChecklistItem;
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
public class ChecklistItemResponse {

    private UUID id;
    private UUID taskId;
    private String content;
    private boolean isChecked;
    private UUID checkedById;
    private String checkedByName;
    private LocalDateTime checkedAt;
    private int position;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ChecklistItemResponse from(ChecklistItem item) {
        return ChecklistItemResponse.builder()
                .id(item.getId())
                .taskId(item.getTask().getId())
                .content(item.getContent())
                .isChecked(item.isChecked())
                .checkedById(item.getCheckedBy() != null ? item.getCheckedBy().getId() : null)
                .checkedByName(item.getCheckedBy() != null ? item.getCheckedBy().getFullName() : null)
                .checkedAt(item.getCheckedAt())
                .position(item.getPosition())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
