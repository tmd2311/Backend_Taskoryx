package com.taskoryx.backend.service;

import com.taskoryx.backend.dto.request.task.AddDependencyRequest;
import com.taskoryx.backend.dto.response.task.TaskDependencyResponse;
import com.taskoryx.backend.entity.Task;
import com.taskoryx.backend.entity.TaskDependency;
import com.taskoryx.backend.exception.BadRequestException;
import com.taskoryx.backend.exception.ResourceNotFoundException;
import com.taskoryx.backend.repository.TaskDependencyRepository;
import com.taskoryx.backend.repository.TaskRepository;
import com.taskoryx.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskDependencyService {

    private final TaskDependencyRepository dependencyRepository;
    private final TaskRepository taskRepository;
    private final ProjectService projectService;

    @Transactional(readOnly = true)
    public List<TaskDependencyResponse> getDependencies(UUID taskId, UserPrincipal principal) {
        Task task = getTaskWithAccess(taskId, principal.getId());
        return dependencyRepository.findByTaskId(task.getId()).stream()
                .map(TaskDependencyResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskDependencyResponse addDependency(UUID taskId, AddDependencyRequest request, UserPrincipal principal) {
        Task task = getTaskWithAccess(taskId, principal.getId());
        UUID dependsOnTaskId = request.getDependsOnTaskId();

        // Không cho phép task phụ thuộc vào chính nó
        if (taskId.equals(dependsOnTaskId)) {
            throw new BadRequestException("Task không thể phụ thuộc vào chính nó");
        }

        Task dependsOnTask = taskRepository.findById(dependsOnTaskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", dependsOnTaskId));

        // Hai task phải cùng project
        if (!task.getProject().getId().equals(dependsOnTask.getProject().getId())) {
            throw new BadRequestException("Hai task phải thuộc cùng một project");
        }

        // Kiểm tra đã tồn tại dependency này chưa
        if (dependencyRepository.existsByTaskIdAndDependsOnTaskId(taskId, dependsOnTaskId)) {
            throw new BadRequestException("Dependency này đã tồn tại");
        }

        // ---- KIỂM TRA CIRCULAR DEPENDENCY bằng DFS ----
        // Thêm (task → dependsOnTask), kiểm tra: từ dependsOnTask có đi tới task không?
        if (hasCircularDependency(taskId, dependsOnTaskId)) {
            throw new BadRequestException(
                "Không thể thêm dependency: sẽ tạo ra vòng phụ thuộc (circular dependency)"
            );
        }

        TaskDependency dependency = TaskDependency.builder()
                .task(task)
                .dependsOnTask(dependsOnTask)
                .type(request.getType())
                .build();

        return TaskDependencyResponse.from(dependencyRepository.save(dependency));
    }

    @Transactional
    public void removeDependency(UUID taskId, UUID dependencyId, UserPrincipal principal) {
        getTaskWithAccess(taskId, principal.getId());

        TaskDependency dependency = dependencyRepository.findById(dependencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Dependency", "id", dependencyId));

        if (!dependency.getTask().getId().equals(taskId)) {
            throw new BadRequestException("Dependency không thuộc task này");
        }

        dependencyRepository.delete(dependency);
    }

    /**
     * DFS kiểm tra vòng phụ thuộc.
     *
     * Ý tưởng: ta sắp thêm cạnh (taskId → dependsOnTaskId).
     * Nếu từ dependsOnTaskId có thể đi ngược lại đến taskId
     * theo các cạnh dependency hiện có → sẽ tạo vòng → trả về true.
     *
     * Ví dụ: A→B, B→C, thêm C→A:
     *   - Bắt đầu từ A (dependsOnTaskId của C)
     *   - A phụ thuộc B → thăm B
     *   - B phụ thuộc C → thăm C (= taskId đang thêm) → VÒNG!
     */
    private boolean hasCircularDependency(UUID taskId, UUID dependsOnTaskId) {
        Set<UUID> visited = new HashSet<>();
        Deque<UUID> stack = new ArrayDeque<>();
        stack.push(dependsOnTaskId);

        while (!stack.isEmpty()) {
            UUID current = stack.pop();

            if (current.equals(taskId)) {
                return true; // Phát hiện vòng!
            }

            if (visited.contains(current)) {
                continue;
            }
            visited.add(current);

            // Lấy tất cả task mà current phụ thuộc vào và tiếp tục duyệt
            List<UUID> nextIds = dependencyRepository.findDependsOnTaskIdsByTaskId(current);
            stack.addAll(nextIds);
        }

        return false;
    }

    private Task getTaskWithAccess(UUID taskId, UUID userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));
        // Kiểm tra user có quyền truy cập project của task không
        projectService.findProjectWithAccess(task.getProject().getId(), userId);
        return task;
    }
}
