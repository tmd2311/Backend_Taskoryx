package com.taskoryx.backend.dto.response.webhook;

import com.taskoryx.backend.entity.Webhook;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookResponse {
    private UUID id;
    private UUID projectId;
    private String name;
    private String url;
    private boolean isActive;
    private List<String> events;
    private LocalDateTime lastTriggeredAt;
    private int successCount;
    private int failureCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static WebhookResponse from(Webhook webhook) {
        List<String> eventList = webhook.getEvents() != null
            ? Arrays.asList(webhook.getEvents().split(","))
            : List.of();
        return WebhookResponse.builder()
                .id(webhook.getId())
                .projectId(webhook.getProject().getId())
                .name(webhook.getName())
                .url(webhook.getUrl())
                .isActive(webhook.isActive())
                .events(eventList)
                .lastTriggeredAt(webhook.getLastTriggeredAt())
                .successCount(webhook.getSuccessCount())
                .failureCount(webhook.getFailureCount())
                .createdAt(webhook.getCreatedAt())
                .updatedAt(webhook.getUpdatedAt())
                .build();
    }
}
