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

import java.util.HashSet;
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
            throw new ForbiddenException("Bạn không có quyền quản trị dự án này");
        }
        return project;
    }

    public void requirePermission(UUID projectId, UUID userId, String permission) {
        Project project = findProject(projectId);
        if (!hasPermission(project, userId, permission)) {
            throw new ForbiddenException("Bạn không có quyền thực hiện thao tác này trong dự án");
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

        if ("OWNER".equals(roleName)
                || "SUPER_ADMIN".equals(roleName)
                || "PROJECT_MANAGER".equals(roleName)) {
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

    /**
     * Tính tập quyền thực tế của một member trong project.
     *
     * Mọi member đều có BASIC (xem/sửa task, log time, comment, board...).
     * Các quyền nâng cao (xóa task, sprint, báo cáo...) chỉ có nếu được khai báo
     * rõ ràng trong custom role lưu ở DB.
     *
     * OWNER / SUPER_ADMIN / PROJECT_MANAGER đã được xử lý trước ở checkAccess (return true).
     */
    private Set<String> resolvePermissions(UUID projectId, String roleName) {
        Set<String> granted = new HashSet<>(ProjectPermission.BASIC);

        // Đọc quyền nâng cao được cấp thêm từ custom role trong DB
        projectRoleRepository.findByProjectIdAndName(projectId, roleName)
                .map(ProjectRole::getPermissionList)
                .ifPresent(extras -> extras.stream()
                        .filter(ProjectPermission.ADVANCED::contains)
                        .forEach(granted::add));

        return granted;
    }
}
