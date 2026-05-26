package com.taskoryx.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskoryx.backend.entity.ActivityLog;
import com.taskoryx.backend.dto.request.project.AddMemberRequest;
import com.taskoryx.backend.dto.request.project.CreateProjectRequest;
import com.taskoryx.backend.dto.request.project.UpdateProjectRequest;
import com.taskoryx.backend.dto.response.comment.MentionedUserInfo;
import com.taskoryx.backend.dto.response.project.ProjectMemberResponse;
import com.taskoryx.backend.dto.response.project.ProjectResponse;
import com.taskoryx.backend.dto.response.template.TemplateConfigDto;
import com.taskoryx.backend.entity.Board;
import com.taskoryx.backend.entity.BoardColumn;
import com.taskoryx.backend.entity.Project;
import com.taskoryx.backend.entity.Task;
import com.taskoryx.backend.entity.ProjectPermission;
import com.taskoryx.backend.entity.ProjectMember;
import com.taskoryx.backend.entity.User;
import com.taskoryx.backend.exception.BadRequestException;
import com.taskoryx.backend.exception.ForbiddenException;
import com.taskoryx.backend.exception.ResourceNotFoundException;
import com.taskoryx.backend.repository.BoardColumnRepository;
import com.taskoryx.backend.repository.BoardRepository;
import com.taskoryx.backend.repository.ProjectMemberRepository;
import com.taskoryx.backend.repository.ProjectRepository;
import com.taskoryx.backend.repository.SprintRepository;
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
    private final SprintRepository sprintRepository;
    private final ProjectAuthorizationService projectAuthorizationService;
    private final ProjectCapabilityService projectCapabilityService;
    private final ObjectMapper objectMapper;
    private final ActivityLogService activityLogService;

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
                .projectType(normalizeProjectType(request.getProjectType()))
                .projectConfig(serializeProjectConfig(request.getProjectConfig()))
                .configVersion(1)
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
                .isArchived(false)
                .build();
        project = projectRepository.save(project);

        // Thêm owner vào project members
        ProjectMember ownerMember = ProjectMember.builder()
                .project(project)
                .user(owner)
                .role("OWNER")
                .build();
        projectMemberRepository.save(ownerMember);

        // Tạo board mặc định
        createDefaultBoard(project);

        String createDesc = owner.getFullName() + " đã tạo dự án \"" + project.getName() + "\" (mã: " + project.getKey() + ")";
        activityLogService.logActivity(owner, project,
                ActivityLog.EntityType.PROJECT, project.getId(), ActivityLog.Action.CREATE,
                project.getName(), createDesc,
                null, "{\"name\":\"" + project.getName() + "\",\"key\":\"" + project.getKey() + "\"}");

        ProjectResponse response = ProjectResponse.from(project);
        response.setCurrentUserRole("OWNER");
        return response;
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(UUID projectId, UserPrincipal principal) {
        Project project = projectAuthorizationService.requireProjectAccess(projectId, principal.getId());
        ProjectResponse response = ProjectResponse.from(project);
        projectMemberRepository.findRoleByProjectIdAndUserId(projectId, principal.getId())
                .ifPresent(response::setCurrentUserRole);
        return response;
    }

    @Transactional
    public ProjectResponse updateProject(UUID projectId, UpdateProjectRequest request, UserPrincipal principal) {
        Project project = projectAuthorizationService.requireProjectAdmin(projectId, principal.getId());

        if (request.getName() != null) project.setName(request.getName());
        if (request.getDescription() != null) project.setDescription(request.getDescription());
        if (request.getColor() != null) project.setColor(request.getColor());
        if (request.getIcon() != null) project.setIcon(request.getIcon());
        if (request.getProjectType() != null) project.setProjectType(normalizeProjectType(request.getProjectType()));
        if (request.getProjectConfig() != null) {
            project.setProjectConfig(serializeProjectConfig(request.getProjectConfig()));
            project.setConfigVersion(project.getConfigVersion() != null ? project.getConfigVersion() + 1 : 1);
        }
        if (request.getIsPublic() != null) project.setIsPublic(request.getIsPublic());
        if (request.getIsArchived() != null) project.setIsArchived(request.getIsArchived());

        Project saved = projectRepository.save(project);
        User actor = userRepository.findById(principal.getId()).orElseThrow();
        String updateDesc = actor.getFullName() + " đã cập nhật thông tin dự án \"" + saved.getName() + "\"";
        activityLogService.logActivity(actor, saved,
                ActivityLog.EntityType.PROJECT, saved.getId(), ActivityLog.Action.UPDATE,
                saved.getName(), updateDesc,
                null, "{\"name\":\"" + saved.getName() + "\"}");
        return ProjectResponse.from(saved);
    }

    @Transactional
    public void deleteProject(UUID projectId, UserPrincipal principal) {
        Project project = projectAuthorizationService.requireProjectAccess(projectId, principal.getId());
        if (!project.getOwner().getId().equals(principal.getId())) {
            throw new ForbiddenException("Chỉ chủ sở hữu mới có thể xóa dự án");
        }

        long taskCount = taskRepository.countByProjectId(projectId);
        if (taskCount > 0) {
            throw new BadRequestException(
                String.format("Không thể xóa dự án vì còn %d task đang tồn tại. " +
                              "Vui lòng xóa hết task trước khi xóa dự án.", taskCount));
        }

        // Xóa sprints trước để tránh FK violation khi boards bị cascade delete
        sprintRepository.deleteByProjectId(projectId);

        projectRepository.delete(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> getMembers(UUID projectId, UserPrincipal principal) {
        projectAuthorizationService.requireProjectAccess(projectId, principal.getId());
        return projectMemberRepository.findByProjectId(projectId)
                .stream()
                .map(ProjectMemberResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Tìm kiếm thành viên trong project để dùng cho @mention autocomplete.
     * Chỉ trả thành viên thuộc project → đảm bảo FE chỉ suggest đúng người.
     */
    @Transactional(readOnly = true)
    public List<MentionedUserInfo> searchMembersForMention(UUID projectId, String keyword,
                                                            UserPrincipal principal) {
        projectAuthorizationService.requireProjectAccess(projectId, principal.getId());
        return projectMemberRepository
                .searchMembersByKeyword(projectId, keyword == null ? "" : keyword)
                .stream()
                .map(pm -> MentionedUserInfo.builder()
                        .userId(pm.getUser().getId())
                        .username(pm.getUser().getUsername())
                        .fullName(pm.getUser().getFullName())
                        .avatarUrl(pm.getUser().getAvatarUrl())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public ProjectMemberResponse addMember(UUID projectId, AddMemberRequest request, UserPrincipal principal) {
        projectAuthorizationService.requirePermission(projectId, principal.getId(), ProjectPermission.MEMBER_MANAGE);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        User newMember = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng với email: " + request.getEmail()));

        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, newMember.getId())) {
            throw new BadRequestException("Người dùng này đã là thành viên của dự án");
        }

        // Role trong dự án = system role của user (user chỉ có 1 system role)
        String projectRole = newMember.getUserRoles().stream()
                .map(ur -> ur.getRole().getName())
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "Người dùng '" + request.getEmail() + "' chưa được gán role hệ thống. " +
                        "Vui lòng liên hệ admin để cấp quyền trước khi thêm vào dự án."));

        ProjectMember member = ProjectMember.builder()
                .project(project)
                .user(newMember)
                .role(projectRole)
                .build();
        ProjectMember savedMember = projectMemberRepository.save(member);
        // Reload để đảm bảo lazy collection getUserRoles() được load đầy đủ
        savedMember = projectMemberRepository.findById(savedMember.getId()).orElse(savedMember);
        ProjectMemberResponse response = ProjectMemberResponse.from(savedMember);

        User actor = userRepository.findById(principal.getId()).orElseThrow();
        String addMemberDesc = actor.getFullName() + " đã thêm thành viên " + newMember.getFullName()
                + " (" + newMember.getEmail() + ") vào dự án \"" + project.getName() + "\"";
        activityLogService.logActivity(actor, project,
                ActivityLog.EntityType.PROJECT, project.getId(), ActivityLog.Action.MEMBER_ADDED,
                project.getName(), addMemberDesc,
                null, "{\"userId\":\"" + newMember.getId() + "\",\"fullName\":\"" + newMember.getFullName()
                        + "\",\"email\":\"" + newMember.getEmail() + "\",\"role\":\"" + projectRole + "\"}");

        return response;
    }

    @Transactional
    public void removeMember(UUID projectId, UUID userId, UserPrincipal principal) {
        projectAuthorizationService.requirePermission(projectId, principal.getId(), ProjectPermission.MEMBER_MANAGE);
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Thành viên không tồn tại trong dự án"));
        if ("OWNER".equals(member.getRole())) {
            throw new ForbiddenException("Không thể xóa chủ sở hữu khỏi dự án");
        }
        Project project = member.getProject();
        User removedUser = member.getUser();
        User actor = userRepository.findById(principal.getId()).orElseThrow();
        String removeMemberDesc = actor.getFullName() + " đã xóa thành viên " + removedUser.getFullName()
                + " (" + removedUser.getEmail() + ") khỏi dự án \"" + project.getName() + "\"";
        activityLogService.logActivity(actor, project,
                ActivityLog.EntityType.PROJECT, project.getId(), ActivityLog.Action.MEMBER_REMOVED,
                project.getName(), removeMemberDesc,
                "{\"userId\":\"" + userId + "\",\"fullName\":\"" + removedUser.getFullName()
                        + "\",\"email\":\"" + removedUser.getEmail() + "\"}", null);
        projectMemberRepository.delete(member);
    }

    @Transactional
    public ProjectMemberResponse updateMemberRole(UUID projectId, UUID userId,
                                                   String newRole, UserPrincipal principal) {
        projectAuthorizationService.requirePermission(projectId, principal.getId(), ProjectPermission.MEMBER_MANAGE);
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Thành viên không tồn tại trong dự án"));
        if ("OWNER".equals(member.getRole())) {
            throw new ForbiddenException("Không thể thay đổi role của chủ sở hữu dự án");
        }

        String oldRole = member.getRole();
        member.setRole(newRole);
        ProjectMember saved = projectMemberRepository.save(member);
        saved = projectMemberRepository.findById(saved.getId()).orElse(saved);

        User actor = userRepository.findById(principal.getId()).orElseThrow();
        User targetUser = member.getUser();
        Project project = member.getProject();
        String desc = actor.getFullName() + " đã đổi role của " + targetUser.getFullName()
                + " từ \"" + oldRole + "\" → \"" + newRole + "\" trong dự án \"" + project.getName() + "\"";
        activityLogService.logActivity(actor, project,
                ActivityLog.EntityType.PROJECT, project.getId(), ActivityLog.Action.UPDATE,
                project.getName(), desc,
                "{\"userId\":\"" + userId + "\",\"role\":\"" + oldRole + "\"}",
                "{\"userId\":\"" + userId + "\",\"fullName\":\"" + targetUser.getFullName()
                        + "\",\"role\":\"" + newRole + "\"}");

        return ProjectMemberResponse.from(saved);
    }

    // ========== HELPERS ==========

    public Project findProjectWithAccess(UUID projectId, UUID userId) {
        return projectAuthorizationService.requireProjectAccess(projectId, userId);
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
        record ColDef(String name, String color, boolean isCompleted, Task.TaskStatus status) {}
        List<ColDef> cols = List.of(
            new ColDef("Cần làm",  "#6b7280", false, Task.TaskStatus.TODO),
            new ColDef("Đang làm", "#3b82f6", false, Task.TaskStatus.IN_PROGRESS),
            new ColDef("Đã xong",  "#22c55e", true,  Task.TaskStatus.DONE)
        );

        for (int i = 0; i < cols.size(); i++) {
            ColDef def = cols.get(i);
            BoardColumn column = BoardColumn.builder()
                    .board(board)
                    .name(def.name())
                    .position(i)
                    .color(def.color())
                    .isCompleted(def.isCompleted())
                    .mappedStatus(def.status())
                    .build();
            boardColumnRepository.save(column);
        }
    }

    private String normalizeProjectType(String projectType) {
        if (projectType == null || projectType.isBlank()) {
            return null;
        }
        return projectType.trim()
                .toUpperCase()
                .replace(' ', '_')
                .replace('-', '_');
    }

    private String serializeProjectConfig(Object config) {
        if (config == null) {
            return null;
        }
        try {
            TemplateConfigDto sanitizedConfig = projectCapabilityService.sanitizeProjectConfig(
                    objectMapper.convertValue(config, TemplateConfigDto.class));
            return objectMapper.writeValueAsString(sanitizedConfig);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Cấu hình project không hợp lệ");
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Cấu hình project không hợp lệ");
        }
    }
}
