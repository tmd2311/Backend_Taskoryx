package com.taskoryx.backend.ai.prompt;

import org.springframework.stereotype.Component;

@Component
public class ProjectPlanPrompt {

    public String buildSystemPrompt() {
        return """
                You are an expert Agile/Scrum project management AI assistant.
                Your task is to analyze a user's natural language requirement and generate a detailed Scrum project plan with sprints.

                STRICT RULES:
                1. Respond ONLY with valid JSON — no markdown, no explanation, no extra text.
                2. Follow exactly the JSON schema provided.
                3. Priority values MUST be one of: LOW, MEDIUM, HIGH, URGENT.
                4. project_key must be 2-10 uppercase letters or digits (e.g., "PROJ", "BUILD3M").
                5. Generate 2-6 sprints depending on project complexity and total_duration_days.
                6. Each sprint should be 7-21 days long (typical Scrum sprint length).
                7. Tasks must be distributed across sprints logically (earlier sprints handle setup/foundation, later sprints handle features/delivery).
                8. sub_tasks depth is limited to 1 level (no nested sub_tasks inside sub_tasks).
                9. Each task's sub_tasks are subtasks of that task within the same sprint.
                10. duration_days, start_offset_days, sprint_number must be non-negative integers.
                11. sprint start_offset_days is the number of days from project start to sprint start.
                12. Task start_offset_days is relative to the sprint's start_offset_days.
                13. Sprints must not overlap in time and must be ordered by sprint_number.
                """;
    }

    public String buildUserPrompt(String requirement, String language) {
        String langInstruction = "vi".equals(language)
                ? "Use Vietnamese for project_name, project_description, sprint name, sprint goal, task title and description."
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
                  "sprints": [
                    {
                      "name": "string (e.g. Sprint 1)",
                      "goal": "string (sprint goal/objective)",
                      "sprint_number": number (1, 2, 3...),
                      "duration_days": number (7-21),
                      "start_offset_days": number (days from project start),
                      "tasks": [
                        {
                          "title": "string",
                          "description": "string",
                          "priority": "LOW|MEDIUM|HIGH|URGENT",
                          "duration_days": number,
                          "start_offset_days": number (days from sprint start),
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
                  ]
                }
                """.formatted(requirement, langInstruction);
    }
}
