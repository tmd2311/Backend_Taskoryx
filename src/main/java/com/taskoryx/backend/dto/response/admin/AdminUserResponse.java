package com.taskoryx.backend.dto.response.admin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.taskoryx.backend.entity.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminUserResponse {
    private UUID id;
    private String username;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String phone;
    private Boolean isActive;
    private Boolean emailVerified;
    private Boolean mustChangePassword;
    private Set<RoleResponse> roles;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;

    // Chỉ có giá trị khi admin vừa tạo user, null ở các response khác
    private String temporaryPassword;

    public static AdminUserResponse from(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .phone(user.getPhone())
                .isActive(user.getIsActive())
                .emailVerified(user.getEmailVerified())
                .mustChangePassword(user.getMustChangePassword())
                .roles(user.getUserRoles().stream()
                        .map(ur -> RoleResponse.from(ur.getRole()))
                        .collect(Collectors.toSet()))
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
