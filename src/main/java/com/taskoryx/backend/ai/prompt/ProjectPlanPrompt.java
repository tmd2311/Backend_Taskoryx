package com.taskoryx.backend.ai.prompt;

import org.springframework.stereotype.Component;

/**
 * Xây dựng system prompt và user prompt cho chức năng sinh kế hoạch dự án.
 * Tách riêng để dễ điều chỉnh prompt engineering mà không chạm vào service logic.
 */
@Component
public class ProjectPlanPrompt {

    public String buildSystemPrompt() {
        return """
                You are an expert project management AI assistant.
                Your task is to analyze a user's natural language requirement and generate a detailed project plan.

                STRICT RULES:
                1. Respond ONLY with valid JSON — no markdown, no explanation, no extra text.
                2. Follow exactly the JSON schema provided.
                3. Priority values MUST be one of: LOW, MEDIUM, HIGH, URGENT.
                4. project_key must be 2-10 uppercase letters or digits (e.g., "PROJ", "BUILD3M").
                5. Tasks should be ordered chronologically.
                6. sub_tasks depth is limited to 1 level (no nested sub_tasks inside sub_tasks).
                7. duration_days and start_offset_days must be non-negative integers.
                """;
    }

    public String buildUserPrompt(String requirement, String language) {
        String langInstruction = "vi".equals(language)
                ? "Use Vietnamese for project_name, project_description, task title and description."
                : "Use English for all text fields.";

        return """
                Requirement: %s

                %s

                Return JSON matching this schema exactly:
                {
                  "project_name": "string",
                  "project_description": "string",
                  "project_key": "string (2-10 uppercase letters/digits)",
                  "total_duration_days": number,
                  "tasks": [
                    {
                      "title": "string",
                      "description": "string",
                      "priority": "LOW|MEDIUM|HIGH|URGENT",
                      "duration_days": number,
                      "start_offset_days": number,
                      "sub_tasks": [
                        {
                          "title": "string",
                          "description": "string",
                          "priority": "LOW|MEDIUM|HIGH|URGENT",
                          "duration_days": number,
                          "start_offset_days": number,
                          "sub_tasks": []
                        }
                      ]
                    }
                  ]
                }
                """.formatted(requirement, langInstruction);
    }
}
