package com.taskoryx.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskoryx.backend.dto.request.webhook.CreateWebhookRequest;
import com.taskoryx.backend.dto.request.webhook.UpdateWebhookRequest;
import com.taskoryx.backend.dto.response.webhook.WebhookResponse;
import com.taskoryx.backend.entity.Project;
import com.taskoryx.backend.entity.User;
import com.taskoryx.backend.entity.Webhook;
import com.taskoryx.backend.exception.ResourceNotFoundException;
import com.taskoryx.backend.repository.ProjectRepository;
import com.taskoryx.backend.repository.UserRepository;
import com.taskoryx.backend.repository.WebhookRepository;
import com.taskoryx.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final WebhookRepository webhookRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;
    private final ObjectMapper objectMapper;

    private static final okhttp3.MediaType JSON = okhttp3.MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build();

    @Transactional
    public WebhookResponse createWebhook(UUID projectId, CreateWebhookRequest request, UserPrincipal principal) {
        projectService.findProjectWithAccess(projectId, principal.getId());

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
        User creator = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        String eventsStr = String.join(",", request.getEvents());

        Webhook webhook = Webhook.builder()
                .project(project)
                .createdBy(creator)
                .name(request.getName())
                .url(request.getUrl())
                .secretToken(request.getSecretToken())
                .events(eventsStr)
                .isActive(true)
                .build();

        return WebhookResponse.from(webhookRepository.save(webhook));
    }

    @Transactional(readOnly = true)
    public List<WebhookResponse> getProjectWebhooks(UUID projectId, UserPrincipal principal) {
        projectService.findProjectWithAccess(projectId, principal.getId());
        return webhookRepository.findByProjectId(projectId).stream()
                .map(WebhookResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public WebhookResponse updateWebhook(UUID webhookId, UpdateWebhookRequest request, UserPrincipal principal) {
        Webhook webhook = webhookRepository.findById(webhookId)
                .orElseThrow(() -> new ResourceNotFoundException("Webhook", "id", webhookId));
        projectService.findProjectWithAccess(webhook.getProject().getId(), principal.getId());

        if (request.getName() != null) webhook.setName(request.getName());
        if (request.getUrl() != null) webhook.setUrl(request.getUrl());
        if (request.getSecretToken() != null) webhook.setSecretToken(request.getSecretToken());
        if (request.getIsActive() != null) webhook.setActive(request.getIsActive());
        if (request.getEvents() != null) webhook.setEvents(String.join(",", request.getEvents()));

        return WebhookResponse.from(webhookRepository.save(webhook));
    }

    @Transactional
    public void deleteWebhook(UUID webhookId, UserPrincipal principal) {
        Webhook webhook = webhookRepository.findById(webhookId)
                .orElseThrow(() -> new ResourceNotFoundException("Webhook", "id", webhookId));
        projectService.findProjectWithAccess(webhook.getProject().getId(), principal.getId());
        webhookRepository.delete(webhook);
    }

    @Async
    @Transactional
    public void triggerWebhooks(UUID projectId, String eventType, Object payload) {
        List<Webhook> webhooks = webhookRepository.findActiveWebhooksForEvent(projectId, eventType);
        for (Webhook webhook : webhooks) {
            deliverWebhook(webhook, eventType, payload);
        }
    }

    private void deliverWebhook(Webhook webhook, String eventType, Object payload) {
        try {
            Map<String, Object> body = Map.of(
                "event", eventType,
                "timestamp", LocalDateTime.now().toString(),
                "projectId", webhook.getProject().getId().toString(),
                "payload", payload
            );
            String jsonBody = objectMapper.writeValueAsString(body);

            Request.Builder requestBuilder = new Request.Builder()
                    .url(webhook.getUrl())
                    .post(RequestBody.create(jsonBody, JSON))
                    .header("Content-Type", "application/json")
                    .header("X-Taskoryx-Event", eventType)
                    .header("X-Taskoryx-Delivery", UUID.randomUUID().toString());

            if (webhook.getSecretToken() != null && !webhook.getSecretToken().isBlank()) {
                requestBuilder.header("X-Taskoryx-Signature", "sha256=" + webhook.getSecretToken());
            }

            try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
                webhook.setLastTriggeredAt(LocalDateTime.now());
                if (response.isSuccessful()) {
                    webhook.setSuccessCount(webhook.getSuccessCount() + 1);
                } else {
                    webhook.setFailureCount(webhook.getFailureCount() + 1);
                    log.warn("Webhook delivery failed for {}: HTTP {}", webhook.getUrl(), response.code());
                }
                webhookRepository.save(webhook);
            }
        } catch (Exception e) {
            log.error("Failed to deliver webhook to {}", webhook.getUrl(), e);
            webhook.setFailureCount(webhook.getFailureCount() + 1);
            webhook.setLastTriggeredAt(LocalDateTime.now());
            webhookRepository.save(webhook);
        }
    }

    @Transactional
    public WebhookResponse testWebhook(UUID webhookId, UserPrincipal principal) {
        Webhook webhook = webhookRepository.findById(webhookId)
                .orElseThrow(() -> new ResourceNotFoundException("Webhook", "id", webhookId));
        projectService.findProjectWithAccess(webhook.getProject().getId(), principal.getId());
        deliverWebhook(webhook, "PING", Map.of("message", "Test delivery from Taskoryx"));
        return WebhookResponse.from(webhookRepository.findById(webhookId).orElseThrow());
    }
}
