package com.taskoryx.backend.service;

import com.taskoryx.backend.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
@Profile("!prod")
@RequiredArgsConstructor
@Slf4j
public class LocalStorageService implements StorageService {

    private final AppProperties appProperties;

    @Override
    public String store(MultipartFile file, String relativePath) throws IOException {
        Path targetPath = Paths.get(appProperties.getStorage().getUploadDir()).resolve(relativePath);
        Files.createDirectories(targetPath.getParent());
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        // URL nội bộ — controller sẽ serve file này
        return "/api/files/" + relativePath;
    }

    @Override
    public void delete(String relativePath) {
        Path filePath = Paths.get(appProperties.getStorage().getUploadDir()).resolve(relativePath);
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Không thể xóa file local: {}", filePath);
        }
    }

    @Override
    public InputStream load(String relativePath) throws IOException {
        Path filePath = Paths.get(appProperties.getStorage().getUploadDir()).resolve(relativePath);
        return Files.newInputStream(filePath);
    }
}
