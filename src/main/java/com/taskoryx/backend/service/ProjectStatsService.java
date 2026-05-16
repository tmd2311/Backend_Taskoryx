package com.taskoryx.backend.service;

import com.taskoryx.backend.dto.response.stats.*;
import com.taskoryx.backend.entity.ProjectPermission;
import com.taskoryx.backend.entity.Sprint;
import com.taskoryx.backend.entity.Task;
import com.taskoryx.backend.entity.User;
import com.taskoryx.backend.repository.SprintRepository;
import com.taskoryx.backend.repository.TaskRepository;
import com.taskoryx.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectStatsService {

    private final TaskRepository taskRepository;
    private final SprintRepository sprintRepository;
    private final ProjectAuthorizationService projectAuthorizationService;
    private final ProjectService projectService;

    @Transactional(readOnly = true)
    public ProjectStatsResponse getStats(UUID projectId, UserPrincipal principal) {
        projectAuthorizationService.requirePermission(projectId, principal.getId(), ProjectPermission.REPORT_VIEW);
        var project = projectService.findProjectWithAccess(projectId, principal.getId());
        LocalDate today = LocalDate.now();

        return ProjectStatsResponse.builder()
                .projectId(project.getId())
                .projectName(project.getName())
                .taskOverview(buildTaskOverview(projectId, today))
                .completionTrend(buildCompletionTrend(projectId, today))
                .memberStats(buildMemberStats(projectId, today))
                .activeSprint(buildActiveSprintStats(projectId, today))
                .overdueTasks(buildOverdueTasks(projectId, today))
                .upcomingTasks(buildUpcomingTasks(projectId, today))
                .build();
    }

    private TaskOverviewStats buildTaskOverview(UUID projectId, LocalDate today) {
        long total      = taskRepository.countByProjectId(projectId);
        long todo       = taskRepository.countByProjectIdAndStatus(projectId, Task.TaskStatus.TODO);
        long inProgress = taskRepository.countByProjectIdAndStatus(projectId, Task.TaskStatus.IN_PROGRESS);
        long inReview   = taskRepository.countByProjectIdAndStatus(projectId, Task.TaskStatus.IN_REVIEW);
        long resolved   = taskRepository.countByProjectIdAndStatus(projectId, Task.TaskStatus.RESOLVED);
        long done       = taskRepository.countByProjectIdAndStatus(projectId, Task.TaskStatus.DONE);
        long cancelled  = taskRepository.countByProjectIdAndStatus(projectId, Task.TaskStatus.CANCELLED);
        long overdue    = taskRepository.findOverdueByProjectId(projectId, today).size();
        long low        = taskRepository.countByProjectIdAndPriority(projectId, Task.TaskPriority.LOW);
        long medium     = taskRepository.countByProjectIdAndPriority(projectId, Task.TaskPriority.MEDIUM);
        long high       = taskRepository.countByProjectIdAndPriority(projectId, Task.TaskPriority.HIGH);
        long urgent     = taskRepository.countByProjectIdAndPriority(projectId, Task.TaskPriority.URGENT);
        long unassigned = taskRepository.countUnassignedByProjectId(projectId);

        double completionRate = total == 0 ? 0.0
                : Math.round((done + resolved + cancelled) * 1000.0 / total) / 10.0;

        return TaskOverviewStats.builder()
                .total(total).todo(todo).inProgress(inProgress).inReview(inReview)
                .resolved(resolved).done(done).cancelled(cancelled).overdue(overdue)
                .completionRate(completionRate)
                .low(low).medium(medium).high(high).urgent(urgent)
                .unassigned(unassigned)
                .build();
    }

    private List<DailyCompletionStats> buildCompletionTrend(UUID projectId, LocalDate today) {
        LocalDate from = today.minusDays(29);
        List<Task> completed = taskRepository.findCompletedBetween(
                projectId, from.atStartOfDay(), today.atTime(LocalTime.MAX));
        List<Task> created = taskRepository.findCreatedBetween(
                projectId, from.atStartOfDay(), today.atTime(LocalTime.MAX));

        // Group by date
        Map<LocalDate, Long> completedByDay = completed.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getCompletedAt().toLocalDate(), Collectors.counting()));
        Map<LocalDate, Long> createdByDay = created.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getCreatedAt().toLocalDate(), Collectors.counting()));

        List<DailyCompletionStats> trend = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            LocalDate date = from.plusDays(i);
            trend.add(DailyCompletionStats.builder()
                    .date(date)
                    .completed(completedByDay.getOrDefault(date, 0L))
                    .created(createdByDay.getOrDefault(date, 0L))
                    .build());
        }
        return trend;
    }

    private List<MemberTaskStats> buildMemberStats(UUID projectId, LocalDate today) {
        List<Task> allTasks = taskRepository.findAllWithAssigneeByProjectId(projectId);

        Map<User, List<Task>> byAssignee = allTasks.stream()
                .collect(Collectors.groupingBy(Task::getAssignee));

        return byAssignee.entrySet().stream()
                .map(e -> {
                    User user = e.getKey();
                    List<Task> tasks = e.getValue();
                    long doneMember = tasks.stream()
                            .filter(t -> t.getStatus() == Task.TaskStatus.DONE
                                    || t.getStatus() == Task.TaskStatus.RESOLVED)
                            .count();
                    long inProgressMember = tasks.stream()
                            .filter(t -> t.getStatus() == Task.TaskStatus.IN_PROGRESS)
                            .count();
                    long overdueMember = tasks.stream()
                            .filter(t -> t.getDueDate() != null
                                    && t.getDueDate().isBefore(today)
                                    && t.getCompletedAt() == null)
                            .count();
                    return MemberTaskStats.builder()
                            .userId(user.getId())
                            .fullName(user.getFullName())
                            .avatarUrl(user.getAvatarUrl())
                            .total(tasks.size())
                            .inProgress(inProgressMember)
                            .done(doneMember)
                            .overdue(overdueMember)
                            .build();
                })
                .sorted(Comparator.comparingLong(MemberTaskStats::getTotal).reversed())
                .collect(Collectors.toList());
    }

    private ActiveSprintStats buildActiveSprintStats(UUID projectId, LocalDate today) {
        return sprintRepository.findActiveSprintByProjectId(projectId)
                .map(sprint -> {
                    long total      = taskRepository.countBySprintId(sprint.getId());
                    long done       = taskRepository.countDoneBySprintId(sprint.getId());
                    long inProgress = taskRepository.countInProgressBySprintId(sprint.getId());
                    long todo       = taskRepository.countTodoBySprintId(sprint.getId());
                    double rate     = total == 0 ? 0.0 : Math.round(done * 1000.0 / total) / 10.0;
                    long daysLeft   = sprint.getEndDate() != null
                            ? sprint.getEndDate().toEpochDay() - today.toEpochDay() : 0;

                    return ActiveSprintStats.builder()
                            .sprintId(sprint.getId())
                            .sprintName(sprint.getName())
                            .startDate(sprint.getStartDate())
                            .endDate(sprint.getEndDate())
                            .daysRemaining(daysLeft)
                            .totalTasks(total)
                            .doneTasks(done)
                            .inProgressTasks(inProgress)
                            .todoTasks(todo)
                            .completionRate(rate)
                            .build();
                })
                .orElse(null);
    }

    private List<TaskAlertItem> buildOverdueTasks(UUID projectId, LocalDate today) {
        return taskRepository.findOverdueByProjectId(projectId, today)
                .stream()
                .map(t -> TaskAlertItem.from(t, today))
                .collect(Collectors.toList());
    }

    private List<TaskAlertItem> buildUpcomingTasks(UUID projectId, LocalDate today) {
        return taskRepository.findUpcomingByProjectId(projectId, today, today.plusDays(7))
                .stream()
                .map(t -> TaskAlertItem.from(t, today))
                .collect(Collectors.toList());
    }
}
