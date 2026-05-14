package com.taskoryx.backend.dto.response.attachment;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class InlineUploadResponse {
    private String url;
    private UUID attachmentId;
}
