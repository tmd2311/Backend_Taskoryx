package com.taskoryx.backend.dto.response.label;

import com.taskoryx.backend.entity.Label;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabelResponse {

    private UUID id;
    private String name;
    private String color;
    private String description;

    public static LabelResponse from(Label label) {
        return LabelResponse.builder()
                .id(label.getId())
                .name(label.getName())
                .color(label.getColor())
                .description(label.getDescription())
                .build();
    }
}
