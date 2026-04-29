package com.taskoryx.backend.service;

import com.taskoryx.backend.dto.request.role.CreateProjectRoleRequest;
import com.taskoryx.backend.dto.request.role.UpdateProjectRoleRequest;
import com.taskoryx.backend.dto.response.role.ProjectRoleResponse;
import com.taskoryx.backend.entity.Project;
import com.taskoryx.backend.entity.ProjectMember;
import com.taskoryx.backend.entity.ProjectPermission;
import com.taskoryx.backend.entity.ProjectRole;
import com.taskoryx.backend.exception.BadRequestException;
import com.taskoryx.backend.exception.ResourceNotFoundException;
import com.taskoryx.backend.repository.ProjectMemberRepository;
import com.taskoryx.backend.repository.ProjectRoleRepository;
import com.taskoryx.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectRoleService {

    // System roles — không thể dùng làm tên custom project role
    private static final Set<String> RESERVED_ROLE_NAMES = Set.of(
            "OWNER", "SUPER_ADMIN", "ADMIN", "PROJECT_MANAGER", "TEAM_LEAD", "MEMBER"
    );

    private final ProjectRoleRepository projectRoleRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectService projectService;
    private final ProjectAuthorizationService projectAuthorizationService;

    /**
     * Lấy danh sách custom roles của project.
     */
    @Transactional(readOnly = true)
    public List<ProjectRoleResponse> getRoles(UUID projectId, UserPrincipal principal) {
        projectAuthorizationService.requireProjectAccess(projectId, principal.getId());
        return projectRoleRepository.findByProjectIdOrderByNameAsc(projectId)
                .stream()
                .map(ProjectRoleResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Tạo custom role mới cho project (chỉ Owner/Admin).
     */
    @Transactional
    public ProjectRoleResponse createRole(UUID projectId, CreateProjectRoleRequest request,
                                           UserPrincipal principal) {
        projectAuthorizationService.requirePermission(projectId, principal.getId(), ProjectPermission.MEMBER_MANAGE);
        Project project = projectService.findProjectWithAccess(projectId, principal.getId());

        if (RESERVED_ROLE_NAMES.contains(request.getName().toUpperCase())) {
            throw new BadRequestException("'" + request.getName() + "' là tên role hệ thống, không thể dùng làm custom role");
        }
        if (projectRoleRepository.existsByProjectIdAndName(projectId, request.getName())) {
            throw new BadRequestException("Vai trò '" + request.getName() + "' đã tồn tại trong dự án");
        }

        validatePermissions(request.getPermissions());

        ProjectRole role = ProjectRole.builder()
                .project(project)
                .name(request.getName())
                .description(request.getDescription())
                .isDefault(request.isDefault())
                .build();
        role.setPermissionList(request.getPermissions());

        return ProjectRoleResponse.from(projectRoleRepository.save(role));
    }

    /**
     * Cập nhật custom role (chỉ Owner/Admin của project đó).
     */
    @Transactional
    public ProjectRoleResponse updateRole(UUID roleId, UpdateProjectRoleRequest request,
                                           UserPrincipal principal) {
        ProjectRole role = projectRoleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectRole", "id", roleId));

        UUID projectId = role.getProject().getId();
        projectAuthorizationService.requirePermission(projectId, principal.getId(), ProjectPermission.MEMBER_MANAGE);

        if (request.getName() != null && !request.getName().equals(role.getName())) {
            if (projectRoleRepository.existsByProjectIdAndName(projectId, request.getName())) {
                throw new BadRequestException("Vai trò '" + request.getName() + "' đã tồn tại trong dự án");
            }
            role.setName(request.getName());
        }
        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }
        if (request.getIsDefault() != null) {
            role.setDefault(request.getIsDefault());
        }
        if (request.getPermissions() != null) {
            validatePermissions(request.getPermissions());
            role.setPermissionList(request.getPermissions());
        }

        return ProjectRoleResponse.from(projectRoleRepository.save(role));
    }

    /**
     * Xoá custom role (chỉ Owner/Admin).
     */
    @Transactional
    public void deleteRole(UUID roleId, UserPrincipal principal) {
        ProjectRole role = projectRoleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectRole", "id", roleId));

        UUID projectId = role.getProject().getId();
        projectAuthorizationService.requirePermission(projectId, principal.getId(), ProjectPermission.MEMBER_MANAGE);

        projectRoleRepository.delete(role);
    }

    private void validatePermissions(List<String> permissions) {
        if (permissions == null || permissions.isEmpty()) return;
        List<String> invalid = permissions.stream()
                .filter(p -> !ProjectPermission.ALL.contains(p))
                .collect(Collectors.toList());
        if (!invalid.isEmpty()) {
            throw new BadRequestException("Quyền không hợp lệ: " + invalid);
        }
    }
}
