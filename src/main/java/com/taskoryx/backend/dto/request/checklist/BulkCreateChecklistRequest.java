package com.taskoryx.backend.dto.request.checklist;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkCreateChecklistRequest {

    @NotEmpty(message = "Danh sách checklist không được để trống")
    private List<@jakarta.validation.constraints.NotBlank String> items;
}
