package com.taskoryx.backend.dto.response.sprint;

import com.taskoryx.backend.dto.response.task.TaskSummaryResponse;
import com.taskoryx.backend.entity.Sprint;
import com.taskoryx.backend.entity.Task;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SprintResponse {

    private UUID id;
    private UUID projectId;
    private String projectName;
    private String name;
    private String goal;
    private Sprint.SprintStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime completedAt;
    private int taskCount;
    private int completedTaskCount;
    private int inProgressTaskCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Populated for detail view; null for list view */
    private List<TaskSummaryResponse> tasks;

    public static SprintResponse from(Sprint sprint) {
        return from(sprint, false);
    }

    public static SprintResponse fromWithTasks(Sprint sprint) {
        return from(sprint, true);
    }

    private static SprintResponse from(Sprint sprint, boolean includeTasks) {
        int taskCount = sprint.getTasks().size();

        int completedTaskCount = (int) sprint.getTasks().stream()
                .filter(t -> t.getStatus() == Task.TaskStatus.DONE
                          || t.getStatus() == Task.TaskStatus.RESOLVED)
                .count();

        int inProgressTaskCount = (int) sprint.getTasks().stream()
                .filter(t -> t.getStatus() == Task.TaskStatus.IN_PROGRESS
                          || t.getStatus() == Task.TaskStatus.IN_REVIEW)
                .count();

        List<TaskSummaryResponse> taskList = includeTasks
                ? sprint.getTasks().stream()
                        .map(TaskSummaryResponse::from)
                        .collect(Collectors.toList())
                : null;

        return SprintResponse.builder()
                .id(sprint.getId())
                .projectId(sprint.getProject().getId())
                .projectName(sprint.getProject().getName())
                .name(sprint.getName())
                .goal(sprint.getGoal())
                .status(sprint.getStatus())
                .startDate(sprint.getStartDate())
                .endDate(sprint.getEndDate())
                .completedAt(sprint.getCompletedAt())
                .taskCount(taskCount)
                .completedTaskCount(completedTaskCount)
                .inProgressTaskCount(inProgressTaskCount)
                .createdAt(sprint.getCreatedAt())
                .updatedAt(sprint.getUpdatedAt())
                .tasks(taskList)
                .build();
    }
}
