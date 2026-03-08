package com.taskoryx.backend.service;

import com.taskoryx.backend.dto.request.project.AddMemberRequest;
import com.taskoryx.backend.dto.request.project.CreateProjectRequest;
import com.taskoryx.backend.dto.request.project.UpdateMemberRoleRequest;
import com.taskoryx.backend.dto.request.project.UpdateProjectRequest;
import com.taskoryx.backend.dto.response.project.ProjectMemberResponse;
import com.taskoryx.backend.dto.response.project.ProjectResponse;
import com.taskoryx.backend.entity.Board;
import com.taskoryx.backend.entity.BoardColumn;
import com.taskoryx.backend.entity.Project;
import com.taskoryx.backend.entity.ProjectMember;
import com.taskoryx.backend.entity.User;
import com.taskoryx.backend.exception.BadRequestException;
import com.taskoryx.backend.exception.ForbiddenException;
import com.taskoryx.backend.exception.ResourceNotFoundException;
import com.taskoryx.backend.repository.BoardColumnRepository;
import com.taskoryx.backend.repository.BoardRepository;
import com.taskoryx.backend.repository.ProjectMemberRepository;
import com.taskoryx.backend.repository.ProjectRepository;
import com.taskoryx.backend.repository.TaskRepository;
import com.taskoryx.backend.repository.UserRepository;
import com.taskoryx.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final BoardRepository boardRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final TaskRepository taskRepository;

    @Transactional(readOnly = true)
    public List<ProjectResponse> getMyProjects(UserPrincipal principal) {
        return projectRepository.findProjectsByUserId(principal.getId())
                .stream()
                .map(p -> {
                    ProjectResponse res = ProjectResponse.from(p);
                    projectMemberRepository.findRoleByProjectIdAndUserId(p.getId(), principal.getId())
                            .ifPresent(res::setCurrentUserRole);
                    return res;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public ProjectResponse createProject(UserPrincipal principal, CreateProjectRequest request) {
        if (projectRepository.existsByKey(request.getKey())) {
            throw new BadRequestException("Mã dự án '" + request.getKey() + "' đã tồn tại");
        }
        User owner = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .key(request.getKey())
                .owner(owner)
                .color(request.getColor() != null ? request.getColor() : "#1976d2")
                .icon(request.getIcon())
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
                .isArchived(false)
                .build();
        project = projectRepository.save(project);

        // Thêm owner vào project members
        ProjectMember ownerMember = ProjectMember.builder()
                .project(project)
                .user(owner)
                .role(ProjectMember.ProjectRole.OWNER)
                .build();
        projectMemberRepository.save(ownerMember);

        // Tạo board mặc định
        createDefaultBoard(project);

        ProjectResponse response = ProjectResponse.from(project);
        response.setCurrentUserRole(ProjectMember.ProjectRole.OWNER);
        return response;
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(UUID projectId, UserPrincipal principal) {
        Project project = findProjectWithAccess(projectId, principal.getId());
        ProjectResponse response = ProjectResponse.from(project);
        projectMemberRepository.findRoleByProjectIdAndUserId(projectId, principal.getId())
                .ifPresent(response::setCurrentUserRole);
        return response;
    }

    @Transactional
    public ProjectResponse updateProject(UUID projectId, UpdateProjectRequest request, UserPrincipal principal) {
        Project project = findProjectWithAccess(projectId, principal.getId());
        requireAdminOrOwner(projectId, principal.getId());

        if (request.getName() != null) project.setName(request.getName());
        if (request.getDescription() != null) project.setDescription(request.getDescription());
        if (request.getColor() != null) project.setColor(request.getColor());
        if (request.getIcon() != null) project.setIcon(request.getIcon());
        if (request.getIsPublic() != null) project.setIsPublic(request.getIsPublic());
        if (request.getIsArchived() != null) project.setIsArchived(request.getIsArchived());

        return ProjectResponse.from(projectRepository.save(project));
    }

    @Transactional
    public void deleteProject(UUID projectId, UserPrincipal principal) {
        Project project = findProjectWithAccess(projectId, principal.getId());
        if (!project.getOwner().getId().equals(principal.getId())) {
            throw new ForbiddenException("Chỉ chủ sở hữu mới có thể xóa dự án");
        }

        long taskCount = taskRepository.countByProjectId(projectId);
        if (taskCount > 0) {
            throw new BadRequestException(
                String.format("Không thể xóa dự án vì còn %d task đang tồn tại. " +
                              "Vui lòng xóa hết task trước khi xóa dự án.", taskCount));
        }

        projectRepository.delete(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> getMembers(UUID projectId, UserPrincipal principal) {
        findProjectWithAccess(projectId, principal.getId());
        return projectMemberRepository.findByProjectId(projectId)
                .stream()
                .map(ProjectMemberResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProjectMemberResponse addMember(UUID projectId, AddMemberRequest request, UserPrincipal principal) {
        requireAdminOrOwner(projectId, principal.getId());
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        User newMember = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng với email: " + request.getEmail()));

        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, newMember.getId())) {
            throw new BadRequestException("Người dùng này đã là thành viên của dự án");
        }

        ProjectMember member = ProjectMember.builder()
                .project(project)
                .user(newMember)
                .role(request.getRole())
                .build();
        return ProjectMemberResponse.from(projectMemberRepository.save(member));
    }

    @Transactional
    public ProjectMemberResponse updateMemberRole(UUID projectId, UUID userId,
                                                   UpdateMemberRoleRequest request,
                                                   UserPrincipal principal) {
        requireAdminOrOwner(projectId, principal.getId());
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Thành viên không tồn tại trong dự án"));
        if (member.getRole() == ProjectMember.ProjectRole.OWNER) {
            throw new ForbiddenException("Không thể thay đổi vai trò của chủ sở hữu");
        }
        member.setRole(request.getRole());
        return ProjectMemberResponse.from(projectMemberRepository.save(member));
    }

    @Transactional
    public void removeMember(UUID projectId, UUID userId, UserPrincipal principal) {
        requireAdminOrOwner(projectId, principal.getId());
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Thành viên không tồn tại trong dự án"));
        if (member.getRole() == ProjectMember.ProjectRole.OWNER) {
            throw new ForbiddenException("Không thể xóa chủ sở hữu khỏi dự án");
        }
        projectMemberRepository.delete(member);
    }

    // ========== HELPERS ==========

    public Project findProjectWithAccess(UUID projectId, UUID userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
        if (!projectRepository.isProjectMember(projectId, userId)
                && !project.getIsPublic()
                && !project.getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Bạn không có quyền truy cập dự án này");
        }
        return project;
    }

    private void requireAdminOrOwner(UUID projectId, UUID userId) {
        ProjectMember.ProjectRole role = projectMemberRepository
                .findRoleByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ForbiddenException("Bạn không phải thành viên của dự án này"));
        if (role != ProjectMember.ProjectRole.OWNER && role != ProjectMember.ProjectRole.ADMIN) {
            throw new ForbiddenException("Bạn cần quyền ADMIN hoặc OWNER để thực hiện thao tác này");
        }
    }

    private void createDefaultBoard(Project project) {
        Board board = Board.builder()
                .project(project)
                .name("Kanban Board")
                .position(0)
                .isDefault(true)
                .build();
        board = boardRepository.save(board);

        // Tạo các cột mặc định
        String[] defaultColumns = {"Cần làm", "Đang làm", "Đã xong"};
        boolean[] completedFlags = {false, false, true};
        String[] colors = {"#6b7280", "#3b82f6", "#22c55e"};

        for (int i = 0; i < defaultColumns.length; i++) {
            BoardColumn column = BoardColumn.builder()
                    .board(board)
                    .name(defaultColumns[i])
                    .position(i)
                    .color(colors[i])
                    .isCompleted(completedFlags[i])
                    .build();
            boardColumnRepository.save(column);
        }
    }
}
