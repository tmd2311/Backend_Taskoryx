package com.taskoryx.backend.dto.request.webhook;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateWebhookRequest {
    @NotBlank
    @Size(max = 255)
    private String name;

    @NotBlank
    @Size(max = 500)
    private String url;

    private String secretToken;

    @NotEmpty
    private List<String> events; // list of WebhookEvent names
}
