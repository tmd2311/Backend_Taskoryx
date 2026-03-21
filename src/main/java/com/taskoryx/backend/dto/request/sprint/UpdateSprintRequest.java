package com.taskoryx.backend.dto.request.sprint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSprintRequest {

    private String name;

    private String goal;

    private LocalDate startDate;

    private LocalDate endDate;
}
