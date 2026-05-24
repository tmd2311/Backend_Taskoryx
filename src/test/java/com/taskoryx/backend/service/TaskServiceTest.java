package com.taskoryx.backend.service;

import com.taskoryx.backend.dto.request.task.CreateTaskRequest;
import com.taskoryx.backend.entity.*;
import com.taskoryx.backend.exception.BadRequestException;
import com.taskoryx.backend.exception.ResourceNotFoundException;
import com.taskoryx.backend.repository.*;
import com.taskoryx.backend.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskService - Task Hierarchy & Business Rule Tests")
class TaskServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private BoardRepository boardRepository;
    @Mock private BoardColumnRepository boardColumnRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectMemberRepository projectMemberRepository;
    @Mock private UserRepository userRepository;
    @Mock private LabelRepository labelRepository;
    @Mock private ProjectService projectService;
    @Mock private ProjectAuthorizationService projectAuthorizationService;
    @Mock private ProjectCapabilityService projectCapabilityService;
    @Mock private NotificationService notificationService;
    @Mock private IssueCategoryRepository issueCategoryRepository;
    @Mock private SprintRepository sprintRepository;
    @Mock private TaskWatcherService taskWatcherService;
    @Mock private ActivityLogService activityLogService;

    @InjectMocks
    private TaskService taskService;

    private UUID projectId;
    private UUID userId;
    private UUID sprintId;
    private UUID boardId;
    private UUID columnId;

    private User reporter;
    private Project project;
    private Sprint sprint;
    private Board board;
    private BoardColumn column;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        userId = UUID.randomUUID();
        sprintId = UUID.randomUUID();
        boardId = UUID.randomUUID();
        columnId = UUID.randomUUID();

        reporter = User.builder()
                .id(userId).username("user1").email("user1@test.com")
                .passwordHash("hash").fullName("User One").build();

        project = Project.builder()
                .id(projectId).key("TX").name("Test Project").owner(reporter).build();

        board = Board.builder().id(boardId).project(project).name("Main Board").build();

        sprint = Sprint.builder()
                .id(sprintId).name("Sprint 1")
                .project(project).board(board)
                .status(Sprint.SprintStatus.ACTIVE).build();

        column = BoardColumn.builder()
                .id(columnId).board(board).name("To Do")
                .mappedStatus(Task.TaskStatus.TODO).position(1)
                .build();

        principal = UserPrincipal.builder()
                .id(userId).username("user1").email("user1@test.com")
                .password("hash").fullName("User One").active(true)
                .authorities(Collections.emptyList()).build();
    }

    // ─── Task hierarchy validation ────────────────────────────────────────────

    @Test
    @DisplayName("Tạo task cha cấp 3 (canHaveChildren=false) → BadRequestException")
    void createTask_parentAtLevel3_throwsBadRequest() {
        // Tạo chuỗi cha ông → cha → cha cấp 3
        Task grandParent = Task.builder().id(UUID.randomUUID()).title("Epic")
                .project(project).taskNumber(1).build();
        Task parent = Task.builder().id(UUID.randomUUID()).title("Story")
                .project(project).taskNumber(2).parentTask(grandParent).build();
        Task level3Parent = Task.builder().id(UUID.randomUUID()).title("Subtask")
                .project(project).taskNumber(3).parentTask(parent).build();
        // level3Parent.canHaveChildren() = false vì depth = 3

        UUID level3ParentId = level3Parent.getId();
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Sub-subtask");
        request.setSprintId(sprintId);
        request.setParentTaskId(level3ParentId);

        // Setup mocks để vượt qua authorization và sprint lookup
        doNothing().when(projectAuthorizationService)
                .requirePermission(projectId, userId, ProjectPermission.TASK_CREATE);
        when(projectService.findProjectWithAccess(projectId, userId)).thenReturn(project);
        doNothing().when(projectCapabilityService).requireModule(any(), any());
        when(sprintRepository.findById(sprintId)).thenReturn(Optional.of(sprint));
        when(boardColumnRepository.findByBoardIdAndMappedStatus(boardId, Task.TaskStatus.TODO))
                .thenReturn(Optional.of(column));
        when(userRepository.findById(userId)).thenReturn(Optional.of(reporter));
        when(taskRepository.findMaxTaskNumberByProjectId(projectId)).thenReturn(Optional.of(3));
        when(taskRepository.findMaxPositionByColumnId(columnId)).thenReturn(Optional.of(BigDecimal.valueOf(3000)));
        when(taskRepository.findById(level3ParentId)).thenReturn(Optional.of(level3Parent));

        assertThatThrownBy(() -> taskService.createTask(projectId, request, principal))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cấp 3");
    }

    @Test
    @DisplayName("Tạo task với parentId không tồn tại → ResourceNotFoundException")
    void createTask_nonExistentParent_throwsResourceNotFound() {
        UUID nonExistentParentId = UUID.randomUUID();
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Child task");
        request.setSprintId(sprintId);
        request.setParentTaskId(nonExistentParentId);

        doNothing().when(projectAuthorizationService)
                .requirePermission(projectId, userId, ProjectPermission.TASK_CREATE);
        when(projectService.findProjectWithAccess(projectId, userId)).thenReturn(project);
        doNothing().when(projectCapabilityService).requireModule(any(), any());
        when(sprintRepository.findById(sprintId)).thenReturn(Optional.of(sprint));
        when(boardColumnRepository.findByBoardIdAndMappedStatus(boardId, Task.TaskStatus.TODO))
                .thenReturn(Optional.of(column));
        when(userRepository.findById(userId)).thenReturn(Optional.of(reporter));
        when(taskRepository.findMaxTaskNumberByProjectId(projectId)).thenReturn(Optional.empty());
        when(taskRepository.findMaxPositionByColumnId(columnId)).thenReturn(Optional.empty());
        when(taskRepository.findById(nonExistentParentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.createTask(projectId, request, principal))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Tạo task với parent thuộc project khác → BadRequestException")
    void createTask_parentFromDifferentProject_throwsBadRequest() {
        UUID otherProjectId = UUID.randomUUID();
        Project otherProject = Project.builder().id(otherProjectId).key("OTH")
                .name("Other Project").owner(reporter).build();
        Task foreignParent = Task.builder().id(UUID.randomUUID()).title("Foreign")
                .project(otherProject).taskNumber(1).build();

        UUID foreignParentId = foreignParent.getId();
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Child");
        request.setSprintId(sprintId);
        request.setParentTaskId(foreignParentId);

        doNothing().when(projectAuthorizationService)
                .requirePermission(projectId, userId, ProjectPermission.TASK_CREATE);
        when(projectService.findProjectWithAccess(projectId, userId)).thenReturn(project);
        doNothing().when(projectCapabilityService).requireModule(any(), any());
        when(sprintRepository.findById(sprintId)).thenReturn(Optional.of(sprint));
        when(boardColumnRepository.findByBoardIdAndMappedStatus(boardId, Task.TaskStatus.TODO))
                .thenReturn(Optional.of(column));
        when(userRepository.findById(userId)).thenReturn(Optional.of(reporter));
        when(taskRepository.findMaxTaskNumberByProjectId(projectId)).thenReturn(Optional.empty());
        when(taskRepository.findMaxPositionByColumnId(columnId)).thenReturn(Optional.empty());
        when(taskRepository.findById(foreignParentId)).thenReturn(Optional.of(foreignParent));

        assertThatThrownBy(() -> taskService.createTask(projectId, request, principal))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cùng project");
    }

    @Test
    @DisplayName("Sprint không có board → BadRequestException")
    void createTask_sprintWithoutBoard_throwsBadRequest() {
        Sprint sprintWithoutBoard = Sprint.builder()
                .id(sprintId).name("Sprint 1")
                .project(project).board(null)
                .status(Sprint.SprintStatus.ACTIVE).build();

        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Task");
        request.setSprintId(sprintId);

        doNothing().when(projectAuthorizationService)
                .requirePermission(projectId, userId, ProjectPermission.TASK_CREATE);
        when(projectService.findProjectWithAccess(projectId, userId)).thenReturn(project);
        doNothing().when(projectCapabilityService).requireModule(any(), any());
        when(sprintRepository.findById(sprintId)).thenReturn(Optional.of(sprintWithoutBoard));

        assertThatThrownBy(() -> taskService.createTask(projectId, request, principal))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("board");
    }

    @Test
    @DisplayName("Sprint không tồn tại → ResourceNotFoundException hoặc BadRequestException")
    void createTask_sprintNotFound_throwsException() {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Task");
        request.setSprintId(UUID.randomUUID());

        doNothing().when(projectAuthorizationService)
                .requirePermission(projectId, userId, ProjectPermission.TASK_CREATE);
        when(projectService.findProjectWithAccess(projectId, userId)).thenReturn(project);
        doNothing().when(projectCapabilityService).requireModule(any(), any());
        when(sprintRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.createTask(projectId, request, principal))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("User không có quyền TASK_CREATE → ForbiddenException")
    void createTask_noPermission_throwsForbidden() {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Task");
        request.setSprintId(sprintId);

        doThrow(new com.taskoryx.backend.exception.ForbiddenException("Không có quyền"))
                .when(projectAuthorizationService)
                .requirePermission(projectId, userId, ProjectPermission.TASK_CREATE);

        assertThatThrownBy(() -> taskService.createTask(projectId, request, principal))
                .isInstanceOf(com.taskoryx.backend.exception.ForbiddenException.class);
    }

    // ─── getTask tests ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getTask với taskId không tồn tại → ResourceNotFoundException")
    void getTask_notFound_throwsResourceNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(taskRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTask(unknownId, principal))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
