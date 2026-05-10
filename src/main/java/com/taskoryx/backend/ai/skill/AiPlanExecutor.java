package com.taskoryx.backend.ai.skill;

import com.taskoryx.backend.ai.dto.response.AiExecuteResult;
import com.taskoryx.backend.ai.dto.response.AiProjectPlan;
import com.taskoryx.backend.ai.dto.response.AiTaskItem;
import com.taskoryx.backend.entity.BoardColumn;
import com.taskoryx.backend.entity.Project;
import com.taskoryx.backend.entity.Sprint;
import com.taskoryx.backend.entity.Task;
import com.taskoryx.backend.entity.User;
import com.taskoryx.backend.exception.BadRequestException;
import com.taskoryx.backend.exception.ResourceNotFoundException;
import com.taskoryx.backend.repository.BoardColumnRepository;
import com.taskoryx.backend.repository.ProjectRepository;
import com.taskoryx.backend.repository.SprintRepository;
import com.taskoryx.backend.repository.TaskRepository;
import com.taskoryx.backend.repository.UserRepository;
import com.taskoryx.backend.service.ProjectService;
import com.taskoryx.backend.security.UserPrincipal;
import com.taskoryx.backend.dto.request.project.CreateProjectRequest;
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
 * Thực thi kế hoạch AI: AI chỉ suy nghĩ, Executor mới hành động.
 *
 * Flow: AiProjectPlan (validated) → tạo Project → tạo Sprint → tạo Task cây phân cấp
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiPlanExecutor {

    private final ProjectService projectService;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final SprintRepository sprintRepository;
    private final BoardColumnRepository boardColumnRepository;

    private static final Pattern KEY_PATTERN = Pattern.compile("^[A-Z0-9]{2,10}$");

    @Transactional
    public AiExecuteResult execute(AiProjectPlan plan, UUID targetProjectId, UserPrincipal principal) {
        Project project = targetProjectId != null
                ? resolveExistingProject(targetProjectId, principal)
                : createNewProject(plan, principal);

        Sprint sprint = resolveOrCreateSprint(project);

        User reporter = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user"));

        BoardColumn todoColumn = resolveTodoColumn(sprint);

        AtomicInteger taskCount = new AtomicInteger(0);
        AtomicInteger subTaskCount = new AtomicInteger(0);
        AtomicInteger taskNumberCounter = new AtomicInteger(
                taskRepository.findMaxTaskNumberByProjectId(project.getId()).orElse(0)
        );
        AtomicInteger positionCounter = new AtomicInteger(
                (int) taskRepository.countByProjectId(project.getId())
        );

        if (plan.getTasks() != null) {
            LocalDate baseDate = LocalDate.now();
            for (AiTaskItem item : plan.getTasks()) {
                Task parent = buildTask(item, project, sprint, todoColumn, reporter, null, baseDate,
                        taskNumberCounter.incrementAndGet(), positionCounter.incrementAndGet());
                taskRepository.save(parent);
                taskCount.incrementAndGet();

                if (item.getSubTasks() != null) {
                    for (AiTaskItem subItem : item.getSubTasks()) {
                        Task child = buildTask(subItem, project, sprint, todoColumn, reporter, parent, baseDate,
                                taskNumberCounter.incrementAndGet(), positionCounter.incrementAndGet());
                        taskRepository.save(child);
                        subTaskCount.incrementAndGet();
                    }
                }
            }
        }

        log.info("AI plan executed: projectId={}, tasks={}, subTasks={}",
                project.getId(), taskCount.get(), subTaskCount.get());

        return AiExecuteResult.builder()
                .projectId(project.getId())
                .projectKey(project.getKey())
                .projectName(project.getName())
                .tasksCreated(taskCount.get())
                .subTasksCreated(subTaskCount.get())
                .build();
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

    private Sprint resolveOrCreateSprint(Project project) {
        return sprintRepository.findFirstByProjectIdOrderByCreatedAtAsc(project.getId())
                .orElseGet(() -> {
                    Sprint sprint = new Sprint();
                    sprint.setProject(project);
                    sprint.setName("Sprint 1");
                    sprint.setStatus(Sprint.SprintStatus.PLANNED);
                    return sprintRepository.save(sprint);
                });
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
        task.setStatus(Task.TaskStatus.TODO);
        task.setProject(project);
        task.setSprint(sprint);
        task.setColumn(column);
        task.setReporter(reporter);
        task.setParentTask(parent);
        task.setTaskNumber(taskNumber);

        if (item.getStartOffsetDays() != null) {
            task.setStartDate(baseDate.plusDays(item.getStartOffsetDays()));
        }
        if (item.getDurationDays() != null && task.getStartDate() != null) {
            task.setDueDate(task.getStartDate().plusDays(item.getDurationDays()));
        }

        task.setPosition(BigDecimal.valueOf(position));

        return task;
    }

    private String sanitizeProjectKey(String raw) {
        if (raw == null || raw.isBlank()) return "AI";
        String upper = raw.toUpperCase().replaceAll("[^A-Z0-9]", "");
        if (upper.length() < 2) upper = upper + "AI";
        if (upper.length() > 10) upper = upper.substring(0, 10);
        return KEY_PATTERN.matcher(upper).matches() ? upper : "AIPROJ";
    }
}
