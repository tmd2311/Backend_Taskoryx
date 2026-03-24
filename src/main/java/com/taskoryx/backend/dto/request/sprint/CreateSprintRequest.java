package com.taskoryx.backend.dto.request.sprint;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSprintRequest {

    @NotBlank(message = "Tên sprint không được để trống")
    private String name;

    private String goal;

    private LocalDate startDate;

    private LocalDate endDate;

    /** Board SCRUM gắn với sprint này (optional) */
    private UUID boardId;
}
