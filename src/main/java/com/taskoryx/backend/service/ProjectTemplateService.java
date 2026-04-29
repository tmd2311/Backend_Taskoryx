package com.taskoryx.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskoryx.backend.dto.request.template.CreateProjectFromTemplateRequest;
import com.taskoryx.backend.dto.request.template.CreateTemplateRequest;
import com.taskoryx.backend.dto.response.project.ProjectResponse;
import com.taskoryx.backend.dto.response.template.ProjectTemplateResponse;
import com.taskoryx.backend.dto.response.template.TemplateColumnConfigDto;
import com.taskoryx.backend.dto.response.template.TemplateConfigDto;
import com.taskoryx.backend.dto.response.template.TemplateSprintConfigDto;
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

import java.time.LocalDate;
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
    private final SprintRepository sprintRepository;
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

        ProjectMember ownerMember = ProjectMember.builder()
                .project(project)
                .user(owner)
                .role("OWNER")
                .build();
        projectMemberRepository.save(ownerMember);

        // SCRUM template có sprint config → tạo sprints thay vì board đơn
        boolean isScrum = "SCRUM".equalsIgnoreCase(config != null ? config.getBoardType() : null);
        List<TemplateSprintConfigDto> sprintConfigs = config != null ? config.getSprints() : null;

        if (isScrum && sprintConfigs != null && !sprintConfigs.isEmpty()) {
            createSprintsFromTemplate(project, sprintConfigs);
        } else {
            // Kanban hoặc template không có sprint config → tạo board đơn như cũ
            createDefaultBoard(project, config);
        }

        ProjectResponse response = ProjectResponse.from(project);
        response.setCurrentUserRole("OWNER");
        return response;
    }

    private void createSprintsFromTemplate(Project project, List<TemplateSprintConfigDto> sprintConfigs) {
        LocalDate cursor = LocalDate.now();
        int boardPosition = 0;

        for (TemplateSprintConfigDto sc : sprintConfigs) {
            LocalDate startDate = cursor;
            LocalDate endDate = cursor.plusWeeks(sc.getDurationWeeks()).minusDays(1);

            Board sprintBoard = Board.builder()
                    .project(project)
                    .name(sc.getName())
                    .boardType(Board.BoardType.SCRUM)
                    .position(boardPosition++)
                    .isDefault(false)
                    .build();
            sprintBoard = boardRepository.save(sprintBoard);
            createStatusColumns(sprintBoard);

            Sprint sprint = Sprint.builder()
                    .project(project)
                    .board(sprintBoard)
                    .name(sc.getName())
                    .goal(sc.getGoal())
                    .startDate(startDate)
                    .endDate(endDate)
                    .status(Sprint.SprintStatus.PLANNED)
                    .build();
            sprintRepository.save(sprint);

            cursor = endDate.plusDays(1);
        }
    }

    private void createDefaultBoard(Project project, TemplateConfigDto config) {
        Board board = Board.builder()
                .project(project)
                .name("Main Board")
                .position(0)
                .boardType(resolveBoardType(config))
                .isDefault(true)
                .build();
        board = boardRepository.save(board);

        if (config != null) {
            try {
                List<TemplateColumnConfigDto> columns = config.getColumns();
                if (columns == null || columns.isEmpty()) {
                    createDefaultColumns(board);
                } else {
                    for (int i = 0; i < columns.size(); i++) {
                        TemplateColumnConfigDto col = columns.get(i);
                        boardColumnRepository.save(BoardColumn.builder()
                                .board(board)
                                .name(col.getName() != null ? col.getName() : "Column " + (i + 1))
                                .position(i)
                                .color(col.getColor() != null ? col.getColor() : "#6b7280")
                                .isCompleted(Boolean.TRUE.equals(col.getIsCompleted()))
                                .mappedStatus(col.getMappedStatus())
                                .taskLimit(col.getTaskLimit())
                                .build());
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse template columns config, using defaults", e);
                createDefaultColumns(board);
            }
        } else {
            createDefaultColumns(board);
        }
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
                    .position(i)
                    .mappedStatus(d.status())
                    .isCompleted(d.completed())
                    .build());
        }
    }

    private void createDefaultColumns(Board board) {
        String[] names  = {"Cần làm", "Đang làm", "Đã xong"};
        String[] colors = {"#6b7280", "#3b82f6", "#22c55e"};
        boolean[] done  = {false, false, true};
        for (int i = 0; i < names.length; i++) {
            boardColumnRepository.save(BoardColumn.builder()
                    .board(board).name(names[i]).position(i)
                    .color(colors[i]).isCompleted(done[i])
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
        if (config == null) return null;
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
        if (projectType == null || projectType.isBlank()) return null;
        return projectType.trim().toUpperCase().replace(' ', '_').replace('-', '_');
    }

    // ── Admin CRUD ────────────────────────────────────────────────────────────

    @Transactional
    public ProjectTemplateResponse createTemplate(CreateTemplateRequest request, UserPrincipal principal) {
        validateColumnsConfig(request.getColumnsConfig());
        User creator = userRepository.findById(principal.getId()).orElseThrow();
        ProjectTemplate template = ProjectTemplate.builder()
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .icon(request.getIcon())
                .color(request.getColor())
                .columnsConfig(request.getColumnsConfig())
                .isPublic(Boolean.TRUE.equals(request.getIsPublic()))
                .createdBy(creator)
                .build();
        return ProjectTemplateResponse.from(templateRepository.save(template));
    }

    @Transactional
    public ProjectTemplateResponse updateTemplate(UUID templateId, CreateTemplateRequest request) {
        ProjectTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template", "id", templateId));
        validateColumnsConfig(request.getColumnsConfig());
        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setCategory(request.getCategory());
        template.setIcon(request.getIcon());
        template.setColor(request.getColor());
        template.setColumnsConfig(request.getColumnsConfig());
        template.setPublic(Boolean.TRUE.equals(request.getIsPublic()));
        return ProjectTemplateResponse.from(templateRepository.save(template));
    }

    @Transactional
    public void deleteTemplate(UUID templateId) {
        if (!templateRepository.existsById(templateId)) {
            throw new ResourceNotFoundException("Template", "id", templateId);
        }
        templateRepository.deleteById(templateId);
    }

    @Transactional(readOnly = true)
    public ProjectTemplateResponse getTemplateById(UUID templateId) {
        return ProjectTemplateResponse.from(
                templateRepository.findById(templateId)
                        .orElseThrow(() -> new ResourceNotFoundException("Template", "id", templateId)));
    }

    private void validateColumnsConfig(String config) {
        if (config == null || config.isBlank()) {
            throw new BadRequestException("columnsConfig không được để trống");
        }
        try {
            objectMapper.readValue(config, TemplateConfigDto.class);
        } catch (Exception e) {
            throw new BadRequestException("columnsConfig không hợp lệ: " + e.getMessage());
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initDefaultTemplates() {
        // Dùng count để kiểm tra idempotent — nếu đã có ≥ 6 template hệ thống thì bỏ qua
        long systemCount = templateRepository.findAll().stream()
                .filter(t -> t.getCreatedBy() == null)
                .count();
        if (systemCount >= 6) return;

        log.info("Initializing default project templates...");

        // ── 1. Phát triển phần mềm — SCRUM 4 sprint ─────────────────────────
        upsertSystemTemplate("Phát triển phần mềm",
            "Template Agile/Scrum cho dự án phần mềm. Tự động tạo sẵn 4 sprint lên kế hoạch.",
            "Software", "💻", "#1976d2",
            "{\"projectType\":\"SOFTWARE\"," +
            "\"boardType\":\"SCRUM\"," +
            "\"enabledModules\":[\"SPRINT\",\"TIME_TRACKING\",\"ATTACHMENT\"]," +
            "\"taskFields\":[\"priority\",\"dueDate\",\"estimatedHours\",\"assignee\",\"labels\",\"sprint\"]," +
            "\"evaluationConfig\":{\"onTimeWeight\":40,\"completionWeight\":30,\"timeAccuracyWeight\":20,\"engagementWeight\":10}," +
            "\"sprints\":[" +
              "{\"name\":\"Sprint 1\",\"goal\":\"Thiết lập nền tảng và kiến trúc hệ thống\",\"durationWeeks\":2}," +
              "{\"name\":\"Sprint 2\",\"goal\":\"Xây dựng tính năng cốt lõi\",\"durationWeeks\":2}," +
              "{\"name\":\"Sprint 3\",\"goal\":\"Hoàn thiện và tích hợp các module\",\"durationWeeks\":2}," +
              "{\"name\":\"Sprint 4\",\"goal\":\"Kiểm thử, sửa lỗi và ra mắt sản phẩm\",\"durationWeeks\":2}" +
            "]}");

        // ── 2. Dự án Startup — SCRUM 3 sprint ───────────────────────────────
        upsertSystemTemplate("Dự án Startup",
            "Template cho startup cần di chuyển nhanh — 3 sprint tập trung vào MVP.",
            "Software", "🚀", "#7c3aed",
            "{\"projectType\":\"SOFTWARE\"," +
            "\"boardType\":\"SCRUM\"," +
            "\"enabledModules\":[\"SPRINT\",\"TIME_TRACKING\"]," +
            "\"taskFields\":[\"priority\",\"dueDate\",\"estimatedHours\",\"assignee\",\"labels\",\"sprint\"]," +
            "\"evaluationConfig\":{\"onTimeWeight\":40,\"completionWeight\":30,\"timeAccuracyWeight\":20,\"engagementWeight\":10}," +
            "\"sprints\":[" +
              "{\"name\":\"Sprint 1 — Discovery\",\"goal\":\"Xác định vấn đề, nghiên cứu người dùng và lên kế hoạch MVP\",\"durationWeeks\":2}," +
              "{\"name\":\"Sprint 2 — Build\",\"goal\":\"Xây dựng các tính năng cốt lõi của MVP\",\"durationWeeks\":3}," +
              "{\"name\":\"Sprint 3 — Launch\",\"goal\":\"Kiểm thử, sửa lỗi và ra mắt MVP\",\"durationWeeks\":1}" +
            "]}");

        // ── 3. Nghiên cứu & Phát triển — SCRUM 5 sprint ─────────────────────
        upsertSystemTemplate("Nghiên cứu & Phát triển",
            "Template cho dự án R&D dài hạn với 5 sprint theo từng giai đoạn nghiên cứu.",
            "Research", "🔬", "#0ea5e9",
            "{\"projectType\":\"RESEARCH\"," +
            "\"boardType\":\"SCRUM\"," +
            "\"enabledModules\":[\"SPRINT\",\"TIME_TRACKING\",\"ATTACHMENT\"]," +
            "\"taskFields\":[\"priority\",\"dueDate\",\"estimatedHours\",\"assignee\",\"labels\",\"sprint\"]," +
            "\"evaluationConfig\":{\"onTimeWeight\":30,\"completionWeight\":40,\"timeAccuracyWeight\":20,\"engagementWeight\":10}," +
            "\"sprints\":[" +
              "{\"name\":\"Giai đoạn 1 — Khảo sát\",\"goal\":\"Thu thập và phân tích tài liệu, xác định phạm vi nghiên cứu\",\"durationWeeks\":2}," +
              "{\"name\":\"Giai đoạn 2 — Thử nghiệm\",\"goal\":\"Xây dựng prototype và chạy thử nghiệm đầu tiên\",\"durationWeeks\":3}," +
              "{\"name\":\"Giai đoạn 3 — Phân tích\",\"goal\":\"Phân tích kết quả thử nghiệm và điều chỉnh hướng nghiên cứu\",\"durationWeeks\":2}," +
              "{\"name\":\"Giai đoạn 4 — Hoàn thiện\",\"goal\":\"Tối ưu mô hình / giải pháp và chuẩn bị báo cáo\",\"durationWeeks\":3}," +
              "{\"name\":\"Giai đoạn 5 — Trình bày\",\"goal\":\"Viết báo cáo, demo kết quả và bàn giao\",\"durationWeeks\":2}" +
            "]}");

        // ── 4. Dự án Marketing (KANBAN) ──────────────────────────────────────
        upsertSystemTemplate("Dự án Marketing",
            "Template cho chiến dịch marketing và truyền thông số.",
            "Marketing", "📣", "#e91e63",
            "{\"projectType\":\"MARKETING\"," +
            "\"boardType\":\"KANBAN\"," +
            "\"enabledModules\":[\"ATTACHMENT\",\"APPROVAL\"]," +
            "\"columns\":[" +
              "{\"name\":\"Ý tưởng\",\"color\":\"#6b7280\",\"isCompleted\":false,\"mappedStatus\":\"TODO\"}," +
              "{\"name\":\"Lên kế hoạch\",\"color\":\"#f59e0b\",\"isCompleted\":false,\"mappedStatus\":\"IN_PROGRESS\"}," +
              "{\"name\":\"Đang thực hiện\",\"color\":\"#3b82f6\",\"isCompleted\":false,\"mappedStatus\":\"IN_PROGRESS\"}," +
              "{\"name\":\"Đánh giá\",\"color\":\"#8b5cf6\",\"isCompleted\":false,\"mappedStatus\":\"IN_REVIEW\"}," +
              "{\"name\":\"Hoàn thành\",\"color\":\"#22c55e\",\"isCompleted\":true,\"mappedStatus\":\"DONE\"}]," +
            "\"taskFields\":[\"priority\",\"dueDate\",\"assignee\",\"labels\"]," +
            "\"evaluationConfig\":{\"onTimeWeight\":40,\"completionWeight\":30,\"timeAccuracyWeight\":20,\"engagementWeight\":10}}");

        // ── 5. Thiết kế UI/UX (KANBAN) ───────────────────────────────────────
        upsertSystemTemplate("Thiết kế UI/UX",
            "Template cho dự án thiết kế giao diện và trải nghiệm người dùng.",
            "Design", "🎨", "#9c27b0",
            "{\"projectType\":\"DESIGN\"," +
            "\"boardType\":\"KANBAN\"," +
            "\"enabledModules\":[\"ATTACHMENT\",\"APPROVAL\"]," +
            "\"columns\":[" +
              "{\"name\":\"Research\",\"color\":\"#6b7280\",\"isCompleted\":false,\"mappedStatus\":\"TODO\"}," +
              "{\"name\":\"Wireframe\",\"color\":\"#f59e0b\",\"isCompleted\":false,\"mappedStatus\":\"IN_PROGRESS\"}," +
              "{\"name\":\"Thiết kế\",\"color\":\"#3b82f6\",\"isCompleted\":false,\"mappedStatus\":\"IN_PROGRESS\"}," +
              "{\"name\":\"Review\",\"color\":\"#ef4444\",\"isCompleted\":false,\"mappedStatus\":\"IN_REVIEW\"}," +
              "{\"name\":\"Approved\",\"color\":\"#22c55e\",\"isCompleted\":true,\"mappedStatus\":\"DONE\"}]," +
            "\"taskFields\":[\"priority\",\"dueDate\",\"assignee\",\"attachments\"]," +
            "\"evaluationConfig\":{\"onTimeWeight\":40,\"completionWeight\":30,\"timeAccuracyWeight\":20,\"engagementWeight\":10}}");

        // ── 6. Quản lý sự kiện (KANBAN) ──────────────────────────────────────
        upsertSystemTemplate("Quản lý sự kiện",
            "Template cho tổ chức và vận hành sự kiện từ lên kế hoạch đến kết thúc.",
            "Event", "🎪", "#ff5722",
            "{\"projectType\":\"EVENT\"," +
            "\"boardType\":\"KANBAN\"," +
            "\"enabledModules\":[\"CHECKLIST\",\"ATTACHMENT\"]," +
            "\"columns\":[" +
              "{\"name\":\"Lên kế hoạch\",\"color\":\"#6b7280\",\"isCompleted\":false,\"mappedStatus\":\"TODO\"}," +
              "{\"name\":\"Chuẩn bị\",\"color\":\"#f59e0b\",\"isCompleted\":false,\"mappedStatus\":\"IN_PROGRESS\"}," +
              "{\"name\":\"Đang thực hiện\",\"color\":\"#3b82f6\",\"isCompleted\":false,\"mappedStatus\":\"IN_PROGRESS\"}," +
              "{\"name\":\"Đánh giá sau sự kiện\",\"color\":\"#8b5cf6\",\"isCompleted\":false,\"mappedStatus\":\"IN_REVIEW\"}," +
              "{\"name\":\"Hoàn tất\",\"color\":\"#22c55e\",\"isCompleted\":true,\"mappedStatus\":\"DONE\"}]," +
            "\"taskFields\":[\"priority\",\"dueDate\",\"assignee\"]," +
            "\"evaluationConfig\":{\"onTimeWeight\":40,\"completionWeight\":30,\"timeAccuracyWeight\":20,\"engagementWeight\":10}}");

        log.info("Default templates initialized successfully");
    }

    private void upsertSystemTemplate(String name, String description, String category,
                                       String icon, String color, String columnsConfig) {
        boolean exists = templateRepository.findAll().stream()
                .anyMatch(t -> t.getCreatedBy() == null && name.equals(t.getName()));
        if (exists) return;
        templateRepository.save(ProjectTemplate.builder()
                .name(name)
                .description(description)
                .category(category)
                .icon(icon)
                .color(color)
                .columnsConfig(columnsConfig)
                .isPublic(true)
                .build());
    }
}
