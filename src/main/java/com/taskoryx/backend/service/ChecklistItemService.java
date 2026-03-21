package com.taskoryx.backend.service;

import com.taskoryx.backend.dto.request.checklist.BulkCreateChecklistRequest;
import com.taskoryx.backend.dto.request.checklist.CreateChecklistItemRequest;
import com.taskoryx.backend.dto.request.checklist.UpdateChecklistItemRequest;
import com.taskoryx.backend.dto.response.checklist.ChecklistItemResponse;
import com.taskoryx.backend.dto.response.checklist.ChecklistSummaryResponse;
import com.taskoryx.backend.entity.ChecklistItem;
import com.taskoryx.backend.entity.Task;
import com.taskoryx.backend.entity.User;
import com.taskoryx.backend.exception.ResourceNotFoundException;
import com.taskoryx.backend.repository.ChecklistItemRepository;
import com.taskoryx.backend.repository.TaskRepository;
import com.taskoryx.backend.repository.UserRepository;
import com.taskoryx.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChecklistItemService {

    private final ChecklistItemRepository checklistItemRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;

    @Transactional(readOnly = true)
    public ChecklistSummaryResponse getChecklist(UUID taskId, UserPrincipal principal) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));
        projectService.findProjectWithAccess(task.getProject().getId(), principal.getId());

        return buildSummary(taskId);
    }

    @Transactional
    public ChecklistItemResponse addItem(UUID taskId, CreateChecklistItemRequest request, UserPrincipal principal) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));
        projectService.findProjectWithAccess(task.getProject().getId(), principal.getId());

        int nextPosition = resolveNextPosition(taskId, request.getPosition());

        ChecklistItem item = ChecklistItem.builder()
                .task(task)
                .content(request.getContent())
                .isChecked(false)
                .position(nextPosition)
                .build();

        return ChecklistItemResponse.from(checklistItemRepository.save(item));
    }

    @Transactional
    public ChecklistSummaryResponse bulkAdd(UUID taskId, BulkCreateChecklistRequest request, UserPrincipal principal) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));
        projectService.findProjectWithAccess(task.getProject().getId(), principal.getId());

        int startPosition = checklistItemRepository.findMaxPositionByTaskId(taskId)
                .map(max -> max + 1)
                .orElse(0);

        List<ChecklistItem> items = new ArrayList<>();
        int positionCounter = startPosition;
        for (String content : request.getItems()) {
            ChecklistItem item = ChecklistItem.builder()
                    .task(task)
                    .content(content)
                    .isChecked(false)
                    .position(positionCounter)
                    .build();
            items.add(item);
            positionCounter++;
        }

        checklistItemRepository.saveAll(items);

        return buildSummary(taskId);
    }

    @Transactional
    public ChecklistItemResponse updateItem(UUID itemId, UpdateChecklistItemRequest request, UserPrincipal principal) {
        ChecklistItem item = checklistItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("ChecklistItem", "id", itemId));
        projectService.findProjectWithAccess(item.getTask().getProject().getId(), principal.getId());

        if (request.getIsChecked() != null) {
            if (request.getIsChecked() && !item.isChecked()) {
                User currentUser = userRepository.findById(principal.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));
                item.check(currentUser);
            } else if (!request.getIsChecked() && item.isChecked()) {
                item.uncheck();
            }
        }

        if (request.getContent() != null && !request.getContent().isBlank()) {
            item.setContent(request.getContent());
        }

        if (request.getPosition() != null) {
            item.setPosition(request.getPosition());
        }

        return ChecklistItemResponse.from(checklistItemRepository.save(item));
    }

    @Transactional
    public void deleteItem(UUID itemId, UserPrincipal principal) {
        ChecklistItem item = checklistItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("ChecklistItem", "id", itemId));
        projectService.findProjectWithAccess(item.getTask().getProject().getId(), principal.getId());

        checklistItemRepository.delete(item);
    }

    @Transactional
    public void deleteAllItems(UUID taskId, UserPrincipal principal) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));
        projectService.findProjectWithAccess(task.getProject().getId(), principal.getId());

        checklistItemRepository.deleteByTaskId(taskId);
    }

    private ChecklistSummaryResponse buildSummary(UUID taskId) {
        List<ChecklistItem> items = checklistItemRepository.findByTaskIdOrderByPositionAsc(taskId);
        long totalItems = items.size();
        long checkedItems = items.stream().filter(ChecklistItem::isChecked).count();
        int completionPercentage = totalItems > 0
                ? (int) (checkedItems * 100 / totalItems)
                : 0;

        List<ChecklistItemResponse> itemResponses = items.stream()
                .map(ChecklistItemResponse::from)
                .collect(Collectors.toList());

        return ChecklistSummaryResponse.builder()
                .totalItems(totalItems)
                .checkedItems(checkedItems)
                .completionPercentage(completionPercentage)
                .items(itemResponses)
                .build();
    }

    private int resolveNextPosition(UUID taskId, Integer requestedPosition) {
        if (requestedPosition != null) {
            return requestedPosition;
        }
        return checklistItemRepository.findMaxPositionByTaskId(taskId)
                .map(max -> max + 1)
                .orElse(0);
    }
}
