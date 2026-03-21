package com.taskoryx.backend.dto.request.webhook;

import lombok.Data;

import java.util.List;

@Data
public class UpdateWebhookRequest {
    private String name;
    private String url;
    private String secretToken;
    private Boolean isActive;
    private List<String> events;
}
