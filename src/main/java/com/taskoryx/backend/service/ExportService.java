package com.taskoryx.backend.service;

import com.taskoryx.backend.dto.request.export.ExportFilter;
import com.taskoryx.backend.entity.Project;
import com.taskoryx.backend.entity.ProjectMember;
import com.taskoryx.backend.entity.Sprint;
import com.taskoryx.backend.entity.Task;
import com.taskoryx.backend.entity.ProjectPermission;
import com.taskoryx.backend.repository.ProjectMemberRepository;
import com.taskoryx.backend.repository.ProjectRepository;
import com.taskoryx.backend.repository.SprintRepository;
import com.taskoryx.backend.repository.TaskRepository;
import com.taskoryx.backend.security.UserPrincipal;
import com.taskoryx.backend.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final SprintRepository sprintRepository;
    private final ProjectAuthorizationService projectAuthorizationService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── Màu sắc brand ──────────────────────────────────────────────────────────
    private static final byte[] COLOR_HEADER_BG   = hex("1E3A5F"); // xanh navy đậm
    private static final byte[] COLOR_SECTION_BG  = hex("2E86C1"); // xanh dương
    private static final byte[] COLOR_ALT_ROW      = hex("EBF5FB"); // xanh nhạt xen kẽ
    private static final byte[] COLOR_OVERDUE      = hex("FDEDEC"); // đỏ nhạt — quá hạn
    private static final byte[] COLOR_DONE         = hex("EAFAF1"); // xanh nhạt — hoàn thành
    private static final byte[] COLOR_WARN         = hex("FEF9E7"); // vàng nhạt — cảnh báo
    private static final byte[] COLOR_ACCENT       = hex("1ABC9C"); // xanh lá nhấn
    private static final byte[] COLOR_WHITE        = hex("FFFFFF");
    private static final byte[] COLOR_DARK_TEXT    = hex("1C2833");
    private static final byte[] COLOR_GRAY_BORDER  = hex("BDC3C7");

    @Transactional(readOnly = true)
    public byte[] exportProjectTasksToExcel(UUID projectId, UserPrincipal principal, ExportFilter filter) {
        projectAuthorizationService.requirePermission(projectId, principal.getId(), ProjectPermission.REPORT_VIEW);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        // Load toàn bộ task của project, sau đó lọc in-memory theo filter
        List<Task> allTasks = taskRepository.findByProjectId(projectId, PageRequest.of(0, 10000)).getContent();
        List<Task> filteredTasks = applyFilter(allTasks, filter);

        List<ProjectMember> members = projectMemberRepository.findByProjectId(projectId);
        // Nếu lọc theo assignee thì chỉ giữ member đó trong sheet members
        List<ProjectMember> filteredMembers = (filter.getAssigneeId() != null)
                ? members.stream().filter(m -> filter.getAssigneeId().equals(m.getUser().getId())).collect(Collectors.toList())
                : members;

        List<Sprint> sprints = sprintRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        List<Sprint> filteredSprints = (filter.getSprintIds() != null && !filter.getSprintIds().isEmpty())
                ? sprints.stream().filter(s -> filter.getSprintIds().contains(s.getId())).collect(Collectors.toList())
                : sprints;

        LocalDate today = LocalDate.now();

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            StyleKit sk = new StyleKit(wb);

            if (filter.hasSheet(ExportFilter.SHEET_OVERVIEW))
                buildSheet1Overview(wb, sk, project, filteredTasks, filteredMembers, filteredSprints, today, filter);
            if (filter.hasSheet(ExportFilter.SHEET_TASKS))
                buildSheet2TaskList(wb, sk, filteredTasks, today);
            if (filter.hasSheet(ExportFilter.SHEET_MEMBERS))
                buildSheet3MemberSummary(wb, sk, filteredTasks, filteredMembers);
            if (filter.hasSheet(ExportFilter.SHEET_SPRINTS))
                buildSheet4SprintSummary(wb, sk, filteredTasks, filteredSprints);
            if (filter.hasSheet(ExportFilter.SHEET_OVERDUE))
                buildSheet5Overdue(wb, sk, filteredTasks, today);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Không thể tạo file Excel", e);
        }
    }

    // ── Áp dụng filter lên danh sách task ─────────────────────────────────────
    private List<Task> applyFilter(List<Task> tasks, ExportFilter f) {
        return tasks.stream()
                .filter(t -> f.getSprintIds() == null || f.getSprintIds().isEmpty()
                        || (t.getSprint() != null && f.getSprintIds().contains(t.getSprint().getId())))
                .filter(t -> f.getAssigneeId() == null
                        || (t.getAssignee() != null && f.getAssigneeId().equals(t.getAssignee().getId())))
                .filter(t -> f.getStatuses() == null || f.getStatuses().isEmpty()
                        || f.getStatuses().contains(t.getStatus()))
                .filter(t -> f.getPriorities() == null || f.getPriorities().isEmpty()
                        || f.getPriorities().contains(t.getPriority()))
                .filter(t -> {
                    if (f.getDateFrom() == null && f.getDateTo() == null) return true;
                    // Dùng dueDate làm cơ sở; fallback về createdAt nếu không có dueDate
                    LocalDate ref = t.getDueDate() != null ? t.getDueDate()
                            : (t.getCreatedAt() != null ? t.getCreatedAt().toLocalDate() : null);
                    if (ref == null) return true; // task không có ngày → không lọc
                    if (f.getDateFrom() != null && ref.isBefore(f.getDateFrom())) return false;
                    if (f.getDateTo()   != null && ref.isAfter(f.getDateTo()))    return false;
                    return true;
                })
                .collect(Collectors.toList());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SHEET 1 — TỔNG QUAN DỰ ÁN
    // ══════════════════════════════════════════════════════════════════════════
    private void buildSheet1Overview(XSSFWorkbook wb, StyleKit sk, Project project,
                                     List<Task> tasks, List<ProjectMember> members,
                                     List<Sprint> sprints, LocalDate today, ExportFilter filter) {
        Sheet sheet = wb.createSheet("📊 Tổng Quan");

        int r = 0;

        // ── Tiêu đề chính ──
        r = writeBanner(sheet, sk, r, "BÁO CÁO TỔNG QUAN DỰ ÁN: " + project.getName().toUpperCase(), 5);
        writeSubBanner(sheet, sk, r++, "Mã dự án: " + project.getKey()
                + "   |   Xuất ngày: " + today.format(DATE_FMT)
                + "   |   Thành viên: " + members.size(), 5);

        // ── Dòng bộ lọc đang áp dụng ──
        String filterDesc = buildFilterDescription(filter);
        if (!filterDesc.isEmpty()) {
            writeSubBanner(sheet, sk, r++, "Bộ lọc: " + filterDesc, 5);
        }
        r++;

        // ── Thống kê tổng số task theo trạng thái ──
        long total   = tasks.size();
        long todo    = tasks.stream().filter(t -> t.getStatus() == Task.TaskStatus.TODO).count();
        long inProg  = tasks.stream().filter(t -> t.getStatus() == Task.TaskStatus.IN_PROGRESS).count();
        long inReview= tasks.stream().filter(t -> t.getStatus() == Task.TaskStatus.IN_REVIEW).count();
        long done    = tasks.stream().filter(t -> t.getStatus() == Task.TaskStatus.DONE
                                              || t.getStatus() == Task.TaskStatus.RESOLVED).count();
        long cancelled=tasks.stream().filter(t -> t.getStatus() == Task.TaskStatus.CANCELLED).count();
        long overdue = tasks.stream().filter(t ->
                t.getDueDate() != null && t.getDueDate().isBefore(today) && t.getCompletedAt() == null).count();
        long noAssignee = tasks.stream().filter(t -> t.getAssignee() == null).count();

        double doneRate = total > 0 ? (double) done / total * 100 : 0;
        double overdueRate = total > 0 ? (double) overdue / total * 100 : 0;

        writeSectionHeader(sheet, sk, r++, "A. THỐNG KÊ TASK THEO TRẠNG THÁI", 5);
        writeKpiRow(sheet, sk, r++, "Tổng số task", total, "Hoàn thành (DONE + RESOLVED)", done);
        writeKpiRow(sheet, sk, r++, "Cần làm (TODO)", todo, "Đang làm (IN_PROGRESS)", inProg);
        writeKpiRow(sheet, sk, r++, "Đang review (IN_REVIEW)", inReview, "Đã hủy (CANCELLED)", cancelled);
        writeKpiRow(sheet, sk, r++, "Quá hạn", overdue, "Chưa giao (unassigned)", noAssignee);
        r++;

        writeSectionHeader(sheet, sk, r++, "B. TỶ LỆ TIẾN ĐỘ", 5);
        writeRateRow(sheet, sk, r++, wb, "Tỷ lệ hoàn thành", doneRate, done, total);
        writeRateRow(sheet, sk, r++, wb, "Tỷ lệ quá hạn",   overdueRate, overdue, total);
        r++;

        // ── Thống kê theo mức độ ưu tiên ──
        writeSectionHeader(sheet, sk, r++, "C. PHÂN BỔ THEO ƯU TIÊN", 5);
        Row hdr = sheet.createRow(r++);
        writeCell(hdr, 0, "Mức độ ưu tiên", sk.subHeader);
        writeCell(hdr, 1, "Số lượng",         sk.subHeader);
        writeCell(hdr, 2, "Tỷ lệ (%)",        sk.subHeader);
        writeCell(hdr, 3, "Đã xong",          sk.subHeader);
        writeCell(hdr, 4, "% Xong",           sk.subHeader);

        int priorityRow = r;
        for (Task.TaskPriority p : Task.TaskPriority.values()) {
            long cnt   = tasks.stream().filter(t -> t.getPriority() == p).count();
            long doneP = tasks.stream().filter(t -> t.getPriority() == p
                    && (t.getStatus() == Task.TaskStatus.DONE || t.getStatus() == Task.TaskStatus.RESOLVED)).count();
            double pct = total > 0 ? cnt * 100.0 / total : 0;
            double donePct = cnt > 0 ? doneP * 100.0 / cnt : 0;
            Row row = sheet.createRow(r++);
            CellStyle cs = (r % 2 == 0) ? sk.altRow : sk.dataCenter;
            writeCell(row, 0, priorityLabel(p), cs);
            writeNumCell(row, 1, cnt, cs);
            writePercentCell(row, 2, pct, wb, cs);
            writeNumCell(row, 3, doneP, cs);
            writePercentCell(row, 4, donePct, wb, cs);
        }
        r++;

        // ── Sprint summary ──
        if (!sprints.isEmpty()) {
            writeSectionHeader(sheet, sk, r++, "D. DANH SÁCH SPRINT", 5);
            Row spHdr = sheet.createRow(r++);
            writeCell(spHdr, 0, "Tên sprint",   sk.subHeader);
            writeCell(spHdr, 1, "Trạng thái",   sk.subHeader);
            writeCell(spHdr, 2, "Ngày bắt đầu", sk.subHeader);
            writeCell(spHdr, 3, "Ngày kết thúc",sk.subHeader);
            writeCell(spHdr, 4, "Tổng task",    sk.subHeader);

            for (Sprint sp : sprints) {
                long spTasks = tasks.stream().filter(t -> sp.equals(t.getSprint())).count();
                Row row = sheet.createRow(r++);
                CellStyle cs = (r % 2 == 0) ? sk.altRow : sk.dataCenter;
                writeCell(row, 0, sp.getName(), cs);
                writeCell(row, 1, sprintStatusLabel(sp.getStatus()), cs);
                writeCell(row, 2, sp.getStartDate() != null ? sp.getStartDate().format(DATE_FMT) : "—", cs);
                writeCell(row, 3, sp.getEndDate()   != null ? sp.getEndDate().format(DATE_FMT)   : "—", cs);
                writeNumCell(row, 4, spTasks, cs);
            }
        }

        // ── Ghi chú cuối ──
        r++;
        Row note = sheet.createRow(r);
        writeCell(note, 0, "* Báo cáo được tạo tự động. Dữ liệu chính xác tại thời điểm xuất file.", sk.italic);
        sheet.addMergedRegion(new CellRangeAddress(r, r, 0, 4));

        autoSizeColumns(sheet, 5);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SHEET 2 — DANH SÁCH TASK CHI TIẾT (có AutoFilter, freeze pane)
    // ══════════════════════════════════════════════════════════════════════════
    private void buildSheet2TaskList(XSSFWorkbook wb, StyleKit sk, List<Task> tasks, LocalDate today) {
        Sheet sheet = wb.createSheet("📋 Danh Sách Task");

        String[] headers = {
            "Task Key", "Cấp", "Tiêu đề", "Trạng thái", "Ưu tiên",
            "Người thực hiện", "Người báo cáo", "Sprint",
            "Ngày bắt đầu", "Deadline", "Giờ ước tính (h)", "Giờ thực tế (h)",
            "Ngày tạo", "Ngày hoàn thành"
        };

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(sk.tableHeader);
        }

        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, headers.length - 1));

        List<Task> sorted = tasks.stream().sorted(Comparator
                .comparing((Task t) -> {
                    boolean overdue = t.getDueDate() != null && t.getDueDate().isBefore(today) && t.getCompletedAt() == null;
                    return overdue ? 0 : 1;
                })
                .thenComparingInt(t -> statusOrder(t.getStatus()))
                .thenComparingInt(t -> priorityOrder(t.getPriority())))
                .collect(Collectors.toList());

        int rowNum = 1;
        for (Task task : sorted) {
            Row row = sheet.createRow(rowNum);
            boolean overdue = task.getDueDate() != null
                    && task.getDueDate().isBefore(today)
                    && task.getCompletedAt() == null;
            boolean isDone = task.getStatus() == Task.TaskStatus.DONE
                    || task.getStatus() == Task.TaskStatus.RESOLVED;

            CellStyle rowStyle = isDone   ? sk.doneRow
                               : overdue  ? sk.overdueRow
                               : (rowNum % 2 == 0) ? sk.altRow : sk.dataLeft;

            writeCell(row, 0,  task.getTaskKey() != null ? task.getTaskKey() : "", sk.taskKey);
            writeCell(row, 1,  "Cấp " + task.getDepth(), rowStyle);
            writeCell(row, 2,  task.getTitle(), rowStyle);
            writeCell(row, 3,  statusLabel(task.getStatus()), rowStyle);
            writeCell(row, 4,  priorityLabel(task.getPriority()), rowStyle);
            writeCell(row, 5,  task.getAssignee()  != null ? task.getAssignee().getFullName()         : "—", rowStyle);
            writeCell(row, 6,  task.getReporter()  != null ? task.getReporter().getFullName()          : "—", rowStyle);
            writeCell(row, 7,  task.getSprint()    != null ? task.getSprint().getName()                : "Backlog", rowStyle);
            writeCell(row, 8,  task.getStartDate() != null ? task.getStartDate().format(DATE_FMT)      : "—", rowStyle);
            writeCell(row, 9,  task.getDueDate()   != null ? task.getDueDate().format(DATE_FMT)        : "—", rowStyle);
            writeNumCell(row, 10, task.getEstimatedHours() != null ? task.getEstimatedHours().doubleValue() : 0, rowStyle);
            writeNumCell(row, 11, task.getActualHours()    != null ? task.getActualHours().doubleValue()    : 0, rowStyle);
            writeCell(row, 12, task.getCreatedAt()   != null ? task.getCreatedAt().format(DATETIME_FMT)  : "—", rowStyle);
            writeCell(row, 13, task.getCompletedAt() != null ? task.getCompletedAt().format(DATETIME_FMT) : "—", rowStyle);

            rowNum++;
        }

        // Tổng hợp cuối bảng
        int sumRow = rowNum + 1;
        Row totalLabelRow = sheet.createRow(sumRow);
        writeCell(totalLabelRow, 0, "TỔNG CỘNG", sk.subHeader);
        writeCell(totalLabelRow, 1, tasks.size() + " task", sk.subHeader);

        Row sumFormRow = sheet.createRow(sumRow + 1);
        writeCell(sumFormRow, 9, "Tổng giờ:", sk.subHeader);
        // dataStart=2 vì Excel row index bắt đầu từ 1, header ở row 1 → data từ row 2
        sumFormRow.createCell(10).setCellFormula(String.format("SUM(K2:K%d)", rowNum));
        sumFormRow.getCell(10).setCellStyle(sk.numBold);
        sumFormRow.createCell(11).setCellFormula(String.format("SUM(L2:L%d)", rowNum));
        sumFormRow.getCell(11).setCellStyle(sk.numBold);

        // Auto-size sau khi có đủ data
        autoSizeColumns(sheet, headers.length);
        // Giới hạn cột tiêu đề không quá rộng
        sheet.setColumnWidth(2, Math.min(sheet.getColumnWidth(2), 15000));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SHEET 3 — THỐNG KÊ THEO THÀNH VIÊN
    // ══════════════════════════════════════════════════════════════════════════
    private void buildSheet3MemberSummary(XSSFWorkbook wb, StyleKit sk,
                                          List<Task> tasks, List<ProjectMember> members) {
        Sheet sheet = wb.createSheet("👥 Thành Viên");

        writeBanner(sheet, sk, 0, "THỐNG KÊ CÔNG VIỆC THEO THÀNH VIÊN", 10);

        String[] headers = {
            "Họ tên", "Email", "Vai trò", "Tổng task", "TODO",
            "Đang làm", "Hoàn thành", "Quá hạn", "Giờ ước tính", "Giờ thực tế"
        };
        Row hdr = sheet.createRow(2);
        for (int i = 0; i < headers.length; i++) {
            Cell c = hdr.createCell(i); c.setCellValue(headers[i]); c.setCellStyle(sk.tableHeader);
        }
        sheet.createFreezePane(0, 3);
        sheet.setAutoFilter(new CellRangeAddress(2, 2, 0, headers.length - 1));

        LocalDate today = LocalDate.now();
        int r = 3;
        for (ProjectMember pm : members) {
            UUID uid = pm.getUser().getId();
            List<Task> memberTasks = tasks.stream()
                    .filter(t -> t.getAssignee() != null && uid.equals(t.getAssignee().getId()))
                    .collect(Collectors.toList());

            long total    = memberTasks.size();
            long todoC    = memberTasks.stream().filter(t -> t.getStatus() == Task.TaskStatus.TODO).count();
            long inProg   = memberTasks.stream().filter(t -> t.getStatus() == Task.TaskStatus.IN_PROGRESS
                                                          || t.getStatus() == Task.TaskStatus.IN_REVIEW).count();
            long doneC    = memberTasks.stream().filter(t -> t.getStatus() == Task.TaskStatus.DONE
                                                          || t.getStatus() == Task.TaskStatus.RESOLVED).count();
            long overdueC = memberTasks.stream().filter(t ->
                    t.getDueDate() != null && t.getDueDate().isBefore(today) && t.getCompletedAt() == null).count();
            double estHrs = memberTasks.stream()
                    .mapToDouble(t -> t.getEstimatedHours() != null ? t.getEstimatedHours().doubleValue() : 0).sum();
            double actHrs = memberTasks.stream()
                    .mapToDouble(t -> t.getActualHours() != null ? t.getActualHours().doubleValue() : 0).sum();

            Row row = sheet.createRow(r);
            CellStyle cs = (r % 2 == 0) ? sk.altRow : sk.dataLeft;
            writeCell(row, 0, pm.getUser().getFullName(), cs);
            writeCell(row, 1, pm.getUser().getEmail(), cs);
            writeCell(row, 2, pm.getRole(), cs);
            writeNumCell(row, 3, total, cs);
            writeNumCell(row, 4, todoC, cs);
            writeNumCell(row, 5, inProg, cs);
            writeNumCell(row, 6, doneC, cs);
            writeNumCell(row, 7, overdueC, overdueC > 0 ? sk.overdueRow : cs);
            writeNumCell(row, 8, estHrs, cs);
            writeNumCell(row, 9, actHrs, cs);
            r++;
        }

        // Tổng cộng cuối bảng
        Row total = sheet.createRow(r + 1);
        writeCell(total, 0, "TỔNG", sk.subHeader);
        for (int col = 3; col <= 9; col++) {
            Cell c = total.createCell(col);
            char colLetter = (char)('A' + col);
            c.setCellFormula(String.format("SUM(%s%d:%s%d)", colLetter, 4, colLetter, r));
            c.setCellStyle(sk.numBold);
        }

        autoSizeColumns(sheet, 10);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SHEET 4 — THỐNG KÊ THEO SPRINT
    // ══════════════════════════════════════════════════════════════════════════
    private void buildSheet4SprintSummary(XSSFWorkbook wb, StyleKit sk,
                                          List<Task> tasks, List<Sprint> sprints) {
        Sheet sheet = wb.createSheet("🏃 Sprint");

        writeBanner(sheet, sk, 0, "THỐNG KÊ THEO SPRINT", 11);

        String[] headers = {
            "Tên Sprint", "Trạng thái", "Ngày bắt đầu", "Ngày kết thúc",
            "Tổng task", "TODO", "Đang làm", "Hoàn thành", "Đã hủy",
            "Tỷ lệ HT (%)", "Giờ ước tính"
        };
        Row hdr = sheet.createRow(2);
        for (int i = 0; i < headers.length; i++) {
            Cell c = hdr.createCell(i); c.setCellValue(headers[i]); c.setCellStyle(sk.tableHeader);
        }
        sheet.createFreezePane(0, 3);

        // Backlog (chưa gán sprint)
        List<Sprint> allSprints = new ArrayList<>(sprints);
        Sprint backlogSentinel = null; // null = backlog

        int r = 3;
        for (Sprint sp : allSprints) {
            List<Task> spTasks = tasks.stream()
                    .filter(t -> sp.equals(t.getSprint()))
                    .collect(Collectors.toList());
            r = writeSprintRow(sheet, sk, r, sp.getName(), sprintStatusLabel(sp.getStatus()),
                    sp.getStartDate() != null ? sp.getStartDate().format(DATE_FMT) : "—",
                    sp.getEndDate()   != null ? sp.getEndDate().format(DATE_FMT)   : "—",
                    spTasks, wb);
        }

        // Backlog
        List<Task> backlogTasks = tasks.stream()
                .filter(t -> t.getSprint() == null)
                .collect(Collectors.toList());
        if (!backlogTasks.isEmpty()) {
            r = writeSprintRow(sheet, sk, r, "Product Backlog", "—", "—", "—", backlogTasks, wb);
        }

        autoSizeColumns(sheet, 11);
    }

    private int writeSprintRow(Sheet sheet, StyleKit sk, int r, String name, String status,
                                String start, String end, List<Task> spTasks, XSSFWorkbook wb) {
        long total = spTasks.size();
        long todo  = spTasks.stream().filter(t -> t.getStatus() == Task.TaskStatus.TODO).count();
        long inP   = spTasks.stream().filter(t -> t.getStatus() == Task.TaskStatus.IN_PROGRESS
                                              || t.getStatus() == Task.TaskStatus.IN_REVIEW).count();
        long done  = spTasks.stream().filter(t -> t.getStatus() == Task.TaskStatus.DONE
                                              || t.getStatus() == Task.TaskStatus.RESOLVED).count();
        long canc  = spTasks.stream().filter(t -> t.getStatus() == Task.TaskStatus.CANCELLED).count();
        double htRate = total > 0 ? done * 100.0 / total : 0;
        double estHrs = spTasks.stream()
                .mapToDouble(t -> t.getEstimatedHours() != null ? t.getEstimatedHours().doubleValue() : 0).sum();

        Row row = sheet.createRow(r);
        CellStyle cs = (r % 2 == 0) ? sk.altRow : sk.dataLeft;
        writeCell(row, 0, name, cs);
        writeCell(row, 1, status, cs);
        writeCell(row, 2, start, cs);
        writeCell(row, 3, end, cs);
        writeNumCell(row, 4, total, cs);
        writeNumCell(row, 5, todo, cs);
        writeNumCell(row, 6, inP, cs);
        writeNumCell(row, 7, done, cs);
        writeNumCell(row, 8, canc, cs);
        writePercentCell(row, 9, htRate, wb, cs);
        writeNumCell(row, 10, estHrs, cs);
        return r + 1;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SHEET 5 — TASK QUÁ HẠN & CẢNH BÁO
    // ══════════════════════════════════════════════════════════════════════════
    private void buildSheet5Overdue(XSSFWorkbook wb, StyleKit sk, List<Task> tasks, LocalDate today) {
        Sheet sheet = wb.createSheet("⚠️ Quá Hạn");

        writeBanner(sheet, sk, 0, "DANH SÁCH TASK QUÁ HẠN & CẢNH BÁO", 8);

        // ── Quá hạn ──
        List<Task> overdue = tasks.stream()
                .filter(t -> t.getDueDate() != null
                        && t.getDueDate().isBefore(today)
                        && t.getCompletedAt() == null)
                .sorted(Comparator.comparing(Task::getDueDate))
                .collect(Collectors.toList());

        Row secHeader = sheet.createRow(2);
        writeCell(secHeader, 0, "⛔ TASK QUÁ HẠN (" + overdue.size() + " task)", sk.sectionHeader);
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, 7));

        String[] headers1 = {"Task Key", "Tiêu đề", "Trạng thái", "Ưu tiên", "Người thực hiện", "Sprint", "Deadline", "Số ngày trễ"};
        Row hdr1 = sheet.createRow(3);
        for (int i = 0; i < headers1.length; i++) {
            Cell c = hdr1.createCell(i); c.setCellValue(headers1[i]); c.setCellStyle(sk.tableHeader);
        }
        sheet.setAutoFilter(new CellRangeAddress(3, 3, 0, headers1.length - 1));
        sheet.createFreezePane(0, 4);

        int r = 4;
        for (Task t : overdue) {
            long daysLate = today.toEpochDay() - t.getDueDate().toEpochDay();
            Row row = sheet.createRow(r++);
            writeCell(row, 0, t.getTaskKey() != null ? t.getTaskKey() : "", sk.taskKey);
            writeCell(row, 1, t.getTitle(), sk.overdueRow);
            writeCell(row, 2, statusLabel(t.getStatus()), sk.overdueRow);
            writeCell(row, 3, priorityLabel(t.getPriority()), sk.overdueRow);
            writeCell(row, 4, t.getAssignee() != null ? t.getAssignee().getFullName() : "—", sk.overdueRow);
            writeCell(row, 5, t.getSprint() != null ? t.getSprint().getName() : "Backlog", sk.overdueRow);
            writeCell(row, 6, t.getDueDate().format(DATE_FMT), sk.overdueRow);
            writeNumCell(row, 7, daysLate, sk.overdueRow);
        }

        // ── Sắp đến hạn trong 7 ngày ──
        r += 2;
        List<Task> upcoming = tasks.stream()
                .filter(t -> t.getDueDate() != null
                        && !t.getDueDate().isBefore(today)
                        && !t.getDueDate().isAfter(today.plusDays(7))
                        && t.getCompletedAt() == null)
                .sorted(Comparator.comparing(Task::getDueDate))
                .collect(Collectors.toList());

        Row sec2 = sheet.createRow(r++);
        writeCell(sec2, 0, "⚡ SẮP ĐẾN HẠN TRONG 7 NGÀY (" + upcoming.size() + " task)", sk.sectionHeader);
        sheet.addMergedRegion(new CellRangeAddress(r - 1, r - 1, 0, 7));

        String[] headers2 = {"Task Key", "Tiêu đề", "Trạng thái", "Ưu tiên", "Người thực hiện", "Sprint", "Deadline", "Còn (ngày)"};
        Row hdr2 = sheet.createRow(r++);
        for (int i = 0; i < headers2.length; i++) {
            Cell c = hdr2.createCell(i); c.setCellValue(headers2[i]); c.setCellStyle(sk.tableHeader);
        }

        for (Task t : upcoming) {
            long daysLeft = t.getDueDate().toEpochDay() - today.toEpochDay();
            Row row = sheet.createRow(r++);
            CellStyle cs = daysLeft <= 2 ? sk.overdueRow : sk.warnRow;
            writeCell(row, 0, t.getTaskKey() != null ? t.getTaskKey() : "", sk.taskKey);
            writeCell(row, 1, t.getTitle(), cs);
            writeCell(row, 2, statusLabel(t.getStatus()), cs);
            writeCell(row, 3, priorityLabel(t.getPriority()), cs);
            writeCell(row, 4, t.getAssignee() != null ? t.getAssignee().getFullName() : "—", cs);
            writeCell(row, 5, t.getSprint() != null ? t.getSprint().getName() : "Backlog", cs);
            writeCell(row, 6, t.getDueDate().format(DATE_FMT), cs);
            writeNumCell(row, 7, daysLeft, cs);
        }

        // ── Task chưa được giao ──
        r += 2;
        List<Task> unassigned = tasks.stream()
                .filter(t -> t.getAssignee() == null
                        && t.getStatus() != Task.TaskStatus.DONE
                        && t.getStatus() != Task.TaskStatus.CANCELLED
                        && t.getStatus() != Task.TaskStatus.RESOLVED)
                .collect(Collectors.toList());

        Row sec3 = sheet.createRow(r++);
        writeCell(sec3, 0, "❓ TASK CHƯA ĐƯỢC GIAO (" + unassigned.size() + " task)", sk.sectionHeader);
        sheet.addMergedRegion(new CellRangeAddress(r - 1, r - 1, 0, 7));

        String[] headers3 = {"Task Key", "Tiêu đề", "Trạng thái", "Ưu tiên", "Sprint", "Deadline", "Ngày tạo", ""};
        Row hdr3 = sheet.createRow(r++);
        for (int i = 0; i < headers3.length; i++) {
            Cell c = hdr3.createCell(i); c.setCellValue(headers3[i]); c.setCellStyle(sk.tableHeader);
        }
        for (Task t : unassigned) {
            Row row = sheet.createRow(r++);
            CellStyle cs = sk.warnRow;
            writeCell(row, 0, t.getTaskKey() != null ? t.getTaskKey() : "", sk.taskKey);
            writeCell(row, 1, t.getTitle(), cs);
            writeCell(row, 2, statusLabel(t.getStatus()), cs);
            writeCell(row, 3, priorityLabel(t.getPriority()), cs);
            writeCell(row, 4, t.getSprint() != null ? t.getSprint().getName() : "Backlog", cs);
            writeCell(row, 5, t.getDueDate() != null ? t.getDueDate().format(DATE_FMT) : "—", cs);
            writeCell(row, 6, t.getCreatedAt() != null ? t.getCreatedAt().format(DATE_FMT) : "—", cs);
            writeCell(row, 7, "", cs);
        }

        autoSizeColumns(sheet, 8);
        // Cột tiêu đề không quá rộng
        sheet.setColumnWidth(1, Math.min(sheet.getColumnWidth(1), 15000));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HELPER — WRITE CELLS
    // ══════════════════════════════════════════════════════════════════════════
    private void writeCell(Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value != null ? value : "");
        if (style != null) c.setCellStyle(style);
    }

    private void writeNumCell(Row row, int col, double value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value);
        if (style != null) c.setCellStyle(style);
    }

    private void writeNumCell(Row row, int col, long value, CellStyle style) {
        writeNumCell(row, col, (double) value, style);
    }

    private void writePercentCell(Row row, int col, double pct, XSSFWorkbook wb, CellStyle base) {
        Cell c = row.createCell(col);
        c.setCellValue(pct / 100.0);
        XSSFCellStyle pctStyle = wb.createCellStyle();
        pctStyle.cloneStyleFrom(base);
        pctStyle.setDataFormat(wb.createDataFormat().getFormat("0.0%"));
        c.setCellStyle(pctStyle);
    }

    private int writeBanner(Sheet sheet, StyleKit sk, int rowIdx, String text, int colspan) {
        sheet.createRow(rowIdx).setHeightInPoints(28);
        Row row = sheet.getRow(rowIdx);
        Cell c = row.createCell(0);
        c.setCellValue(text);
        c.setCellStyle(sk.banner);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, colspan - 1));
        return rowIdx + 1;
    }

    private void writeSubBanner(Sheet sheet, StyleKit sk, int rowIdx, String text, int colspan) {
        Row row = sheet.createRow(rowIdx);
        Cell c = row.createCell(0);
        c.setCellValue(text);
        c.setCellStyle(sk.subBanner);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, colspan - 1));
    }

    private void writeSectionHeader(Sheet sheet, StyleKit sk, int rowIdx, String text, int colspan) {
        Row row = sheet.createRow(rowIdx);
        Cell c = row.createCell(0);
        c.setCellValue(text);
        c.setCellStyle(sk.sectionHeader);
        if (colspan > 1)
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, colspan - 1));
    }

    private void writeKpiRow(Sheet sheet, StyleKit sk, int rowIdx,
                              String label1, long val1, String label2, long val2) {
        Row row = sheet.createRow(rowIdx);
        writeCell(row, 0, label1, sk.kpiLabel);
        writeNumCell(row, 1, val1, sk.kpiValue);
        writeCell(row, 2, label2, sk.kpiLabel);
        writeNumCell(row, 3, val2, sk.kpiValue);
    }

    private void writeRateRow(Sheet sheet, StyleKit sk, int rowIdx, XSSFWorkbook wb,
                               String label, double rate, long numerator, long denominator) {
        Row row = sheet.createRow(rowIdx);
        writeCell(row, 0, label, sk.kpiLabel);
        writePercentCell(row, 1, rate, wb, sk.kpiValue);
        writeCell(row, 2, numerator + " / " + denominator, sk.dataCenter);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FILTER DESCRIPTION HELPER
    private String buildFilterDescription(ExportFilter f) {
        List<String> parts = new ArrayList<>();
        if (f.getSprintIds() != null && !f.getSprintIds().isEmpty())
            parts.add("Sprint: " + f.getSprintIds().stream().map(UUID::toString).collect(Collectors.joining(", ")));
        if (f.getAssigneeId() != null)
            parts.add("Người thực hiện: " + f.getAssigneeId());
        if (f.getDateFrom() != null)
            parts.add("Từ: " + f.getDateFrom().format(DATE_FMT));
        if (f.getDateTo() != null)
            parts.add("Đến: " + f.getDateTo().format(DATE_FMT));
        if (f.getStatuses() != null && !f.getStatuses().isEmpty())
            parts.add("Trạng thái: " + f.getStatuses().stream().map(Enum::name).collect(Collectors.joining(", ")));
        if (f.getPriorities() != null && !f.getPriorities().isEmpty())
            parts.add("Ưu tiên: " + f.getPriorities().stream().map(Enum::name).collect(Collectors.joining(", ")));
        return String.join("   |   ", parts);
    }

    // LABEL HELPERS
    // ══════════════════════════════════════════════════════════════════════════
    private String statusLabel(Task.TaskStatus s) {
        return switch (s) {
            case TODO        -> "Cần làm";
            case IN_PROGRESS -> "Đang làm";
            case IN_REVIEW   -> "Đang review";
            case RESOLVED    -> "Đã giải quyết";
            case DONE        -> "Hoàn thành";
            case CANCELLED   -> "Đã hủy";
        };
    }

    private String priorityLabel(Task.TaskPriority p) {
        return switch (p) {
            case LOW    -> "Thấp";
            case MEDIUM -> "Trung bình";
            case HIGH   -> "Cao";
            case URGENT -> "Khẩn cấp";
        };
    }

    private String sprintStatusLabel(Sprint.SprintStatus s) {
        return switch (s) {
            case PLANNED   -> "Chưa bắt đầu";
            case ACTIVE    -> "Đang chạy";
            case COMPLETED -> "Hoàn thành";
            case CANCELLED -> "Đã hủy";
        };
    }

    private int statusOrder(Task.TaskStatus s) {
        return switch (s) {
            case IN_PROGRESS -> 0;
            case IN_REVIEW   -> 1;
            case TODO        -> 2;
            case RESOLVED    -> 3;
            case DONE        -> 4;
            case CANCELLED   -> 5;
        };
    }

    private int priorityOrder(Task.TaskPriority p) {
        return switch (p) {
            case URGENT -> 0;
            case HIGH   -> 1;
            case MEDIUM -> 2;
            case LOW    -> 3;
        };
    }

    // ══════════════════════════════════════════════════════════════════════════
    // STYLE KIT — tất cả style tập trung tại đây
    // ══════════════════════════════════════════════════════════════════════════
    private static class StyleKit {
        final XSSFCellStyle banner, subBanner, sectionHeader, subHeader;
        final XSSFCellStyle tableHeader, dataLeft, dataCenter, altRow;
        final XSSFCellStyle overdueRow, doneRow, warnRow;
        final XSSFCellStyle kpiLabel, kpiValue, numBold, taskKey, italic;

        StyleKit(XSSFWorkbook wb) {
            banner      = style(wb, COLOR_HEADER_BG,  COLOR_WHITE,    14, true,  HorizontalAlignment.CENTER, BorderStyle.NONE);
            subBanner   = style(wb, COLOR_SECTION_BG, COLOR_WHITE,    10, false, HorizontalAlignment.CENTER, BorderStyle.NONE);
            sectionHeader=style(wb, COLOR_SECTION_BG, COLOR_WHITE,    11, true,  HorizontalAlignment.LEFT,   BorderStyle.THIN);
            subHeader   = style(wb, hex("2C3E50"),    COLOR_WHITE,    10, true,  HorizontalAlignment.CENTER, BorderStyle.THIN);
            tableHeader = style(wb, COLOR_HEADER_BG,  COLOR_WHITE,    10, true,  HorizontalAlignment.CENTER, BorderStyle.THIN);
            dataLeft    = style(wb, COLOR_WHITE,       COLOR_DARK_TEXT, 9, false, HorizontalAlignment.LEFT,  BorderStyle.THIN);
            dataCenter  = style(wb, COLOR_WHITE,       COLOR_DARK_TEXT, 9, false, HorizontalAlignment.CENTER,BorderStyle.THIN);
            altRow      = style(wb, COLOR_ALT_ROW,    COLOR_DARK_TEXT, 9, false, HorizontalAlignment.LEFT,  BorderStyle.THIN);
            overdueRow  = style(wb, COLOR_OVERDUE,    hex("C0392B"),   9, false, HorizontalAlignment.LEFT,  BorderStyle.THIN);
            doneRow     = style(wb, COLOR_DONE,       hex("1E8449"),   9, false, HorizontalAlignment.LEFT,  BorderStyle.THIN);
            warnRow     = style(wb, COLOR_WARN,       hex("7D6608"),   9, false, HorizontalAlignment.LEFT,  BorderStyle.THIN);
            kpiLabel    = style(wb, hex("EBF5FB"),    COLOR_DARK_TEXT, 10, true, HorizontalAlignment.LEFT,  BorderStyle.THIN);
            kpiValue    = style(wb, hex("D6EAF8"),    COLOR_DARK_TEXT, 11, true, HorizontalAlignment.CENTER,BorderStyle.MEDIUM);
            numBold     = style(wb, COLOR_WHITE,       COLOR_DARK_TEXT, 10, true, HorizontalAlignment.CENTER,BorderStyle.THIN);
            taskKey     = style(wb, hex("D5E8D4"),    hex("1E8449"),   9, true,  HorizontalAlignment.CENTER, BorderStyle.THIN);
            italic      = styleItalic(wb);
        }

        private XSSFCellStyle style(XSSFWorkbook wb, byte[] bgRgb, byte[] fgRgb,
                                    int fontSize, boolean bold,
                                    HorizontalAlignment align, BorderStyle border) {
            XSSFCellStyle cs = wb.createCellStyle();
            XSSFColor bg = new XSSFColor(bgRgb, null);
            cs.setFillForegroundColor(bg);
            cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cs.setAlignment(align);
            cs.setVerticalAlignment(VerticalAlignment.TOP);
            cs.setWrapText(false);
            if (border != BorderStyle.NONE) {
                cs.setBorderTop(border); cs.setBorderBottom(border);
                cs.setBorderLeft(border); cs.setBorderRight(border);
                XSSFColor borderColor = new XSSFColor(COLOR_GRAY_BORDER, null);
                cs.setTopBorderColor(borderColor); cs.setBottomBorderColor(borderColor);
                cs.setLeftBorderColor(borderColor); cs.setRightBorderColor(borderColor);
            }
            XSSFFont font = wb.createFont();
            font.setFontHeightInPoints((short) fontSize);
            font.setBold(bold);
            XSSFColor fg = new XSSFColor(fgRgb, null);
            font.setColor(fg);
            font.setFontName("Calibri");
            cs.setFont(font);
            return cs;
        }

        private XSSFCellStyle styleItalic(XSSFWorkbook wb) {
            XSSFCellStyle cs = wb.createCellStyle();
            XSSFFont font = wb.createFont();
            font.setItalic(true);
            font.setFontHeightInPoints((short) 9);
            font.setColor(new XSSFColor(hex("7F8C8D"), null));
            cs.setFont(font);
            return cs;
        }
    }

    // Auto-size tất cả cột sau khi ghi xong data
    private void autoSizeColumns(Sheet sheet, int numCols) {
        for (int i = 0; i < numCols; i++) {
            sheet.autoSizeColumn(i);
            // Thêm padding 512 units (~1 ký tự) để tránh bị cắt sát
            int current = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.min(current + 512, 20000));
        }
    }

    private static byte[] hex(String h) {
        return new byte[]{
            (byte) Integer.parseInt(h.substring(0, 2), 16),
            (byte) Integer.parseInt(h.substring(2, 4), 16),
            (byte) Integer.parseInt(h.substring(4, 6), 16)
        };
    }
}
