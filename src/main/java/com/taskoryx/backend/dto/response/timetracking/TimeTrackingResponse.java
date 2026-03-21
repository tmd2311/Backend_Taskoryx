package com.taskoryx.backend.dto.response.timetracking;

import com.taskoryx.backend.entity.TimeTracking;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeTrackingResponse {

    private UUID id;
    private UUID taskId;
    private String taskKey;
    private String taskTitle;
    private UUID userId;
    private String userName;
    private String userAvatar;
    private BigDecimal hours;
    private String formattedHours;
    private String description;
    private LocalDate workDate;
    private LocalDateTime createdAt;

    public static TimeTrackingResponse from(TimeTracking entry) {
        return TimeTrackingResponse.builder()
                .id(entry.getId())
                .taskId(entry.getTask() != null ? entry.getTask().getId() : null)
                .taskKey(entry.getTask() != null ? entry.getTask().getTaskKey() : null)
                .taskTitle(entry.getTask() != null ? entry.getTask().getTitle() : null)
                .userId(entry.getUser() != null ? entry.getUser().getId() : null)
                .userName(entry.getUser() != null ? entry.getUser().getFullName() : null)
                .userAvatar(entry.getUser() != null ? entry.getUser().getAvatarUrl() : null)
                .hours(entry.getHours())
                .formattedHours(entry.getFormattedHours())
                .description(entry.getDescription())
                .workDate(entry.getWorkDate())
                .createdAt(entry.getCreatedAt())
                .build();
    }
}
