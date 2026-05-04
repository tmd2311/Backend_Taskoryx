package com.taskoryx.backend.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

public interface StorageService {

    /**
     * Lưu file và trả về URL public để truy cập.
     *
     * @param file         file upload từ client
     * @param relativePath đường dẫn tương đối trong storage (vd: "tasks/uuid/file.png")
     * @return URL đầy đủ để trả về cho client
     */
    String store(MultipartFile file, String relativePath) throws IOException;

    /**
     * Xóa file theo đường dẫn tương đối đã dùng khi store.
     *
     * @param relativePath đường dẫn tương đối (vd: "tasks/uuid/file.png")
     */
    void delete(String relativePath);

    /**
     * Lấy InputStream của file (dùng để serve file từ local storage).
     * S3 implementation có thể ném UnsupportedOperationException vì S3 dùng public URL trực tiếp.
     */
    InputStream load(String relativePath) throws IOException;
}
