package com.taskoryx.backend.dto.response.project;

import com.taskoryx.backend.entity.ProjectMember;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMemberResponse {

    private UUID id;
    private UUID userId;
    private String username;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String role;           // system role name (SUPER_ADMIN, PROJECT_MANAGER, ...)
    private String roleDescription;
    private LocalDateTime joinedAt;

    public static ProjectMemberResponse from(ProjectMember member) {
        // Lấy system role từ UserRole (user chỉ có 1 role)
        String roleName = member.getUser().getUserRoles().stream()
                .map(ur -> ur.getRole().getName())
                .findFirst()
                .orElse(member.getRole());
        String roleDesc = member.getUser().getUserRoles().stream()
                .map(ur -> ur.getRole().getDescription())
                .findFirst()
                .orElse(null);

        return ProjectMemberResponse.builder()
                .id(member.getId())
                .userId(member.getUser().getId())
                .username(member.getUser().getUsername())
                .email(member.getUser().getEmail())
                .fullName(member.getUser().getFullName())
                .avatarUrl(member.getUser().getAvatarUrl())
                .role(roleName)
                .roleDescription(roleDesc)
                .joinedAt(member.getJoinedAt())
                .build();
    }
}
