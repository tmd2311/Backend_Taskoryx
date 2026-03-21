package com.taskoryx.backend.service;

import com.taskoryx.backend.dto.request.timetracking.CreateTimeEntryRequest;
import com.taskoryx.backend.dto.request.timetracking.UpdateTimeEntryRequest;
import com.taskoryx.backend.dto.response.timetracking.TimeTrackingResponse;
import com.taskoryx.backend.entity.Task;
import com.taskoryx.backend.entity.TimeTracking;
import com.taskoryx.backend.entity.User;
import com.taskoryx.backend.exception.ForbiddenException;
import com.taskoryx.backend.exception.ResourceNotFoundException;
import com.taskoryx.backend.repository.TaskRepository;
import com.taskoryx.backend.repository.TimeTrackingRepository;
import com.taskoryx.backend.repository.UserRepository;
import com.taskoryx.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimeTrackingService {

    private final TimeTrackingRepository timeTrackingRepository;
    private final TaskRepository taskRepository;
    private final ProjectService projectService;
    private final UserRepository userRepository;

    @Transactional
    public TimeTrackingResponse logTime(CreateTimeEntryRequest request, UserPrincipal principal) {
        Task task = taskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", request.getTaskId()));

        // Kiểm tra quyền truy cập vào project
        projectService.findProjectWithAccess(task.getProject().getId(), principal.getId());

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        LocalDate workDate = request.getWorkDate() != null ? request.getWorkDate() : LocalDate.now();

        TimeTracking entry = TimeTracking.builder()
                .task(task)
                .user(user)
                .hours(request.getHours())
                .description(request.getDescription())
                .workDate(workDate)
                .build();

        entry = timeTrackingRepository.save(entry);

        // Cập nhật actualHours của task
        updateTaskActualHours(task);

        return TimeTrackingResponse.from(entry);
    }

    @Transactional(readOnly = true)
    public List<TimeTrackingResponse> getTimeEntriesForTask(UUID taskId, UserPrincipal principal) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        // Kiểm tra quyền truy cập vào project
        projectService.findProjectWithAccess(task.getProject().getId(), principal.getId());

        return timeTrackingRepository.findByTaskId(taskId)
                .stream()
                .map(TimeTrackingResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<TimeTrackingResponse> getMyTimeEntries(UserPrincipal principal, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("workDate").descending());
        return timeTrackingRepository.findByUserIdOrderByWorkDateDesc(principal.getId(), pageable)
                .map(TimeTrackingResponse::from);
    }

    @Transactional(readOnly = true)
    public List<TimeTrackingResponse> getTimeEntriesByDateRange(UUID userId, LocalDate start, LocalDate end,
                                                                 UserPrincipal principal) {
        // Chỉ được xem của mình hoặc admin có thể xem của người khác
        UUID targetUserId = userId != null ? userId : principal.getId();

        return timeTrackingRepository.findByUserIdAndWorkDateBetween(targetUserId, start, end)
                .stream()
                .map(TimeTrackingResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public TimeTrackingResponse updateTimeEntry(UUID entryId, UpdateTimeEntryRequest request,
                                                 UserPrincipal principal) {
        TimeTracking entry = timeTrackingRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("TimeTracking", "id", entryId));

        // Chỉ người tạo entry mới được sửa
        if (!entry.getUser().getId().equals(principal.getId())) {
            throw new ForbiddenException("Bạn không có quyền chỉnh sửa mục ghi thời gian này");
        }

        if (request.getHours() != null) {
            entry.setHours(request.getHours());
        }
        if (request.getDescription() != null) {
            entry.setDescription(request.getDescription());
        }
        if (request.getWorkDate() != null) {
            entry.setWorkDate(request.getWorkDate());
        }

        entry = timeTrackingRepository.save(entry);

        // Cập nhật actualHours của task
        updateTaskActualHours(entry.getTask());

        return TimeTrackingResponse.from(entry);
    }

    @Transactional
    public void deleteTimeEntry(UUID entryId, UserPrincipal principal) {
        TimeTracking entry = timeTrackingRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("TimeTracking", "id", entryId));

        // Chỉ người tạo entry mới được xóa
        if (!entry.getUser().getId().equals(principal.getId())) {
            throw new ForbiddenException("Bạn không có quyền xóa mục ghi thời gian này");
        }

        Task task = entry.getTask();
        timeTrackingRepository.delete(entry);

        // Cập nhật actualHours của task sau khi xóa
        updateTaskActualHours(task);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTaskTotalHours(UUID taskId, UserPrincipal principal) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        projectService.findProjectWithAccess(task.getProject().getId(), principal.getId());

        return timeTrackingRepository.sumHoursByTaskId(taskId)
                .orElse(BigDecimal.ZERO);
    }

    // ========== HELPERS ==========

    private void updateTaskActualHours(Task task) {
        BigDecimal totalHours = timeTrackingRepository.sumHoursByTaskId(task.getId())
                .orElse(BigDecimal.ZERO);
        task.setActualHours(totalHours);
        taskRepository.save(task);
    }
}
