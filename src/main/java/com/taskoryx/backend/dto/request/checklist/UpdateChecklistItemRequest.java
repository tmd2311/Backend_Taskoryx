package com.taskoryx.backend.dto.request.checklist;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateChecklistItemRequest {

    @Size(max = 500, message = "Nội dung không được vượt quá 500 ký tự")
    private String content;

    private Boolean isChecked;

    private Integer position;
}
