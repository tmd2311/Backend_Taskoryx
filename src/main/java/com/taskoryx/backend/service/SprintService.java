package com.taskoryx.backend.service;

import com.taskoryx.backend.dto.request.sprint.CreateSprintRequest;
import com.taskoryx.backend.dto.request.sprint.UpdateSprintRequest;
import com.taskoryx.backend.dto.response.sprint.SprintResponse;
import com.taskoryx.backend.entity.Project;
import com.taskoryx.backend.entity.ProjectMember;
import com.taskoryx.backend.entity.Sprint;
import com.taskoryx.backend.entity.Task;
import com.taskoryx.backend.exception.BadRequestException;
import com.taskoryx.backend.exception.ForbiddenException;
import com.taskoryx.backend.exception.ResourceNotFoundException;
import com.taskoryx.backend.repository.ProjectMemberRepository;
import com.taskoryx.backend.repository.SprintRepository;
import com.taskoryx.backend.repository.TaskRepository;
import com.taskoryx.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SprintService {

    private final SprintRepository sprintRepository;
    private final TaskRepository taskRepository;
    private final ProjectService projectService;
    private final ProjectMemberRepository projectMemberRepository;

    // ========== CREATE ==========

    public SprintResponse createSprint(UUID projectId, CreateSprintRequest request, UserPrincipal principal) {
        Project project = projectService.findProjectWithAccess(projectId, principal.getId());
        requireAdminOrOwner(projectId, principal.getId());

        if (request.getStartDate() != null && request.getEndDate() != null) {
            if (!request.getStartDate().isBefore(request.getEndDate())) {
                throw new BadRequestException("Ngày bắt đầu phải trước ngày kết thúc");
            }
        }

        Sprint sprint = Sprint.builder()
                .project(project)
                .name(request.getName())
                .goal(request.getGoal())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(Sprint.SprintStatus.PLANNED)
                .build();

        sprint = sprintRepository.save(sprint);
        return SprintResponse.fromWithTasks(sprint);
    }

    // ========== READ ==========

    @Transactional(readOnly = true)
    public List<SprintResponse> getSprints(UUID projectId, UserPrincipal principal) {
        projectService.findProjectWithAccess(projectId, principal.getId());
        return sprintRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(SprintResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SprintResponse getSprint(UUID sprintId, UserPrincipal principal) {
        Sprint sprint = findSprintById(sprintId);
        projectService.findProjectWithAccess(sprint.getProject().getId(), principal.getId());
        return SprintResponse.fromWithTasks(sprint);
    }

    // ========== UPDATE ==========

    public SprintResponse updateSprint(UUID sprintId, UpdateSprintRequest request, UserPrincipal principal) {
        Sprint sprint = findSprintById(sprintId);
        requireAdminOrOwner(sprint.getProject().getId(), principal.getId());

        if (sprint.getStatus() == Sprint.SprintStatus.COMPLETED
                || sprint.getStatus() == Sprint.SprintStatus.CANCELLED) {
            throw new BadRequestException("Không thể cập nhật sprint đã hoàn thành hoặc đã hủy");
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            sprint.setName(request.getName());
        }
        if (request.getGoal() != null) {
            sprint.setGoal(request.getGoal());
        }

        LocalDate newStart = request.getStartDate() != null ? request.getStartDate() : sprint.getStartDate();
        LocalDate newEnd   = request.getEndDate()   != null ? request.getEndDate()   : sprint.getEndDate();

        if (newStart != null && newEnd != null && !newStart.isBefore(newEnd)) {
            throw new BadRequestException("Ngày bắt đầu phải trước ngày kết thúc");
        }

        if (request.getStartDate() != null) sprint.setStartDate(request.getStartDate());
        if (request.getEndDate()   != null) sprint.setEndDate(request.getEndDate());

        sprint = sprintRepository.save(sprint);
        return SprintResponse.fromWithTasks(sprint);
    }

    // ========== START SPRINT ==========

    public SprintResponse startSprint(UUID sprintId, UserPrincipal principal) {
        Sprint sprint = findSprintById(sprintId);
        requireAdminOrOwner(sprint.getProject().getId(), principal.getId());

        if (sprint.getStatus() != Sprint.SprintStatus.PLANNED) {
            throw new BadRequestException("Chỉ có thể bắt đầu sprint đang ở trạng thái PLANNED");
        }

        if (sprintRepository.existsByProjectIdAndStatus(sprint.getProject().getId(), Sprint.SprintStatus.ACTIVE)) {
            throw new BadRequestException("Dự án đã có sprint đang hoạt động. Vui lòng hoàn thành sprint hiện tại trước");
        }

        sprint.setStatus(Sprint.SprintStatus.ACTIVE);
        if (sprint.getStartDate() == null) {
            sprint.setStartDate(LocalDate.now());
        }

        sprint = sprintRepository.save(sprint);
        return SprintResponse.fromWithTasks(sprint);
    }

    // ========== COMPLETE SPRINT ==========

    public SprintResponse completeSprint(UUID sprintId, UserPrincipal principal) {
        Sprint sprint = findSprintById(sprintId);
        requireAdminOrOwner(sprint.getProject().getId(), principal.getId());

        if (sprint.getStatus() != Sprint.SprintStatus.ACTIVE) {
            throw new BadRequestException("Chỉ có thể hoàn thành sprint đang ở trạng thái ACTIVE");
        }

        sprint.setStatus(Sprint.SprintStatus.COMPLETED);
        sprint.setCompletedAt(LocalDateTime.now());

        sprint = sprintRepository.save(sprint);
        return SprintResponse.fromWithTasks(sprint);
    }

    // ========== DELETE ==========

    public void deleteSprint(UUID sprintId, UserPrincipal principal) {
        Sprint sprint = findSprintById(sprintId);
        requireAdminOrOwner(sprint.getProject().getId(), principal.getId());

        if (sprint.getStatus() != Sprint.SprintStatus.PLANNED) {
            throw new BadRequestException("Chỉ có thể xóa sprint đang ở trạng thái PLANNED");
        }

        sprintRepository.delete(sprint);
    }

    // ========== TASK MANAGEMENT ==========

    public SprintResponse addTaskToSprint(UUID sprintId, UUID taskId, UserPrincipal principal) {
        Sprint sprint = findSprintById(sprintId);
        projectService.findProjectWithAccess(sprint.getProject().getId(), principal.getId());

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        if (!task.getProject().getId().equals(sprint.getProject().getId())) {
            throw new BadRequestException("Task không thuộc dự án này");
        }

        sprint.getTasks().add(task);
        sprint = sprintRepository.save(sprint);
        return SprintResponse.fromWithTasks(sprint);
    }

    public SprintResponse removeTaskFromSprint(UUID sprintId, UUID taskId, UserPrincipal principal) {
        Sprint sprint = findSprintById(sprintId);
        projectService.findProjectWithAccess(sprint.getProject().getId(), principal.getId());

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        sprint.getTasks().remove(task);
        sprint = sprintRepository.save(sprint);
        return SprintResponse.fromWithTasks(sprint);
    }

    // ========== HELPERS ==========

    private Sprint findSprintById(UUID sprintId) {
        return sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint", "id", sprintId));
    }

    private void requireAdminOrOwner(UUID projectId, UUID userId) {
        ProjectMember.ProjectRole role = projectMemberRepository
                .findRoleByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ForbiddenException("Bạn không phải thành viên của dự án này"));
        if (role != ProjectMember.ProjectRole.OWNER && role != ProjectMember.ProjectRole.ADMIN) {
            throw new ForbiddenException("Bạn cần quyền ADMIN hoặc OWNER để thực hiện thao tác này");
        }
    }
}
