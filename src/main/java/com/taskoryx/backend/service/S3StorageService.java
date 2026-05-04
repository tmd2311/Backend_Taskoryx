package com.taskoryx.backend.service;

import com.taskoryx.backend.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;

@Service
@Profile("prod")
@Slf4j
public class S3StorageService implements StorageService {

    private final S3Client s3Client;
    private final AppProperties appProperties;

    public S3StorageService(AppProperties appProperties) {
        this.appProperties = appProperties;
        AppProperties.S3 s3Config = appProperties.getS3();
        this.s3Client = S3Client.builder()
                .region(Region.of(s3Config.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(s3Config.getAccessKey(), s3Config.getSecretKey())))
                .build();
    }

    @Override
    public String store(MultipartFile file, String relativePath) throws IOException {
        AppProperties.S3 s3Config = appProperties.getS3();
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(s3Config.getBucket())
                .key(relativePath)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();
        s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        return s3Config.getPublicBaseUrl() + "/" + relativePath;
    }

    @Override
    public void delete(String relativePath) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(appProperties.getS3().getBucket())
                    .key(relativePath)
                    .build());
        } catch (Exception e) {
            log.warn("Không thể xóa file S3: {}", relativePath, e);
        }
    }

    @Override
    public InputStream load(String relativePath) {
        return s3Client.getObject(GetObjectRequest.builder()
                .bucket(appProperties.getS3().getBucket())
                .key(relativePath)
                .build());
    }
}
