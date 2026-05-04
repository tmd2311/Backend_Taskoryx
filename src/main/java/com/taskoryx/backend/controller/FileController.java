package com.taskoryx.backend.controller;

import com.taskoryx.backend.config.AppProperties;
import com.taskoryx.backend.service.StorageService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;

/**
 * Serve file từ local storage.
 * Chỉ active khi KHÔNG chạy profile prod (prod dùng S3 public URL trực tiếp).
 *
 * GET /api/files/**  - Truy cập file theo đường dẫn tương đối
 */
@RestController
@RequestMapping("/files")
@Profile("!prod")
@RequiredArgsConstructor
@Hidden
public class FileController {

    private final StorageService storageService;
    private final AppProperties appProperties;

    @GetMapping("/**")
    public ResponseEntity<InputStreamResource> serveFile(HttpServletRequest request) throws IOException {
        // Lấy phần path sau /api/files/
        String relativePath = request.getRequestURI()
                .replaceFirst(".*/api/files/", "");

        InputStream inputStream;
        try {
            inputStream = storageService.load(relativePath);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = detectMediaType(relativePath);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(new InputStreamResource(inputStream));
    }

    private MediaType detectMediaType(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (lower.endsWith(".gif")) return MediaType.IMAGE_GIF;
        if (lower.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        if (lower.endsWith(".pdf")) return MediaType.APPLICATION_PDF;
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
