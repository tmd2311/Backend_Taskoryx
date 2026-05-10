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
 * Google Gemini implementation.
 * Kích hoạt khi application.ai.provider=gemini (mặc định).
 *
 * API key cấu hình tại: application.ai.gemini.api-key
 * Model mặc định: gemini-2.0-flash
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "application.ai.provider", havingValue = "gemini", matchIfMissing = true)
@RequiredArgsConstructor
public class GeminiChatService implements AiChatService {

    // KEY_REF: AI_KEYS_001 — xem phiếu yêu cầu API_KEYS_REQUEST.md
    @Value("${application.ai.gemini.api-key:GEMINI_API_KEY_PLACEHOLDER}")
    private String apiKey;

    @Value("${application.ai.gemini.model:gemini-2.0-flash}")
    private String model;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    @Override
    public String chat(String systemPrompt, String userMessage) {
        String url = GEMINI_API_URL.formatted(model, apiKey);

        Map<String, Object> body = Map.of(
                "system_instruction", Map.of(
                        "parts", List.of(Map.of("text", systemPrompt))
                ),
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", userMessage)))
                ),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json"
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            String rawBody = objectMapper.writeValueAsString(body);
            HttpEntity<String> entity = new HttpEntity<>(rawBody, headers);
            String response = restTemplate.postForObject(url, entity, String.class);

            JsonNode root = objectMapper.readTree(response);
            return root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();
        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage());
            throw new BadRequestException("Không thể kết nối AI. Vui lòng thử lại sau.");
        }
    }

    @Override
    public String getModelName() {
        return "gemini/" + model;
    }
}
