package com.taskoryx.backend.dto.response.template;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationConfigDto {
    @Builder.Default
    private Integer onTimeWeight = 40;
    @Builder.Default
    private Integer completionWeight = 30;
    @Builder.Default
    private Integer timeAccuracyWeight = 20;
    @Builder.Default
    private Integer engagementWeight = 10;
}
