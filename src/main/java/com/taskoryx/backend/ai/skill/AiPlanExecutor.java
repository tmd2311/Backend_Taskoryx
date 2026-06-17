package com.taskoryx.backend.ai.skill;

import com.taskoryx.backend.ai.dto.response.AiExecuteResult;
import com.taskoryx.backend.ai.dto.response.AiProjectPlan;
import com.taskoryx.backend.ai.dto.response.AiSprintItem;
import com.taskoryx.backend.ai.dto.response.AiTaskItem;
import com.taskoryx.backend.dto.request.project.CreateProjectRequest;
import com.taskoryx.backend.entity.Board;
import com.taskoryx.backend.entity.BoardColumn;
import com.taskoryx.backend.entity.IssueCategory;
import com.taskoryx.backend.entity.Label;
import com.taskoryx.backend.entity.Project;
import com.taskoryx.backend.entity.Sprint;
import com.taskoryx.backend.entity.Task;
import com.taskoryx.backend.entity.TaskLabel;
import com.taskoryx.backend.entity.User;
import com.taskoryx.backend.exception.BadRequestException;
import com.taskoryx.backend.exception.ResourceNotFoundException;
import com.taskoryx.backend.repository.BoardColumnRepository;
import com.taskoryx.backend.repository.BoardRepository;
import com.taskoryx.backend.repository.IssueCategoryRepository;
import com.taskoryx.backend.repository.LabelRepository;
import com.taskoryx.backend.repository.ProjectMemberRepository;
import com.taskoryx.backend.repository.ProjectRepository;
import com.taskoryx.backend.repository.SprintRepository;
import com.taskoryx.backend.repository.TaskRepository;
import com.taskoryx.backend.repository.UserRepository;
import com.taskoryx.backend.security.UserPrincipal;
import com.taskoryx.backend.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Thực thi kế hoạch AI: tạo Project → tạo từng Sprint → tạo Tasks phân bổ vào đúng Sprint.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiPlanExecutor {

    private final ProjectService projectService;
    private final ProjectRepository projectRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final SprintRepository sprintRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final LabelRepository labelRepository;
    private final IssueCategoryRepository issueCategoryRepository;
    private final ProjectMemberRepository projectMemberRepository;

    private static final Pattern KEY_PATTERN = Pattern.compile("^[A-Z0-9]{2,10}$");

    @Transactional
    public AiExecuteResult execute(AiProjectPlan plan, UUID targetProjectId, UserPrincipal principal) {
        Project project = targetProjectId != null
                ? resolveExistingProject(targetProjectId, principal)
                : createNewProject(plan, principal);

        User reporter = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user"));

        AtomicInteger taskCount = new AtomicInteger(0);
        AtomicInteger subTaskCount = new AtomicInteger(0);
        AtomicInteger sprintCount = new AtomicInteger(0);
        AtomicInteger taskNumberCounter = new AtomicInteger(
                taskRepository.findMaxTaskNumberByProjectId(project.getId()).orElse(0)
        );
        AtomicInteger positionCounter = new AtomicInteger(
                (int) taskRepository.countByProjectId(project.getId())
        );

        LocalDate projectStartDate = LocalDate.now();

        if (plan.getSprints() != null) {
            for (AiSprintItem sprintItem : plan.getSprints()) {
                Sprint sprint = createSprint(project, sprintItem, projectStartDate);
                sprintCount.incrementAndGet();

                BoardColumn todoColumn = resolveTodoColumn(sprint);

                if (sprintItem.getTasks() != null) {
                    LocalDate sprintStartDate = sprint.getStartDate() != null
                            ? sprint.getStartDate() : projectStartDate;
                    for (AiTaskItem item : sprintItem.getTasks()) {
                        Task parent = buildTask(item, project, sprint, todoColumn, reporter, null,
                                sprintStartDate, taskNumberCounter.incrementAndGet(),
                                positionCounter.incrementAndGet());
                        parent = taskRepository.save(parent);
                        assignTaskLabels(parent, item.getLabelIds(), project.getId());
                        taskCount.incrementAndGet();

                        if (item.getSubTasks() != null) {
                            for (AiTaskItem subItem : item.getSubTasks()) {
                                Task child = buildTask(subItem, project, sprint, todoColumn, reporter, parent,
                                        sprintStartDate, taskNumberCounter.incrementAndGet(),
                                        positionCounter.incrementAndGet());
                                child = taskRepository.save(child);
                                assignTaskLabels(child, subItem.getLabelIds(), project.getId());
                                subTaskCount.incrementAndGet();
                            }
                        }
                    }
                }
            }
        }

        log.info("AI plan executed: projectId={}, sprints={}, tasks={}, subTasks={}",
                project.getId(), sprintCount.get(), taskCount.get(), subTaskCount.get());

        return AiExecuteResult.builder()
                .projectId(project.getId())
                .projectKey(project.getKey())
                .projectName(project.getName())
                .sprintsCreated(sprintCount.get())
                .tasksCreated(taskCount.get())
                .subTasksCreated(subTaskCount.get())
                .build();
    }

    private Sprint createSprint(Project project, AiSprintItem sprintItem, LocalDate projectStartDate) {
        int maxPos = boardRepository.findMaxPositionByProjectId(project.getId()).orElse(-1);
        Board sprintBoard = Board.builder()
                .project(project)
                .name(sprintItem.getName() != null ? sprintItem.getName() : "Sprint")
                .boardType(Board.BoardType.SCRUM)
                .position(maxPos + 1)
                .isDefault(false)
                .build();
        sprintBoard = boardRepository.save(sprintBoard);
        createStatusColumns(sprintBoard);

        // Ưu tiên ngày tuyệt đối (do user chỉnh sửa), fallback về offset
        LocalDate startDate = sprintItem.getStartDate() != null
                ? sprintItem.getStartDate()
                : projectStartDate.plusDays(sprintItem.getStartOffsetDays() != null ? sprintItem.getStartOffsetDays() : 0);
        LocalDate endDate = sprintItem.getEndDate() != null
                ? sprintItem.getEndDate()
                : startDate.plusDays(sprintItem.getDurationDays() != null ? sprintItem.getDurationDays() : 14);

        Sprint sprint = Sprint.builder()
                .project(project)
                .board(sprintBoard)
                .name(sprintItem.getName() != null ? sprintItem.getName() : "Sprint")
                .goal(sprintItem.getGoal())
                .startDate(startDate)
                .endDate(endDate)
                .status(Sprint.SprintStatus.PLANNED)
                .build();

        return sprintRepository.save(sprint);
    }

    private void createStatusColumns(Board board) {
        record ColDef(String name, String color, Task.TaskStatus status, boolean completed) {}
        List<ColDef> defs = List.of(
            new ColDef("To Do",       "#6B7280", Task.TaskStatus.TODO,        false),
            new ColDef("In Progress", "#3B82F6", Task.TaskStatus.IN_PROGRESS, false),
            new ColDef("In Review",   "#F59E0B", Task.TaskStatus.IN_REVIEW,   false),
            new ColDef("Resolved",    "#8B5CF6", Task.TaskStatus.RESOLVED,    false),
            new ColDef("Done",        "#10B981", Task.TaskStatus.DONE,        true),
            new ColDef("Cancelled",   "#EF4444", Task.TaskStatus.CANCELLED,   false)
        );
        for (int i = 0; i < defs.size(); i++) {
            ColDef d = defs.get(i);
            boardColumnRepository.save(BoardColumn.builder()
                    .board(board)
                    .name(d.name())
                    .color(d.color())
                    .mappedStatus(d.status())
                    .isCompleted(d.completed())
                    .position(i)
                    .build());
        }
    }

    private Project createNewProject(AiProjectPlan plan, UserPrincipal principal) {
        String safeKey = sanitizeProjectKey(plan.getProjectKey());
        if (projectRepository.existsByKey(safeKey)) {
            safeKey = safeKey + (int) (Math.random() * 900 + 100);
            if (safeKey.length() > 10) safeKey = safeKey.substring(0, 10);
        }

        CreateProjectRequest req = new CreateProjectRequest();
        req.setName(plan.getProjectName());
        req.setDescription(plan.getProjectDescription());
        req.setKey(safeKey);
        req.setColor("#1976d2");
        req.setProjectType("SCRUM");
        req.setIsPublic(false);

        return projectRepository.findById(projectService.createProject(principal, req).getId())
                .orElseThrow(() -> new BadRequestException("Không thể tạo dự án"));
    }

    private Project resolveExistingProject(UUID projectId, UserPrincipal principal) {
        return projectService.findProjectWithAccess(projectId, principal.getId());
    }

    private BoardColumn resolveTodoColumn(Sprint sprint) {
        if (sprint.getBoard() == null) return null;
        return boardColumnRepository
                .findByBoardIdAndMappedStatus(sprint.getBoard().getId(), Task.TaskStatus.TODO)
                .or(() -> boardColumnRepository.findFirstByBoardIdOrderByPositionAsc(sprint.getBoard().getId()))
                .orElse(null);
    }

    private Task buildTask(AiTaskItem item, Project project, Sprint sprint, BoardColumn column,
                           User reporter, Task parent, LocalDate baseDate, int taskNumber, int position) {
        Task task = new Task();
        task.setTitle(item.getTitle());
        task.setDescription(item.getDescription());
        task.setPriority(item.getPriority() != null ? item.getPriority() : Task.TaskPriority.MEDIUM);
        task.setStatus(item.getStatus() != null ? item.getStatus() : Task.TaskStatus.TODO);
        task.setProject(project);
        task.setSprint(sprint);
        task.setColumn(resolveColumnForStatus(sprint, task.getStatus(), column));
        task.setReporter(reporter);
        task.setParentTask(parent);
        task.setTaskNumber(taskNumber);
        task.setEstimatedHours(item.getEstimatedHours());

        // Ưu tiên ngày tuyệt đối (do user chỉnh sửa), fallback về offset
        if (item.getStartDate() != null) {
            task.setStartDate(item.getStartDate());
        } else if (item.getStartOffsetDays() != null) {
            task.setStartDate(baseDate.plusDays(item.getStartOffsetDays()));
        }
        if (item.getDueDate() != null) {
            task.setDueDate(item.getDueDate());
        } else if (item.getDurationDays() != null && task.getStartDate() != null) {
            task.setDueDate(task.getStartDate().plusDays(item.getDurationDays()));
        }

        // Gán assignee nếu user chỉ định và là thành viên project
        if (item.getAssigneeId() != null
                && projectMemberRepository.existsByProjectIdAndUserId(project.getId(), item.getAssigneeId())) {
            userRepository.findById(item.getAssigneeId()).ifPresent(task::setAssignee);
        }

        // Gán category nếu thuộc project
        if (item.getCategoryId() != null) {
            issueCategoryRepository.findById(item.getCategoryId())
                    .filter(c -> c.getProject().getId().equals(project.getId()))
                    .ifPresent(task::setCategory);
        }

        task.setPosition(BigDecimal.valueOf(position));
        return task;
    }

    private void assignTaskLabels(Task task, List<UUID> labelIds, UUID projectId) {
        if (labelIds == null || labelIds.isEmpty()) return;
        for (UUID labelId : labelIds) {
            labelRepository.findById(labelId)
                    .filter(l -> l.getProject().getId().equals(projectId))
                    .ifPresent(label -> task.getTaskLabels().add(
                            TaskLabel.builder().task(task).label(label).build()));
        }
    }

    private BoardColumn resolveColumnForStatus(Sprint sprint, Task.TaskStatus status, BoardColumn fallback) {
        if (sprint == null || sprint.getBoard() == null || status == null) return fallback;
        return boardColumnRepository
                .findByBoardIdAndMappedStatus(sprint.getBoard().getId(), status)
                .orElse(fallback);
    }

    private String sanitizeProjectKey(String raw) {
        if (raw == null || raw.isBlank()) return "AI";
        String upper = raw.toUpperCase().replaceAll("[^A-Z0-9]", "");
        if (upper.length() < 2) upper = upper + "AI";
        if (upper.length() > 10) upper = upper.substring(0, 10);
        return KEY_PATTERN.matcher(upper).matches() ? upper : "AIPROJ";
    }
}
