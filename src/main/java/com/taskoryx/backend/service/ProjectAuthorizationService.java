package com.taskoryx.backend.service;

import com.taskoryx.backend.entity.Project;
import com.taskoryx.backend.entity.ProjectPermission;
import com.taskoryx.backend.entity.ProjectRole;
import com.taskoryx.backend.exception.ForbiddenException;
import com.taskoryx.backend.exception.ResourceNotFoundException;
import com.taskoryx.backend.repository.ProjectMemberRepository;
import com.taskoryx.backend.repository.ProjectRepository;
import com.taskoryx.backend.repository.ProjectRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectAuthorizationService {

    private enum AccessMode {
        VIEW,
        ADMIN
    }

    private static final Set<String> VIEW_PERMISSIONS = Set.of(
            ProjectPermission.TASK_VIEW,
            ProjectPermission.BOARD_VIEW,
            ProjectPermission.REPORT_VIEW,
            ProjectPermission.TIME_TRACKING_VIEW
    );

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRoleRepository projectRoleRepository;

    public Project requireProjectAccess(UUID projectId, UUID userId) {
        Project project = findProject(projectId);
        if (!hasProjectAccess(project, userId)) {
            throw new ForbiddenException("Bạn không có quyền truy cập dự án này");
        }
        return project;
    }

    public Project requireProjectAdmin(UUID projectId, UUID userId) {
        Project project = findProject(projectId);
        if (!hasProjectAdmin(project, userId)) {
            throw new ForbiddenException("Bạn cần quyền ADMIN hoặc OWNER để thực hiện thao tác này");
        }
        return project;
    }

    public void requirePermission(UUID projectId, UUID userId, String permission) {
        Project project = findProject(projectId);
        if (!hasPermission(project, userId, permission)) {
            throw new ForbiddenException("Bạn không có quyền " + permission + " trong dự án này");
        }
    }

    public boolean hasPermission(UUID projectId, UUID userId, String permission) {
        return hasPermission(findProject(projectId), userId, permission);
    }

    private Project findProject(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
    }

    private boolean hasProjectAccess(Project project, UUID userId) {
        return checkAccess(project, userId, AccessMode.VIEW, null);
    }

    private boolean hasProjectAdmin(Project project, UUID userId) {
        return checkAccess(project, userId, AccessMode.ADMIN, null);
    }

    private boolean hasPermission(Project project, UUID userId, String permission) {
        return checkAccess(project, userId, AccessMode.VIEW, permission);
    }

    private boolean checkAccess(Project project, UUID userId, AccessMode mode, String permission) {
        if (project.getOwner().getId().equals(userId)) {
            return true;
        }

        String roleName = projectMemberRepository.findRoleByProjectIdAndUserId(project.getId(), userId)
                .orElse(null);

        if (mode == AccessMode.VIEW && roleName == null) {
            return project.getIsPublic() && (permission == null || VIEW_PERMISSIONS.contains(permission));
        }

        if (roleName == null) {
            return false;
        }

        if ("OWNER".equals(roleName) || "ADMIN".equals(roleName)) {
            return true;
        }

        if (mode == AccessMode.ADMIN) {
            return false;
        }

        Set<String> grantedPermissions = resolvePermissions(project.getId(), roleName);
        if (permission == null) {
            return !grantedPermissions.isEmpty();
        }
        return grantedPermissions.contains(permission);
    }

    private Set<String> resolvePermissions(UUID projectId, String roleName) {
        return switch (roleName) {
            case "MEMBER" -> Set.of(
                    ProjectPermission.TASK_VIEW,
                    ProjectPermission.TASK_CREATE,
                    ProjectPermission.TASK_UPDATE,
                    ProjectPermission.TASK_ASSIGN,
                    ProjectPermission.COMMENT_CREATE,
                    ProjectPermission.ATTACHMENT_MANAGE,
                    ProjectPermission.TIME_TRACKING_VIEW,
                    ProjectPermission.TIME_TRACKING_MANAGE,
                    ProjectPermission.LABEL_MANAGE,
                    ProjectPermission.BOARD_VIEW
            );
            case "VIEWER" -> Set.of(
                    ProjectPermission.TASK_VIEW,
                    ProjectPermission.BOARD_VIEW,
                    ProjectPermission.TIME_TRACKING_VIEW
            );
            default -> projectRoleRepository.findByProjectIdAndName(projectId, roleName)
                    .map(ProjectRole::getPermissionList)
                    .map(List::copyOf)
                    .map(Set::copyOf)
                    .orElse(Set.of());
        };
    }
}
