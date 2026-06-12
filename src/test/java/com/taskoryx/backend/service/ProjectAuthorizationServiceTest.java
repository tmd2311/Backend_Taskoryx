package com.taskoryx.backend.service;

import com.taskoryx.backend.entity.Project;
import com.taskoryx.backend.entity.ProjectPermission;
import com.taskoryx.backend.entity.ProjectRole;
import com.taskoryx.backend.entity.User;
import com.taskoryx.backend.exception.ForbiddenException;
import com.taskoryx.backend.exception.ResourceNotFoundException;
import com.taskoryx.backend.repository.ProjectMemberRepository;
import com.taskoryx.backend.repository.ProjectRepository;
import com.taskoryx.backend.repository.ProjectRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectAuthorizationService - Permission & Access Tests")
class ProjectAuthorizationServiceTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectMemberRepository projectMemberRepository;
    @Mock private ProjectRoleRepository projectRoleRepository;

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
        projectId  = UUID.randomUUID();
        ownerId    = UUID.randomUUID();
        memberId   = UUID.randomUUID();
        outsiderId = UUID.randomUUID();

        owner = User.builder().id(ownerId).username("owner")
                .email("owner@test.com").passwordHash("hash").fullName("Owner").build();

        project = Project.builder()
                .id(projectId).name("Test Project").key("TEST")
                .owner(owner).isPublic(false).build();
    }

    // Helper: tạo custom role với danh sách quyền nâng cao
    private ProjectRole customRole(String name, String... advancedPerms) {
        ProjectRole role = new ProjectRole();
        role.setPermissionList(List.of(advancedPerms));
        return role;
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
    @DisplayName("Member có thể truy cập project")
    void requireProjectAccess_member_returnsProject() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findRoleByProjectIdAndUserId(projectId, memberId))
                .thenReturn(Optional.of("Dev"));

        Project result = authzService.requireProjectAccess(projectId, memberId);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("User không phải member của project private → ForbiddenException")
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
        assertThat(authzService.requireProjectAdmin(projectId, ownerId)).isNotNull();
    }

    @Test
    @DisplayName("PROJECT_MANAGER là admin của project")
    void requireProjectAdmin_projectManager_succeeds() {
        UUID pmId = UUID.randomUUID();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findRoleByProjectIdAndUserId(projectId, pmId))
                .thenReturn(Optional.of("PROJECT_MANAGER"));

        assertThat(authzService.requireProjectAdmin(projectId, pmId)).isNotNull();
    }

    @Test
    @DisplayName("Member thường không phải admin → ForbiddenException")
    void requireProjectAdmin_member_throws() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findRoleByProjectIdAndUserId(projectId, memberId))
                .thenReturn(Optional.of("Dev"));

        assertThatThrownBy(() -> authzService.requireProjectAdmin(projectId, memberId))
                .isInstanceOf(ForbiddenException.class);
    }

    // ─── Quyền cơ bản (BASIC) — mọi member đều có ───────────────────────────

    @Test
    @DisplayName("Mọi member đều có TASK_VIEW dù custom role không khai báo")
    void basicPermission_taskView_alwaysGranted() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findRoleByProjectIdAndUserId(projectId, memberId))
                .thenReturn(Optional.of("Intern"));
        when(projectRoleRepository.findByProjectIdAndName(projectId, "Intern"))
                .thenReturn(Optional.of(customRole("Intern"))); // không có quyền nâng cao nào

        authzService.requirePermission(projectId, memberId, ProjectPermission.TASK_VIEW);
    }

    @Test
    @DisplayName("Mọi member đều có TIME_TRACKING_MANAGE dù custom role không khai báo")
    void basicPermission_timeTrackingManage_alwaysGranted() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findRoleByProjectIdAndUserId(projectId, memberId))
                .thenReturn(Optional.of("Tester"));
        when(projectRoleRepository.findByProjectIdAndName(projectId, "Tester"))
                .thenReturn(Optional.of(customRole("Tester")));

        authzService.requirePermission(projectId, memberId, ProjectPermission.TIME_TRACKING_MANAGE);
    }

    @Test
    @DisplayName("Mọi member đều có TASK_CREATE, TASK_UPDATE, COMMENT_CREATE, BOARD_VIEW")
    void basicPermission_otherBasicPerms_alwaysGranted() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findRoleByProjectIdAndUserId(projectId, memberId))
                .thenReturn(Optional.of("Dev"));
        when(projectRoleRepository.findByProjectIdAndName(projectId, "Dev"))
                .thenReturn(Optional.of(customRole("Dev")));

        for (String perm : ProjectPermission.BASIC) {
            authzService.requirePermission(projectId, memberId, perm);
        }
    }

    // ─── Quyền nâng cao — cần khai báo trong custom role ─────────────────────

    @Test
    @DisplayName("Member không có quyền nâng cao nếu custom role không khai báo")
    void advancedPermission_notGranted_ifNotInRole() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findRoleByProjectIdAndUserId(projectId, memberId))
                .thenReturn(Optional.of("Dev"));
        when(projectRoleRepository.findByProjectIdAndName(projectId, "Dev"))
                .thenReturn(Optional.of(customRole("Dev"))); // không cấp gì thêm

        assertThatThrownBy(() ->
                authzService.requirePermission(projectId, memberId, ProjectPermission.SPRINT_MANAGE))
                .isInstanceOf(ForbiddenException.class);

        assertThatThrownBy(() ->
                authzService.requirePermission(projectId, memberId, ProjectPermission.MEMBER_MANAGE))
                .isInstanceOf(ForbiddenException.class);

        assertThatThrownBy(() ->
                authzService.requirePermission(projectId, memberId, ProjectPermission.REPORT_VIEW))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Custom role được cấp SPRINT_MANAGE thì có quyền đó")
    void advancedPermission_sprintManage_grantedIfInRole() {
        UUID leadId = UUID.randomUUID();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findRoleByProjectIdAndUserId(projectId, leadId))
                .thenReturn(Optional.of("TeamLead"));
        when(projectRoleRepository.findByProjectIdAndName(projectId, "TeamLead"))
                .thenReturn(Optional.of(customRole("TeamLead",
                        ProjectPermission.SPRINT_MANAGE,
                        ProjectPermission.REPORT_VIEW,
                        ProjectPermission.TASK_DELETE)));

        authzService.requirePermission(projectId, leadId, ProjectPermission.SPRINT_MANAGE);
        authzService.requirePermission(projectId, leadId, ProjectPermission.REPORT_VIEW);
        authzService.requirePermission(projectId, leadId, ProjectPermission.TASK_DELETE);
    }

    @Test
    @DisplayName("Custom role có SPRINT_MANAGE nhưng không có MEMBER_MANAGE")
    void advancedPermission_partialGrant() {
        UUID leadId = UUID.randomUUID();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findRoleByProjectIdAndUserId(projectId, leadId))
                .thenReturn(Optional.of("TeamLead"));
        when(projectRoleRepository.findByProjectIdAndName(projectId, "TeamLead"))
                .thenReturn(Optional.of(customRole("TeamLead", ProjectPermission.SPRINT_MANAGE)));

        authzService.requirePermission(projectId, leadId, ProjectPermission.SPRINT_MANAGE);

        assertThatThrownBy(() ->
                authzService.requirePermission(projectId, leadId, ProjectPermission.MEMBER_MANAGE))
                .isInstanceOf(ForbiddenException.class);
    }

    // ─── Owner & SUPER_ADMIN ──────────────────────────────────────────────────

    @Test
    @DisplayName("Owner có tất cả permission")
    void requirePermission_owner_allPermissions() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        for (String perm : ProjectPermission.ALL) {
            authzService.requirePermission(projectId, ownerId, perm);
        }
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

    // ─── Public project ───────────────────────────────────────────────────────

    @Test
    @DisplayName("User không có role trong project private → ForbiddenException")
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

        authzService.requirePermission(projectId, outsiderId, ProjectPermission.TASK_VIEW);
        authzService.requirePermission(projectId, outsiderId, ProjectPermission.BOARD_VIEW);
    }

    // ─── hasPermission (non-throwing) ─────────────────────────────────────────

    @Test
    @DisplayName("hasPermission() trả về true với quyền cơ bản")
    void hasPermission_basicPerm_returnsTrue() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findRoleByProjectIdAndUserId(projectId, memberId))
                .thenReturn(Optional.of("Dev"));
        when(projectRoleRepository.findByProjectIdAndName(projectId, "Dev"))
                .thenReturn(Optional.of(customRole("Dev")));

        assertThat(authzService.hasPermission(projectId, memberId, ProjectPermission.TASK_VIEW)).isTrue();
        assertThat(authzService.hasPermission(projectId, memberId, ProjectPermission.TIME_TRACKING_MANAGE)).isTrue();
    }

    @Test
    @DisplayName("hasPermission() trả về false với quyền nâng cao chưa được cấp")
    void hasPermission_advancedNotGranted_returnsFalse() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findRoleByProjectIdAndUserId(projectId, memberId))
                .thenReturn(Optional.of("Dev"));
        when(projectRoleRepository.findByProjectIdAndName(projectId, "Dev"))
                .thenReturn(Optional.of(customRole("Dev")));

        assertThat(authzService.hasPermission(projectId, memberId, ProjectPermission.SPRINT_MANAGE)).isFalse();
        assertThat(authzService.hasPermission(projectId, memberId, ProjectPermission.MEMBER_MANAGE)).isFalse();
    }
}
