package com.taskoryx.backend.service;

import com.taskoryx.backend.config.AppProperties;
import com.taskoryx.backend.dto.request.user.ChangePasswordRequest;
import com.taskoryx.backend.dto.request.user.UpdateProfileRequest;
import com.taskoryx.backend.dto.response.user.UserResponse;
import com.taskoryx.backend.entity.User;
import com.taskoryx.backend.exception.BadRequestException;
import com.taskoryx.backend.exception.ResourceNotFoundException;
import com.taskoryx.backend.repository.UserRepository;
import com.taskoryx.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private static final List<String> ALLOWED_IMAGE_TYPES = List.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024; // 5MB

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;
    private final StorageService storageService;

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateProfile(UserPrincipal principal, UpdateProfileRequest request) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());
        if (request.getTimezone() != null) user.setTimezone(request.getTimezone());
        if (request.getLanguage() != null) user.setLanguage(request.getLanguage());

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void changePassword(UserPrincipal principal, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Mật khẩu xác nhận không khớp");
        }
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Mật khẩu hiện tại không đúng");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> searchUsers(String keyword, Pageable pageable) {
        return userRepository.searchActiveUsers(keyword, pageable)
                .map(UserResponse::from);
    }

    @Transactional
    public UserResponse uploadAvatar(UserPrincipal principal, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File không được để trống");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new BadRequestException("Ảnh không được quá 5MB");
        }
        if (!ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            throw new BadRequestException("Chỉ chấp nhận ảnh định dạng JPEG, PNG, GIF, WEBP");
        }

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        // Xóa avatar cũ nếu là file do hệ thống quản lý (local hoặc S3)
        deleteOldAvatar(user.getAvatarUrl());

        String ext = getExtension(file.getContentType());
        String relativePath = "avatars/" + principal.getId() + "_" + UUID.randomUUID() + ext;
        String avatarUrl = storageService.store(file, relativePath);

        user.setAvatarUrl(avatarUrl);
        return UserResponse.from(userRepository.save(user));
    }

    private void deleteOldAvatar(String avatarUrl) {
        if (avatarUrl == null) return;
        // Local: /api/files/avatars/xxx.jpg → relativePath = avatars/xxx.jpg
        if (avatarUrl.startsWith("/api/files/avatars/")) {
            storageService.delete(avatarUrl.substring("/api/files/".length()));
        }
        // S3: publicBaseUrl/avatars/xxx.jpg → relativePath = avatars/xxx.jpg
        String publicBase = appProperties.getS3().getPublicBaseUrl();
        if (publicBase != null && avatarUrl.startsWith(publicBase + "/")) {
            storageService.delete(avatarUrl.substring(publicBase.length() + 1));
        }
    }

    private String getExtension(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
