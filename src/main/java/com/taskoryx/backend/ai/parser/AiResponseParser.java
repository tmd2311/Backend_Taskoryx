package com.taskoryx.backend.ai.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskoryx.backend.ai.dto.response.AiProjectPlan;
import com.taskoryx.backend.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parse raw AI text output thành typed DTO.
 * Xử lý trường hợp AI trả thêm markdown code block (```json ... ```).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiResponseParser {

    private static final Pattern JSON_BLOCK = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```");

    private final ObjectMapper objectMapper;

    public AiProjectPlan parseProjectPlan(String rawResponse) {
        String json = extractJson(rawResponse);
        try {
            return objectMapper.readValue(json, AiProjectPlan.class);
        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", rawResponse);
            throw new BadRequestException("AI trả về dữ liệu không hợp lệ. Vui lòng thử lại.");
        }
    }

    private String extractJson(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("AI không trả về kết quả.");
        }
        Matcher matcher = JSON_BLOCK.matcher(raw);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return trimmed;
        }
        throw new BadRequestException("Không tìm thấy JSON hợp lệ trong phản hồi AI.");
    }
}
