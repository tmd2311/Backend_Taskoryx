package com.taskoryx.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskoryx.backend.dto.response.template.TemplateConfigDto;
import com.taskoryx.backend.entity.Project;
import com.taskoryx.backend.exception.BadRequestException;
import com.taskoryx.backend.exception.ResourceNotFoundException;
import com.taskoryx.backend.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectCapabilityService {

    public static final String MODULE_SPRINT = "SPRINT";
    public static final String MODULE_TIME_TRACKING = "TIME_TRACKING";
    public static final String MODULE_ATTACHMENT = "ATTACHMENT";

    private final ProjectRepository projectRepository;
    private final ProjectAuthorizationService projectAuthorizationService;
    private final ObjectMapper objectMapper;

    public void requireModule(UUID projectId, String module, UUID userId) {
        Project project = projectAuthorizationService.requireProjectAccess(projectId, userId);
        requireModule(project, module);
    }

    public void requireModule(Project project, String module) {
        if (!isModuleEnabled(project, module)) {
            throw new BadRequestException("Dự án này không bật module " + normalize(module));
        }
    }

    public boolean isModuleEnabled(UUID projectId, String module) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
        return isModuleEnabled(project, module);
    }

    public boolean isModuleEnabled(Project project, String module) {
        TemplateConfigDto config = getProjectConfig(project);
        if (config == null || config.getEnabledModules() == null || config.getEnabledModules().isEmpty()) {
            return true;
        }
        Set<String> enabled = config.getEnabledModules().stream()
                .map(this::normalize)
                .collect(Collectors.toSet());
        return enabled.contains(normalize(module));
    }

    public TemplateConfigDto getProjectConfig(UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
        return getProjectConfig(project);
    }

    public TemplateConfigDto getProjectConfig(Project project) {
        if (project.getProjectConfig() == null || project.getProjectConfig().isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(project.getProjectConfig(), TemplateConfigDto.class);
        } catch (Exception e) {
            throw new BadRequestException("Cấu hình project không hợp lệ");
        }
    }

    public TemplateConfigDto sanitizeProjectConfig(TemplateConfigDto config) {
        if (config == null) {
            return null;
        }

        List<String> enabledModules = normalizeModules(config.getEnabledModules());
        List<String> taskFields = normalizeTaskFields(config.getTaskFields());
        String boardType = normalizeBoardType(config.getBoardType());

        if ("SCRUM".equals(boardType) && !enabledModules.contains(MODULE_SPRINT)) {
            throw new BadRequestException("Project dùng boardType SCRUM phải bật module SPRINT");
        }
        if (taskFields.contains("sprint") && !enabledModules.contains(MODULE_SPRINT)) {
            throw new BadRequestException("Task field sprint yêu cầu module SPRINT");
        }
        if (taskFields.contains("attachments") && !enabledModules.contains(MODULE_ATTACHMENT)) {
            throw new BadRequestException("Task field attachments yêu cầu module ATTACHMENT");
        }

        return TemplateConfigDto.builder()
                .projectType(normalize(config.getProjectType()))
                .boardType(boardType)
                .enabledModules(enabledModules)
                .columns(config.getColumns())
                .taskFields(taskFields)
                .customTaskFields(config.getCustomTaskFields())
                .evaluationConfig(config.getEvaluationConfig())
                .build();
    }

    private String normalize(String module) {
        return module == null ? "" : module.trim()
                .toUpperCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
    }

    private List<String> normalizeModules(List<String> modules) {
        if (modules == null) {
            return List.of();
        }
        return modules.stream()
                .map(this::normalize)
                .filter(value -> !value.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    private List<String> normalizeTaskFields(List<String> taskFields) {
        if (taskFields == null) {
            return List.of();
        }
        return taskFields.stream()
                .map(this::canonicalTaskField)
                .filter(value -> !value.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    private String normalizeBoardType(String boardType) {
        return boardType == null || boardType.isBlank()
                ? null
                : boardType.trim().toUpperCase(Locale.ROOT);
    }

    private String canonicalTaskField(String fieldName) {
        String normalized = fieldName == null ? "" : fieldName.trim()
                .toLowerCase(Locale.ROOT)
                .replace(" ", "")
                .replace("_", "")
                .replace("-", "");
        return switch (normalized) {
            case "duedate" -> "dueDate";
            case "estimatedhours" -> "estimatedHours";
            case "actualhours" -> "actualHours";
            case "attachments" -> "attachments";
            case "assignee" -> "assignee";
            case "labels" -> "labels";
            case "priority" -> "priority";
            case "sprint" -> "sprint";
            default -> normalized;
        };
    }
}
