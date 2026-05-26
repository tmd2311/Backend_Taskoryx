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
    private String role;             // tên role kỹ thuật lưu trong project_members (VD: "PROJECT_MANAGER")
    private String roleDisplayName;  // tên hiển thị (VD: "Project Manager") — null nếu là custom role
    private String roleDescription;
    private LocalDateTime joinedAt;

    public static ProjectMemberResponse from(ProjectMember member) {
        String memberRole = member.getRole(); // role lưu trực tiếp trong project_members

        // Tìm displayName và description từ system role nếu tên khớp
        String displayName = member.getUser().getUserRoles().stream()
                .filter(ur -> ur.getRole() != null && memberRole != null
                        && memberRole.equals(ur.getRole().getName()))
                .map(ur -> ur.getRole().getDisplayName())
                .findFirst()
                .orElse(null);

        String roleDesc = member.getUser().getUserRoles().stream()
                .filter(ur -> ur.getRole() != null && memberRole != null
                        && memberRole.equals(ur.getRole().getName()))
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
                .role(memberRole)
                .roleDisplayName(displayName)
                .roleDescription(roleDesc)
                .joinedAt(member.getJoinedAt())
                .build();
    }
}
