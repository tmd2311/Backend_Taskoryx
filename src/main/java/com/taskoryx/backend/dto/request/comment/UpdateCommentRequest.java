package com.taskoryx.backend.dto.request.comment;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateCommentRequest {

    @NotBlank(message = "Nội dung bình luận không được để trống")
    private String content;
}
