package com.taskoryx.backend.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskoryx.backend.ai.service.AiChatService;
import com.taskoryx.backend.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * OpenAI ChatCompletion implementation.
 * Kích hoạt khi application.ai.provider=openai.
 *
 * API key cấu hình tại: application.ai.openai.api-key
 * Model mặc định: gpt-4o-mini
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "application.ai.provider", havingValue = "openai")
@RequiredArgsConstructor
public class OpenAiChatService implements AiChatService {

    // KEY_REF: AI_KEYS_002 — xem phiếu yêu cầu API_KEYS_REQUEST.md
    @Value("${application.ai.openai.api-key:OPENAI_API_KEY_PLACEHOLDER}")
    private String apiKey;

    @Value("${application.ai.openai.model:gpt-4o-mini}")
    private String model;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    @Override
    public String chat(String systemPrompt, String userMessage) {
        Map<String, Object> body = Map.of(
                "model", model,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        try {
            String rawBody = objectMapper.writeValueAsString(body);
            HttpEntity<String> entity = new HttpEntity<>(rawBody, headers);
            String response = restTemplate.postForObject(OPENAI_API_URL, entity, String.class);

            JsonNode root = objectMapper.readTree(response);
            return root.path("choices").get(0)
                    .path("message").path("content").asText();
        } catch (Exception e) {
            log.error("OpenAI API call failed: {}", e.getMessage());
            throw new BadRequestException("Không thể kết nối AI. Vui lòng thử lại sau.");
        }
    }

    @Override
    public String getModelName() {
        return "openai/" + model;
    }
}
