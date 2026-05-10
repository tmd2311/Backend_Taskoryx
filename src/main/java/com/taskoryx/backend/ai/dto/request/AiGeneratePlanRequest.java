package com.taskoryx.backend.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiGeneratePlanRequest {

    @NotBlank(message = "Yêu cầu không được để trống")
    @Size(max = 2000, message = "Yêu cầu không được quá 2000 ký tự")
    private String requirement;

    private String language = "vi";
}
