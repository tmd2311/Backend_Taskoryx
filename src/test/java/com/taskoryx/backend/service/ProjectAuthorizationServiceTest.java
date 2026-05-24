package com.taskoryx.backend.service;

import com.taskoryx.backend.entity.Project;
import com.taskoryx.backend.entity.ProjectPermission;
import com.taskoryx.backend.entity.User;
import com.taskoryx.backend.exception.ForbiddenException;
import com.taskoryx.backend.exception.ResourceNotFoundException;
import com.taskoryx.backend.repository.ProjectMemberRepository;
import com.taskoryx.backend.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectAuthorizationService - Permission & Access Tests")
class ProjectAuthorizationServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @InjectMocks
    private ProjectAuthorizationService authzService;

    private UUID projectId;
    private UUID ownerId;
    private UUID memberId;
    private UUID outsiderId;
    private Project project;
    private User owner;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        memberId = UUID.randomUUID();
        outsiderId = UUID.randomUUID();

        owner = User.builder().id(ownerId).username("owner")
                .email("owner@test.com").passwordHash("hash").fullName("Owner").build();

        project = Project.builder()
                .id(projectId)
                .name("Test Project")
                .key("TEST")
                .owner(owner)
                .isPublic(false)
                .build();
    }

    // ─── requireProjectAccess ─────────────────────────────────────────────────

    @Test
    @DisplayName("Owner có thể truy cập project của mình")
    void requireProjectAccess_owner_returnsProject() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        Project result = authzService.requireProjectAccess(projectId, ownerId);
        assertThat(result.getId()).isEqualTo(projectId);
    }

    @Test
    @DisplayName("Member (MEMBER role) có thể truy cập project")
    void requireProjectAccess_memberRole_returnsProject() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findRoleByProjectIdAndUserId(projectId, memberId))
                .thenReturn(Optional.of("MEMBER"));

        Project result = authzService.requireProjectAccess(projectId, memberId);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("User không phải member project private → ForbiddenException")
    void requireProjectAccess_outsider_private_throws() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findRoleByProjectIdAndUserId(projectId, outsiderId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authzService.requireProjectAccess(projectId, outsiderId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Project không tồn tại → ResourceNotFoundException")
    void requireProjectAccess_projectNotFound_throws() {
        when(projectRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authzService.requireProjectAccess(projectId, ownerId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── requireProjectAdmin ──────────────────────────────────────────────────

    @Test
    @DisplayName("Owner là admin của project")
    void requireProjectAdmin_owner_succeeds() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        Project result = authzService.requireProjectAdmin(projectId, ownerId);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("PROJECT_MANAGER là admin của project")
    void requireProjectAdmin_projectManager_succeeds() {
        UUID pmId = UUID.randomUUID();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findRoleByProjectIdAndUserId(projectId, pmId))
                .thenReturn(Optional.of("PROJECT_MANAGER"));

        Project result = authzService.requireProjectAdmin(projectId, pmId);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("MEMBER không phải admin → ForbiddenException")
    void requireProjectAdmin_member_throws() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findRoleByProjectIdAndUserId(projectId, memberId))
                .thenReturn(Optional.of("MEMBER"));

        assertThatThrownBy(() -> authzService.requireProjectAdmin(projectId, memberId))
                .isInstanceOf(ForbiddenException.class);
    }

    // ─── requirePermission ────────────────────────────────────────────────────

    @Test
    @DisplayName("Owner có tất cả permission")
    void requirePermission_owner_allPermissions() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        // Không ném exception = pass
        for (String perm : ProjectPermission.ALL) {
            authzService.requirePermission(projectId, ownerId, perm);
        }
    }

    @Test
    @DisplayName("MEMBER có quyền TASK_CREATE")
    void requirePermission_member_hasTaskCreate() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findRoleByProjectIdAndUserId(projectId, memberId))
                .thenReturn(Optional.of("MEMBER"));

        // Không ném exception = có quyền
        authzService.requirePermission(projectId, memberId, ProjectPermission.TASK_CREATE);
    }

    @Test
    @DisplayName("MEMBER KHÔNG có quyền SPRINT_MANAGE")
    void requirePermission_member_noSprintManage() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findRoleByProjectIdAndUserId(projectId, memberId))
                .thenReturn(Optional.of("MEMBER"));

        assertThatThrownBy(() ->
                authzService.requirePermission(projectId, memberId, ProjectPermission.SPRINT_MANAGE))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("TEAM_LEAD có quyền SPRINT_MANAGE")
    void requirePermission_teamLead_hasSprintManage() {
        UUID teamLeadId = UUID.randomUUID();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findRoleByProjectIdAndUserId(projectId, teamLeadId))
                .thenReturn(Optional.of("TEAM_LEAD"));

        authzService.requirePermission(projectId, teamLeadId, ProjectPermission.SPRINT_MANAGE);
    }

    @Test
    @DisplayName("TEAM_LEAD KHÔNG có quyền MEMBER_MANAGE")
    void requirePermission_teamLead_noMemberManage() {
        UUID teamLeadId = UUID.randomUUID();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findRoleByProjectIdAndUserId(projectId, teamLeadId))
                .thenReturn(Optional.of("TEAM_LEAD"));

        assertThatThrownBy(() ->
                authzService.requirePermission(projectId, teamLeadId, ProjectPermission.MEMBER_MANAGE))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("ADMIN (system role trong project) chỉ có TASK_VIEW, BOARD_VIEW, REPORT_VIEW")
    void requirePermission_adminRole_limitedPermissions() {
        UUID adminId = UUID.randomUUID();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findRoleByProjectIdAndUserId(projectId, adminId))
                .thenReturn(Optional.of("ADMIN"));

        // Có quyền
        authzService.requirePermission(projectId, adminId, ProjectPermission.TASK_VIEW);
        authzService.requirePermission(projectId, adminId, ProjectPermission.BOARD_VIEW);
        authzService.requirePermission(projectId, adminId, ProjectPermission.REPORT_VIEW);

        // Không có quyền
        assertThatThrownBy(() ->
                authzService.requirePermission(projectId, adminId, ProjectPermission.TASK_CREATE))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("User không có role nào trong project private → ForbiddenException")
    void requirePermission_noRole_privateProject_throws() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findRoleByProjectIdAndUserId(projectId, outsiderId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                authzService.requirePermission(projectId, outsiderId, ProjectPermission.TASK_VIEW))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("User không có role trong project public → có thể VIEW")
    void requirePermission_noRole_publicProject_canView() {
        project.setIsPublic(true);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findRoleByProjectIdAndUserId(projectId, outsiderId))
                .thenReturn(Optional.empty());

        // View permissions được phép trên public project
        authzService.requirePermission(projectId, outsiderId, ProjectPermission.TASK_VIEW);
        authzService.requirePermission(projectId, outsiderId, ProjectPermission.BOARD_VIEW);
    }

    @Test
    @DisplayName("SUPER_ADMIN có toàn quyền trong project")
    void requirePermission_superAdmin_allPermissions() {
        UUID superAdminId = UUID.randomUUID();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findRoleByProjectIdAndUserId(projectId, superAdminId))
                .thenReturn(Optional.of("SUPER_ADMIN"));

        for (String perm : ProjectPermission.ALL) {
            authzService.requirePermission(projectId, superAdminId, perm);
        }
    }

    // ─── hasPermission (non-throwing) ─────────────────────────────────────────

    @Test
    @DisplayName("hasPermission() trả về true khi có quyền")
    void hasPermission_withPermission_returnsTrue() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findRoleByProjectIdAndUserId(projectId, memberId))
                .thenReturn(Optional.of("MEMBER"));

        boolean result = authzService.hasPermission(projectId, memberId, ProjectPermission.TASK_VIEW);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("hasPermission() trả về false khi không có quyền")
    void hasPermission_withoutPermission_returnsFalse() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findRoleByProjectIdAndUserId(projectId, memberId))
                .thenReturn(Optional.of("MEMBER"));

        boolean result = authzService.hasPermission(projectId, memberId, ProjectPermission.SPRINT_MANAGE);
        assertThat(result).isFalse();
    }
}
