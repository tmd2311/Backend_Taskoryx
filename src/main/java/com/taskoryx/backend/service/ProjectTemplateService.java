package com.taskoryx.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskoryx.backend.dto.request.template.CreateProjectFromTemplateRequest;
import com.taskoryx.backend.dto.response.project.ProjectResponse;
import com.taskoryx.backend.dto.response.template.ProjectTemplateResponse;
import com.taskoryx.backend.dto.response.template.TemplateColumnConfigDto;
import com.taskoryx.backend.dto.response.template.TemplateConfigDto;
import com.taskoryx.backend.entity.*;
import com.taskoryx.backend.exception.BadRequestException;
import com.taskoryx.backend.exception.ResourceNotFoundException;
import com.taskoryx.backend.repository.*;
import com.taskoryx.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectTemplateService {

    private final ProjectTemplateRepository templateRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final BoardRepository boardRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final UserRepository userRepository;
    private final ProjectCapabilityService projectCapabilityService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<ProjectTemplateResponse> getTemplates(UserPrincipal principal) {
        return templateRepository.findAvailableTemplates(principal.getId())
                .stream()
                .map(ProjectTemplateResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectTemplateResponse> getPublicTemplates() {
        return templateRepository.findByIsPublicTrueOrderByNameAsc()
                .stream()
                .map(ProjectTemplateResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProjectResponse createProjectFromTemplate(UUID templateId,
                                                      CreateProjectFromTemplateRequest request,
                                                      UserPrincipal principal) {
        ProjectTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template", "id", templateId));

        if (projectRepository.existsByKey(request.getKey())) {
            throw new BadRequestException("Mã dự án '" + request.getKey() + "' đã tồn tại");
        }

        User owner = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        TemplateConfigDto config = parseTemplateConfig(template);

        // Create project
        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription() != null ? request.getDescription() : template.getDescription())
                .key(request.getKey())
                .owner(owner)
                .color(request.getColor() != null ? request.getColor() : (template.getColor() != null ? template.getColor() : "#1976d2"))
                .icon(template.getIcon())
                .projectType(resolveProjectType(template, config))
                .projectConfig(serializeProjectConfig(config))
                .configVersion(1)
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
                .isArchived(false)
                .build();
        project = projectRepository.save(project);

        // Add owner as member
        ProjectMember ownerMember = ProjectMember.builder()
                .project(project)
                .user(owner)
                .role("OWNER")
                .build();
        projectMemberRepository.save(ownerMember);

        // Create board from template columns
        Board board = Board.builder()
                .project(project)
                .name("Main Board")
                .position(0)
                .boardType(resolveBoardType(config))
                .isDefault(true)
                .build();
        board = boardRepository.save(board);

        // Parse and create columns from template config
        if (config != null) {
            try {
                List<TemplateColumnConfigDto> columns = config.getColumns();
                if (columns == null || columns.isEmpty()) {
                    createDefaultColumns(board);
                } else {
                    for (int i = 0; i < columns.size(); i++) {
                        TemplateColumnConfigDto col = columns.get(i);
                        BoardColumn column = BoardColumn.builder()
                                .board(board)
                                .name(col.getName() != null ? col.getName() : "Column " + (i + 1))
                                .position(i)
                                .color(col.getColor() != null ? col.getColor() : "#6b7280")
                                .isCompleted(Boolean.TRUE.equals(col.getIsCompleted()))
                                .mappedStatus(col.getMappedStatus())
                                .taskLimit(col.getTaskLimit())
                                .build();
                        boardColumnRepository.save(column);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse template columns config, using defaults", e);
                createDefaultColumns(board);
            }
        } else {
            createDefaultColumns(board);
        }

        ProjectResponse response = ProjectResponse.from(project);
        response.setCurrentUserRole("OWNER");
        return response;
    }

    private void createDefaultColumns(Board board) {
        String[] names = {"Cần làm", "Đang làm", "Đã xong"};
        String[] colors = {"#6b7280", "#3b82f6", "#22c55e"};
        boolean[] completed = {false, false, true};
        for (int i = 0; i < names.length; i++) {
            boardColumnRepository.save(BoardColumn.builder()
                    .board(board)
                    .name(names[i])
                    .position(i)
                    .color(colors[i])
                    .isCompleted(completed[i])
                    .build());
        }
    }

    private TemplateConfigDto parseTemplateConfig(ProjectTemplate template) {
        if (template.getColumnsConfig() == null || template.getColumnsConfig().isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(template.getColumnsConfig(), TemplateConfigDto.class);
        } catch (Exception e) {
            try {
                List<TemplateColumnConfigDto> cols = objectMapper.readValue(
                        template.getColumnsConfig(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, TemplateColumnConfigDto.class));
                return TemplateConfigDto.builder().columns(cols).build();
            } catch (Exception ignored) {
                log.warn("Failed to parse template config for template {}", template.getId());
                return null;
            }
        }
    }

    private String serializeProjectConfig(TemplateConfigDto config) {
        if (config == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(projectCapabilityService.sanitizeProjectConfig(config));
        } catch (Exception e) {
            throw new BadRequestException("Không thể lưu cấu hình project từ template");
        }
    }

    private String resolveProjectType(ProjectTemplate template, TemplateConfigDto config) {
        if (config != null && config.getProjectType() != null && !config.getProjectType().isBlank()) {
            return normalizeProjectType(config.getProjectType());
        }
        return normalizeProjectType(template.getCategory());
    }

    private Board.BoardType resolveBoardType(TemplateConfigDto config) {
        if (config == null || config.getBoardType() == null || config.getBoardType().isBlank()) {
            return Board.BoardType.KANBAN;
        }
        try {
            return Board.BoardType.valueOf(config.getBoardType().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Board.BoardType.KANBAN;
        }
    }

    private String normalizeProjectType(String projectType) {
        if (projectType == null || projectType.isBlank()) {
            return null;
        }
        return projectType.trim()
                .toUpperCase()
                .replace(' ', '_')
                .replace('-', '_');
    }

    // Initialize default system templates on startup
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initDefaultTemplates() {
        // Kiểm tra theo category để support re-seed sau khi đổi format
        boolean hasSoftware = templateRepository.findAll().stream()
                .anyMatch(t -> "Software".equals(t.getCategory()));
        if (hasSoftware) return;

        log.info("Initializing default project templates (extended config format)...");

        createSystemTemplate("Phát triển phần mềm",
            "Template cho dự án phát triển phần mềm theo Agile/Scrum",
            "Software", "💻", "#1976d2",
            "{\"projectType\":\"SOFTWARE\"," +
            "\"boardType\":\"SCRUM\"," +
            "\"enabledModules\":[\"SPRINT\",\"TIME_TRACKING\"]," +
            "\"columns\":[" +
              "{\"name\":\"Backlog\",\"color\":\"#6b7280\",\"isCompleted\":false}," +
              "{\"name\":\"Cần làm\",\"color\":\"#f59e0b\",\"isCompleted\":false}," +
              "{\"name\":\"Đang làm\",\"color\":\"#3b82f6\",\"isCompleted\":false}," +
              "{\"name\":\"Review\",\"color\":\"#8b5cf6\",\"isCompleted\":false}," +
              "{\"name\":\"Done\",\"color\":\"#22c55e\",\"isCompleted\":true}]," +
            "\"taskFields\":[\"priority\",\"dueDate\",\"estimatedHours\",\"assignee\",\"labels\",\"sprint\"]," +
            "\"evaluationConfig\":{\"onTimeWeight\":40,\"completionWeight\":30,\"timeAccuracyWeight\":20,\"engagementWeight\":10}}");

        createSystemTemplate("Dự án Marketing",
            "Template cho chiến dịch marketing và truyền thông",
            "Marketing", "📣", "#e91e63",
            "{\"projectType\":\"MARKETING\"," +
            "\"boardType\":\"KANBAN\"," +
            "\"enabledModules\":[\"ATTACHMENT\",\"APPROVAL\"]," +
            "\"columns\":[" +
              "{\"name\":\"Ý tưởng\",\"color\":\"#6b7280\",\"isCompleted\":false}," +
              "{\"name\":\"Lên kế hoạch\",\"color\":\"#f59e0b\",\"isCompleted\":false}," +
              "{\"name\":\"Đang thực hiện\",\"color\":\"#3b82f6\",\"isCompleted\":false}," +
              "{\"name\":\"Đánh giá\",\"color\":\"#8b5cf6\",\"isCompleted\":false}," +
              "{\"name\":\"Hoàn thành\",\"color\":\"#22c55e\",\"isCompleted\":true}]," +
            "\"taskFields\":[\"priority\",\"dueDate\",\"assignee\",\"labels\"]," +
            "\"evaluationConfig\":{\"onTimeWeight\":40,\"completionWeight\":30,\"timeAccuracyWeight\":20,\"engagementWeight\":10}}");

        createSystemTemplate("Thiết kế UI/UX",
            "Template cho dự án thiết kế giao diện và trải nghiệm người dùng",
            "Design", "🎨", "#9c27b0",
            "{\"projectType\":\"DESIGN\"," +
            "\"boardType\":\"KANBAN\"," +
            "\"enabledModules\":[\"ATTACHMENT\",\"APPROVAL\",\"REVIEW\"]," +
            "\"columns\":[" +
              "{\"name\":\"Research\",\"color\":\"#6b7280\",\"isCompleted\":false}," +
              "{\"name\":\"Wireframe\",\"color\":\"#f59e0b\",\"isCompleted\":false}," +
              "{\"name\":\"Design\",\"color\":\"#3b82f6\",\"isCompleted\":false}," +
              "{\"name\":\"Review\",\"color\":\"#ef4444\",\"isCompleted\":false}," +
              "{\"name\":\"Approved\",\"color\":\"#22c55e\",\"isCompleted\":true}]," +
            "\"taskFields\":[\"priority\",\"dueDate\",\"assignee\",\"attachments\"]," +
            "\"evaluationConfig\":{\"onTimeWeight\":40,\"completionWeight\":30,\"timeAccuracyWeight\":20,\"engagementWeight\":10}}");

        createSystemTemplate("Quản lý sự kiện",
            "Template cho tổ chức và quản lý sự kiện",
            "Event", "🎪", "#ff5722",
            "{\"projectType\":\"EVENT\"," +
            "\"boardType\":\"KANBAN\"," +
            "\"enabledModules\":[\"CHECKLIST\",\"MILESTONE\"]," +
            "\"columns\":[" +
              "{\"name\":\"Lên kế hoạch\",\"color\":\"#6b7280\",\"isCompleted\":false}," +
              "{\"name\":\"Chuẩn bị\",\"color\":\"#f59e0b\",\"isCompleted\":false}," +
              "{\"name\":\"Đang thực hiện\",\"color\":\"#3b82f6\",\"isCompleted\":false}," +
              "{\"name\":\"Đã xong\",\"color\":\"#22c55e\",\"isCompleted\":true}]," +
            "\"taskFields\":[\"priority\",\"dueDate\",\"assignee\"]," +
            "\"evaluationConfig\":{\"onTimeWeight\":40,\"completionWeight\":30,\"timeAccuracyWeight\":20,\"engagementWeight\":10}}");

        log.info("Default templates initialized successfully");
    }

    private void createSystemTemplate(String name, String description, String category,
                                      String icon, String color, String columnsConfig) {
        ProjectTemplate template = ProjectTemplate.builder()
                .name(name)
                .description(description)
                .category(category)
                .icon(icon)
                .color(color)
                .columnsConfig(columnsConfig)
                .isPublic(true)
                .build();
        templateRepository.save(template);
    }
}
