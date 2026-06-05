package com.taskoryx.backend.config;

import com.taskoryx.backend.entity.*;
import com.taskoryx.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Tạo dữ liệu demo khi app khởi động lần đầu (bỏ qua nếu đã có project).
 * Chạy sau DataInitializer (@Order 1) để đảm bảo roles/permissions sẵn sàng.
 *
 * Kịch bản: Công ty TechVision — 2 dự án đang hoạt động
 *   Dự án 1: "Hệ thống CRM nội bộ" (key: CRM) — SCRUM, đang Sprint 3
 *   Dự án 2: "App Mobile KH" (key: MAPP) — KANBAN, đang chạy
 *
 * Tài khoản (mật khẩu đều là Demo@123456):
 *   admin            → admin@techvision.vn       — System Admin / OWNER
 *   nguyen.van.an    → an.nguyen@techvision.vn   — Project Manager
 *   tran.thi.bich    → bich.tran@techvision.vn   — Tech Lead Backend
 *   le.minh.duc      → duc.le@techvision.vn       — Backend Developer
 *   pham.thi.huong   → huong.pham@techvision.vn  — Frontend Developer
 *   hoang.van.kiet   → kiet.hoang@techvision.vn  — QA / Tester
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(100)
public class DemoDataInitializer implements ApplicationRunner {

    private final UserRepository               userRepository;
    private final ProjectRepository            projectRepository;
    private final ProjectMemberRepository      projectMemberRepository;
    private final BoardRepository              boardRepository;
    private final BoardColumnRepository        boardColumnRepository;
    private final SprintRepository             sprintRepository;
    private final TaskRepository               taskRepository;
    private final LabelRepository              labelRepository;
    private final IssueCategoryRepository      issueCategoryRepository;
    private final CommentRepository            commentRepository;
    private final TimeTrackingRepository       timeTrackingRepository;
    private final TaskLabelRepository          taskLabelRepository;
    private final TaskWatcherRepository        taskWatcherRepository;
    private final TaskDependencyRepository     taskDependencyRepository;
    private final NotificationRepository       notificationRepository;
    private final RoleRepository               roleRepository;
    private final UserRoleRepository           userRoleRepository;
    private final PasswordEncoder              passwordEncoder;

    private static final String DEMO_PASSWORD = "Demo@123456";

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (projectRepository.count() > 0) {
            log.info("Demo data already exists — skipping.");
            return;
        }
        log.info("Seeding demo data...");

        // ── 0. System roles (tạo bởi DataInitializer trước) ──────────────────
        Role roleSuperAdmin     = findRole("SUPER_ADMIN");
        Role roleProjectManager = findRole("PROJECT_MANAGER");
        Role roleTeamLead       = findRole("TEAM_LEAD");
        Role roleMember         = findRole("MEMBER");

        // ── 1. Users ──────────────────────────────────────────────────────────
        User admin  = upsertUser("admin",           "Trần Quốc Admin",    "admin@techvision.vn",        false);
        User an     = upsertUser("nguyen.van.an",   "Nguyễn Văn An",      "an.nguyen@techvision.vn",    false);
        User bich   = upsertUser("tran.thi.bich",   "Trần Thị Bích",      "bich.tran@techvision.vn",    false);
        User duc    = upsertUser("le.minh.duc",     "Lê Minh Đức",        "duc.le@techvision.vn",       false);
        User huong  = upsertUser("pham.thi.huong",  "Phạm Thị Hương",     "huong.pham@techvision.vn",   false);
        User kiet   = upsertUser("hoang.van.kiet",  "Hoàng Văn Kiệt",     "kiet.hoang@techvision.vn",   false);

        // ── 2. System roles ───────────────────────────────────────────────────
        grantSystemRole(admin, roleSuperAdmin);
        grantSystemRole(an,    roleProjectManager);
        grantSystemRole(bich,  roleTeamLead);
        grantSystemRole(duc,   roleMember);
        grantSystemRole(huong, roleMember);
        grantSystemRole(kiet,  roleMember);

        // ═══════════════════════════════════════════════════════════════════════
        // DỰ ÁN 1: Hệ thống CRM nội bộ (SCRUM)
        // ═══════════════════════════════════════════════════════════════════════
        Project crm = projectRepository.save(Project.builder()
                .name("Hệ thống CRM nội bộ")
                .description("Xây dựng hệ thống quản lý quan hệ khách hàng nội bộ: " +
                             "quản lý leads, pipeline bán hàng, lịch sử liên lạc, báo cáo doanh số và tự động hóa email.")
                .key("CRM")
                .owner(admin)
                .color("#7c3aed")
                .icon("users")
                .isPublic(false)
                .isArchived(false)
                .build());

        addMember(crm, admin,  "OWNER");
        addMember(crm, an,     "PM");
        addMember(crm, bich,   "MEMBER");
        addMember(crm, duc,    "MEMBER");
        addMember(crm, huong,  "MEMBER");
        addMember(crm, kiet,   "MEMBER");

        // Labels CRM
        Label lBe   = saveLabel(crm, "Backend",     "#22c55e");
        Label lFe   = saveLabel(crm, "Frontend",    "#3b82f6");
        Label lApi  = saveLabel(crm, "API",         "#f59e0b");
        Label lDb   = saveLabel(crm, "Database",    "#8b5cf6");
        Label lUx   = saveLabel(crm, "UI/UX",       "#ec4899");
        Label lBug  = saveLabel(crm, "Bug",         "#ef4444");
        Label lPerf = saveLabel(crm, "Performance", "#f97316");

        // Categories CRM
        IssueCategory catFeature = saveCategory(crm, "Tính năng",  bich);
        IssueCategory catBug     = saveCategory(crm, "Bug",        kiet);
        IssueCategory catImprove = saveCategory(crm, "Cải thiện",  an);
        IssueCategory catDoc     = saveCategory(crm, "Tài liệu",   an);

        // ── Sprint 1 — Nền tảng (COMPLETED) ──────────────────────────────────
        Sprint sp1 = saveSprint(crm, "Sprint 1 — Nền tảng",
                "Thiết kế DB schema, cài môi trường, auth module, skeleton UI.",
                Sprint.SprintStatus.COMPLETED,
                LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 18));

        Map<Task.TaskStatus, BoardColumn> sp1c = sprintCols(sp1);

        Task t1 = saveTask(crm, 1, "Thiết kế database schema",
                "Thiết kế toàn bộ schema: customers, contacts, deals, pipelines, activities, notes, tags.",
                Task.TaskPriority.HIGH, Task.TaskStatus.DONE,
                bich, admin,
                LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 8),
                bd(16), bd(18), null, catFeature,
                sp1, sp1.getBoard(), sp1c.get(Task.TaskStatus.DONE), bd(1000));
        addLabels(t1, lBe, lDb);
        logTime(t1, bich, "Phân tích yêu cầu, vẽ ER diagram", 6.0, LocalDate.of(2026, 1, 6));
        logTime(t1, bich, "Viết migration scripts V1", 5.0, LocalDate.of(2026, 1, 7));
        logTime(t1, bich, "Review và finalize schema với team", 3.0, LocalDate.of(2026, 1, 8));
        comment(t1, an,   "Schema trông ổn. Tuy nhiên bảng `activities` thiếu index cho (customer_id, created_at) — sẽ rất chậm khi lọc lịch sử theo khách hàng.");
        comment(t1, bich, "Đã thêm composite index. Cũng tạo thêm partial index cho deal trong trạng thái OPEN để tối ưu pipeline view.");
        comment(t1, duc,  "Đồng ý thiết kế. Một lưu ý: cột `metadata` nên dùng kiểu JSONB thay vì TEXT để query được bên trong.");
        comment(t1, bich, "Cảm ơn @duc.le, đã đổi sang JSONB rồi.");

        Task t2 = saveTask(crm, 2, "Cài đặt môi trường CI/CD",
                "Cấu hình Docker Compose, GitHub Actions pipeline, môi trường dev/staging/prod.",
                Task.TaskPriority.HIGH, Task.TaskStatus.DONE,
                an, admin,
                LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 7),
                bd(10), bd(9), null, catFeature,
                sp1, sp1.getBoard(), sp1c.get(Task.TaskStatus.DONE), bd(2000));
        addLabels(t2, lBe);
        logTime(t2, an, "Docker Compose + env vars", 4.0, LocalDate.of(2026, 1, 6));
        logTime(t2, an, "GitHub Actions workflow", 4.0, LocalDate.of(2026, 1, 7));
        comment(t2, an,   "Môi trường đã up. Mọi người chạy `docker compose up -d` là được. DB seed tự động chạy khi start.");

        Task t3 = saveTask(crm, 3, "Module xác thực người dùng",
                "JWT auth: đăng nhập, refresh token, phân quyền RBAC, middleware bảo vệ route.",
                Task.TaskPriority.HIGH, Task.TaskStatus.DONE,
                bich, admin,
                LocalDate.of(2026, 1, 7), LocalDate.of(2026, 1, 13),
                bd(20), bd(19), null, catFeature,
                sp1, sp1.getBoard(), sp1c.get(Task.TaskStatus.DONE), bd(3000));
        addLabels(t3, lBe, lApi);
        logTime(t3, bich, "Implement JWT filter, UserDetails, SecurityConfig", 8.0, LocalDate.of(2026, 1, 9));
        logTime(t3, bich, "RBAC: Role, Permission, UserRole entities + service", 6.0, LocalDate.of(2026, 1, 10));
        logTime(t3, bich, "Unit test auth endpoints", 4.0, LocalDate.of(2026, 1, 12));
        comment(t3, kiet, "Test xong auth flow. Tất cả case pass: login thành công, sai mật khẩu, token hết hạn, refresh token, logout. Merge được.");
        comment(t3, an,   "Chú ý: nếu refresh token đã bị revoke cần trả 401 không phải 403 để FE biết cách redirect đúng.");
        comment(t3, bich, "Đã fix, cảm ơn @an.nguyen.");

        Task t4 = saveTask(crm, 4, "Skeleton giao diện chính",
                "Layout chính: sidebar nav, header, breadcrumb, responsive breakpoints, dark mode toggle.",
                Task.TaskPriority.MEDIUM, Task.TaskStatus.DONE,
                huong, admin,
                LocalDate.of(2026, 1, 8), LocalDate.of(2026, 1, 16),
                bd(24), bd(22), null, catFeature,
                sp1, sp1.getBoard(), sp1c.get(Task.TaskStatus.DONE), bd(4000));
        addLabels(t4, lFe, lUx);
        logTime(t4, huong, "Thiết kế layout và component system", 8.0, LocalDate.of(2026, 1, 9));
        logTime(t4, huong, "Implement Sidebar và Header", 7.0, LocalDate.of(2026, 1, 13));
        logTime(t4, huong, "Dark mode và responsive", 5.0, LocalDate.of(2026, 1, 15));
        comment(t4, huong, "Dark mode xong. Dùng CSS variables + Tailwind, toggle lưu vào localStorage.");
        comment(t4, an,    "UI trông clean. Góp ý nhỏ: sidebar nên có trạng thái collapsed để tăng không gian làm việc.");
        comment(t4, huong, "Đã thêm collapsed state với animation. Lưu preference vào localStorage.");

        // ── Sprint 2 — Quản lý khách hàng (COMPLETED) ────────────────────────
        Sprint sp2 = saveSprint(crm, "Sprint 2 — Quản lý khách hàng",
                "CRUD khách hàng, import CSV, tìm kiếm nâng cao, giao diện danh sách và chi tiết.",
                Sprint.SprintStatus.COMPLETED,
                LocalDate.of(2026, 1, 19), LocalDate.of(2026, 2, 1));

        Map<Task.TaskStatus, BoardColumn> sp2c = sprintCols(sp2);

        Task t5 = saveTask(crm, 5, "CRUD API khách hàng",
                "API quản lý khách hàng: tạo, sửa, xóa, phân trang, lọc đa tiêu chí, soft delete.",
                Task.TaskPriority.HIGH, Task.TaskStatus.DONE,
                bich, admin,
                LocalDate.of(2026, 1, 19), LocalDate.of(2026, 1, 24),
                bd(20), bd(21), null, catFeature,
                sp2, sp2.getBoard(), sp2c.get(Task.TaskStatus.DONE), bd(1000));
        addLabels(t5, lBe, lApi);
        logTime(t5, bich, "CRUD endpoints + validation", 8.0, LocalDate.of(2026, 1, 21));
        logTime(t5, bich, "Specification filter + phân trang", 6.0, LocalDate.of(2026, 1, 23));
        comment(t5, duc,  "API trả về đủ field cần thiết. Thêm field `lastContactedAt` vào response để FE hiển thị không?");
        comment(t5, bich, "Đã thêm vào CustomerResponse, tính từ bảng activities mới nhất.");

        Task t6 = saveTask(crm, 6, "Import khách hàng từ CSV",
                "Upload file CSV, validate dữ liệu, preview trước khi import, xử lý lỗi từng dòng.",
                Task.TaskPriority.MEDIUM, Task.TaskStatus.DONE,
                duc, admin,
                LocalDate.of(2026, 1, 21), LocalDate.of(2026, 1, 28),
                bd(16), bd(18), null, catFeature,
                sp2, sp2.getBoard(), sp2c.get(Task.TaskStatus.DONE), bd(2000));
        addLabels(t6, lBe, lApi);
        logTime(t6, duc, "CSV parser + validation logic", 7.0, LocalDate.of(2026, 1, 24));
        logTime(t6, duc, "Batch insert + error report", 6.0, LocalDate.of(2026, 1, 27));
        comment(t6, kiet, "Test import 500 dòng: 480 thành công, 20 lỗi (email trùng). Báo lỗi rõ ràng từng dòng, accepted.");

        Task t7 = saveTask(crm, 7, "Giao diện danh sách khách hàng",
                "Table view với sort/filter/search, infinite scroll, quick actions, bulk select.",
                Task.TaskPriority.HIGH, Task.TaskStatus.DONE,
                huong, admin,
                LocalDate.of(2026, 1, 21), LocalDate.of(2026, 1, 29),
                bd(20), bd(22), null, catFeature,
                sp2, sp2.getBoard(), sp2c.get(Task.TaskStatus.DONE), bd(3000));
        addLabels(t7, lFe, lUx);
        logTime(t7, huong, "CustomerTable với virtual scroll", 9.0, LocalDate.of(2026, 1, 26));
        logTime(t7, huong, "Filter panel và bulk actions", 7.0, LocalDate.of(2026, 1, 28));
        comment(t7, an,    "Bulk delete cần confirmation dialog. Và nên có undo trong 5 giây sau khi xóa.");
        comment(t7, huong, "Đã thêm confirm dialog và toast với nút Undo (soft delete 5 giây).");

        Task t8 = saveTask(crm, 8, "Fix bug: Lọc theo ngày tạo sai múi giờ",
                "Khi lọc khách hàng theo ngày tạo, kết quả bị lệch 1 ngày do FE gửi UTC thay vì Asia/Ho_Chi_Minh.",
                Task.TaskPriority.HIGH, Task.TaskStatus.DONE,
                huong, kiet,
                LocalDate.of(2026, 1, 27), LocalDate.of(2026, 1, 28),
                bd(3), bd(2), null, catBug,
                sp2, sp2.getBoard(), sp2c.get(Task.TaskStatus.DONE), bd(4000));
        addLabels(t8, lFe, lBug);
        comment(t8, kiet,  "Reproduce: lọc ngày 2026-01-20, nhận được record tạo lúc 23:00 ngày 19 (đúng giờ VN là 06:00 ngày 20). Server xử lý timezone sai.");
        comment(t8, huong, "Fix: DatePicker giờ gửi `date + T00:00:00+07:00` thay vì UTC. Backend nhận LocalDate nên không bị lệch.");
        comment(t8, kiet,  "Retest OK. Closing.");

        // ── Sprint 3 — Pipeline bán hàng (ACTIVE) ────────────────────────────
        Sprint sp3 = saveSprint(crm, "Sprint 3 — Pipeline bán hàng",
                "Deal management, pipeline Kanban, forecast doanh số, reminder tự động.",
                Sprint.SprintStatus.ACTIVE,
                LocalDate.of(2026, 5, 19), LocalDate.of(2026, 6, 1));

        Map<Task.TaskStatus, BoardColumn> sp3c = sprintCols(sp3);

        Task t9 = saveTask(crm, 9, "API quản lý deals",
                "CRUD deal: tạo, cập nhật stage, ghi note, gán nhân viên, set expected close date, probability.",
                Task.TaskPriority.HIGH, Task.TaskStatus.DONE,
                bich, admin,
                LocalDate.of(2026, 5, 19), LocalDate.of(2026, 5, 23),
                bd(20), bd(21), null, catFeature,
                sp3, sp3.getBoard(), sp3c.get(Task.TaskStatus.DONE), bd(1000));
        addLabels(t9, lBe, lApi);
        logTime(t9, bich, "Deal entity, repo, service, controller", 10.0, LocalDate.of(2026, 5, 21));
        logTime(t9, bich, "Stage transition logic + validation", 5.0, LocalDate.of(2026, 5, 22));
        comment(t9, duc, "API tốt. Thêm endpoint `GET /deals/summary?period=monthly` để FE render forecast chart không?");
        comment(t9, bich, "Đã thêm, gộp theo month + stage, trả về tổng value và count.");

        Task t10 = saveTask(crm, 10, "Pipeline Kanban view",
                "Giao diện pipeline: kéo thả deal giữa các stage, hiển thị tổng value mỗi cột, quick edit.",
                Task.TaskPriority.HIGH, Task.TaskStatus.IN_PROGRESS,
                huong, admin,
                LocalDate.of(2026, 5, 21), LocalDate.of(2026, 5, 28),
                bd(24), null, null, catFeature,
                sp3, sp3.getBoard(), sp3c.get(Task.TaskStatus.IN_PROGRESS), bd(2000));
        addLabels(t10, lFe, lUx);
        logTime(t10, huong, "Setup dnd-kit, DealCard component", 8.0, LocalDate.of(2026, 5, 24));
        comment(t10, huong, "Drag & drop cơ bản đã hoạt động. Đang xử lý optimistic update và rollback khi API lỗi.");
        comment(t10, an,    "Nhớ debounce API call khi kéo thả nhanh liên tục, tránh spam request.");
        comment(t10, huong, "Đã xử lý bằng optimistic UI — chỉ gọi API 1 lần khi drop, không gọi khi đang drag.");

        Task t11 = saveTask(crm, 11, "Forecast doanh số",
                "Dashboard forecast: biểu đồ pipeline theo tháng, win rate, average deal size, trend 6 tháng.",
                Task.TaskPriority.MEDIUM, Task.TaskStatus.TODO,
                duc, admin,
                LocalDate.of(2026, 5, 26), LocalDate.of(2026, 5, 30),
                bd(16), null, null, catFeature,
                sp3, sp3.getBoard(), sp3c.get(Task.TaskStatus.TODO), bd(3000));
        addLabels(t11, lFe, lBe);
        comment(t11, an, "Cần trao đổi với sales team về các metric quan trọng trước khi implement. Tôi sẽ gửi brief vào thứ Hai.");

        Task t12 = saveTask(crm, 12, "Reminder tự động cho deals",
                "Gửi email/notification nhắc nhở khi deal sắp đến expected close date hoặc không có activity trong N ngày.",
                Task.TaskPriority.MEDIUM, Task.TaskStatus.TODO,
                bich, admin,
                LocalDate.of(2026, 5, 26), LocalDate.of(2026, 6, 1),
                bd(12), null, null, catFeature,
                sp3, sp3.getBoard(), sp3c.get(Task.TaskStatus.TODO), bd(4000));
        addLabels(t12, lBe);

        Task t13 = saveTask(crm, 13, "Fix bug: Tìm kiếm khách hàng không tìm được số điện thoại",
                "Search bar tìm theo tên và email OK, nhưng tìm theo số điện thoại (VD: 0912345678) không trả về kết quả.",
                Task.TaskPriority.HIGH, Task.TaskStatus.IN_REVIEW,
                bich, kiet,
                LocalDate.of(2026, 5, 27), LocalDate.of(2026, 5, 29),
                bd(4), bd(3), null, catBug,
                sp3, sp3.getBoard(), sp3c.get(Task.TaskStatus.IN_REVIEW), bd(5000));
        addLabels(t13, lBe, lBug);
        logTime(t13, bich, "Debug và fix search query", 3.0, LocalDate.of(2026, 5, 28));
        comment(t13, kiet, "Reproduce: search '0912345678' → 0 kết quả. Tuy nhiên search '912345' (bỏ đầu) lại tìm thấy. Có vẻ do chuẩn hóa số điện thoại.");
        comment(t13, bich, "Root cause: DB lưu '0912345678', search query dùng LIKE '%912345678%' (bỏ số 0 đầu). Fix bằng cách normalize input trước khi search.");
        comment(t13, bich, "Đã fix + thêm test case. Assign @kiet.hoang retest.");

        Task t14 = saveTask(crm, 14, "Viết tài liệu API cho module Deals",
                "OpenAPI spec, ví dụ request/response, mô tả business rules, error codes.",
                Task.TaskPriority.LOW, Task.TaskStatus.TODO,
                an, admin,
                LocalDate.of(2026, 5, 28), LocalDate.of(2026, 6, 1),
                bd(8), null, null, catDoc,
                sp3, sp3.getBoard(), sp3c.get(Task.TaskStatus.TODO), bd(6000));
        addLabels(t14, lApi);

        // Task dependency: t11 (Forecast) phụ thuộc t9 (API deals) đã xong
        taskDependencyRepository.save(TaskDependency.builder()
                .task(t11).dependsOnTask(t9)
                .type(TaskDependency.DependencyType.DEPENDS_ON).build());
        // t12 (Reminder) phụ thuộc t9 (API deals)
        taskDependencyRepository.save(TaskDependency.builder()
                .task(t12).dependsOnTask(t9)
                .type(TaskDependency.DependencyType.DEPENDS_ON).build());
        // t14 (Docs) phụ thuộc t9 (API deals)
        taskDependencyRepository.save(TaskDependency.builder()
                .task(t14).dependsOnTask(t9)
                .type(TaskDependency.DependencyType.DEPENDS_ON).build());

        // Sprint 4 — PLANNED
        Sprint sp4 = saveSprint(crm, "Sprint 4 — Tích hợp & Báo cáo",
                "Email integration, báo cáo tổng hợp, export Excel, webhook outbound.",
                Sprint.SprintStatus.PLANNED,
                LocalDate.of(2026, 6, 2), LocalDate.of(2026, 6, 15));

        Map<Task.TaskStatus, BoardColumn> sp4c = sprintCols(sp4);

        Task t15 = saveTask(crm, 15, "Tích hợp Gmail / Outlook",
                "Kết nối hộp thư Gmail/Outlook, đồng bộ email liên quan đến khách hàng vào CRM.",
                Task.TaskPriority.HIGH, Task.TaskStatus.TODO,
                bich, admin, null, null,
                bd(28), null, null, catFeature,
                sp4, sp4.getBoard(), sp4c.get(Task.TaskStatus.TODO), bd(1000));
        addLabels(t15, lBe, lApi);

        Task t16 = saveTask(crm, 16, "Báo cáo tổng hợp doanh số",
                "Dashboard báo cáo: doanh số theo nhân viên, theo tháng/quý, top khách hàng, conversion rate.",
                Task.TaskPriority.MEDIUM, Task.TaskStatus.TODO,
                duc, admin, null, null,
                bd(20), null, null, catFeature,
                sp4, sp4.getBoard(), sp4c.get(Task.TaskStatus.TODO), bd(2000));
        addLabels(t16, lBe, lFe);

        // Watchers
        watch(t10, an);
        watch(t10, duc);
        watch(t13, an);
        watch(t13, kiet);
        watch(t9,  an);

        // Notifications mẫu
        saveNotification(bich,
                Notification.NotificationType.TASK_ASSIGNED,
                "Task mới được giao",
                "Bạn được giao task [CRM-13] Fix bug: Tìm kiếm khách hàng không tìm được số điện thoại",
                Notification.RelatedType.TASK, t13.getId(), false);
        saveNotification(kiet,
                Notification.NotificationType.MENTION,
                "Bạn được nhắc đến",
                "Trần Thị Bích đã đề cập đến bạn trong task [CRM-13]",
                Notification.RelatedType.TASK, t13.getId(), false);
        saveNotification(huong,
                Notification.NotificationType.TASK_UPDATED,
                "Task được cập nhật",
                "Task [CRM-10] Pipeline Kanban view vừa được cập nhật bởi Hoàng Văn Kiệt",
                Notification.RelatedType.TASK, t10.getId(), true);
        saveNotification(an,
                Notification.NotificationType.TASK_COMMENTED,
                "Bình luận mới",
                "Trần Thị Bích đã bình luận vào task [CRM-9] API quản lý deals",
                Notification.RelatedType.TASK, t9.getId(), true);

        // ═══════════════════════════════════════════════════════════════════════
        // DỰ ÁN 2: App Mobile Khách hàng (KANBAN)
        // ═══════════════════════════════════════════════════════════════════════
        Project mapp = projectRepository.save(Project.builder()
                .name("App Mobile Khách hàng")
                .description("Ứng dụng mobile React Native cho khách hàng: xem deals, tra cứu thông tin liên hệ, push notification.")
                .key("MAPP")
                .owner(an)
                .color("#0ea5e9")
                .icon("mobile")
                .isPublic(false)
                .isArchived(false)
                .build());

        addMember(mapp, an,    "OWNER");
        addMember(mapp, bich,  "PM");
        addMember(mapp, huong, "MEMBER");
        addMember(mapp, duc,   "MEMBER");
        addMember(mapp, kiet,  "MEMBER");

        // Kanban board
        Board mboard = boardRepository.save(Board.builder()
                .project(mapp).name("Kanban Board")
                .position(0)
                .boardType(Board.BoardType.KANBAN)
                .isDefault(true)
                .build());

        BoardColumn mColBacklog = col(mboard, "Backlog",      0, "#6b7280", Task.TaskStatus.TODO,        false);
        BoardColumn mColTodo    = col(mboard, "To Do",        1, "#64748b", Task.TaskStatus.TODO,        false);
        BoardColumn mColDoing   = col(mboard, "In Progress",  2, "#3b82f6", Task.TaskStatus.IN_PROGRESS, false);
        BoardColumn mColReview  = col(mboard, "Review",       3, "#f59e0b", Task.TaskStatus.IN_REVIEW,   false);
        BoardColumn mColDone    = col(mboard, "Done",         4, "#22c55e", Task.TaskStatus.DONE,        true);

        Label mlMobile = saveLabel(mapp, "Mobile",   "#0ea5e9");
        Label mlApi    = saveLabel(mapp, "API",      "#f59e0b");
        Label mlUx     = saveLabel(mapp, "UI/UX",    "#ec4899");
        Label mlBug    = saveLabel(mapp, "Bug",      "#ef4444");

        IssueCategory mCatFeature = saveCategory(mapp, "Tính năng", huong);
        IssueCategory mCatBug     = saveCategory(mapp, "Bug",       kiet);

        // Tasks KANBAN dự án 2
        Task m1 = saveTask(mapp, 1, "Màn hình đăng nhập",
                "Login screen: email/password, biometric, remember me, forgot password flow.",
                Task.TaskPriority.HIGH, Task.TaskStatus.DONE,
                huong, an,
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 7),
                bd(16), bd(15), null, mCatFeature,
                null, mboard, mColDone, bd(1000));
        addLabels(m1, mlMobile, mlUx);
        comment(m1, an, "Biometric login hoạt động tốt trên iOS. Android cần test thêm.");
        comment(m1, huong, "Đã test trên Pixel 6 và Samsung S23 — OK cả hai.");

        Task m2 = saveTask(mapp, 2, "Màn hình danh sách deals",
                "Hiển thị deals được giao, lọc theo stage và priority, pull-to-refresh.",
                Task.TaskPriority.HIGH, Task.TaskStatus.DONE,
                huong, an,
                LocalDate.of(2026, 3, 5), LocalDate.of(2026, 3, 12),
                bd(20), bd(21), null, mCatFeature,
                null, mboard, mColDone, bd(2000));
        addLabels(m2, mlMobile, mlUx);
        logTime(m2, huong, "Implement DealList screen với filters", 10.0, LocalDate.of(2026, 3, 10));

        Task m3 = saveTask(mapp, 3, "API mobile: đồng bộ dữ liệu offline",
                "Sync strategy: local SQLite cache, conflict resolution, background sync khi có mạng.",
                Task.TaskPriority.HIGH, Task.TaskStatus.IN_PROGRESS,
                duc, an,
                LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 20),
                bd(24), null, null, mCatFeature,
                null, mboard, mColDoing, bd(3000));
        addLabels(m3, mlApi);
        logTime(m3, duc, "SQLite schema + sync service skeleton", 8.0, LocalDate.of(2026, 3, 15));
        comment(m3, duc, "Conflict resolution phức tạp hơn dự tính. Đang nghiên cứu CRDT approach.");
        comment(m3, bich, "CRDT overkill cho use case này. Last-write-wins với server timestamp là đủ rồi.");
        comment(m3, duc,  "Đồng ý, đổi sang LWW. Đơn giản hơn nhiều và đủ dùng.");

        Task m4 = saveTask(mapp, 4, "Push notification",
                "Tích hợp Firebase FCM, gửi notification khi deal được cập nhật hoặc có reminder.",
                Task.TaskPriority.MEDIUM, Task.TaskStatus.IN_REVIEW,
                duc, an,
                LocalDate.of(2026, 3, 12), LocalDate.of(2026, 3, 18),
                bd(12), bd(12), null, mCatFeature,
                null, mboard, mColReview, bd(4000));
        addLabels(m4, mlApi, mlMobile);
        logTime(m4, duc, "FCM integration và notification handler", 8.0, LocalDate.of(2026, 3, 16));
        comment(m4, kiet, "Test notification: nhận được đúng content, deep link vào đúng màn hình. iOS cần xin permission lần đầu — UX flow OK.");
        comment(m4, duc, "Cảm ơn @kiet.hoang. Đã xử lý edge case notification khi app ở foreground.");

        Task m5 = saveTask(mapp, 5, "Fix bug: App crash khi mở deal có nhiều notes",
                "Crash khi deal có >50 notes, lỗi `OutOfMemoryError` trong FlatList render.",
                Task.TaskPriority.HIGH, Task.TaskStatus.TODO,
                huong, kiet,
                LocalDate.of(2026, 3, 20), LocalDate.of(2026, 3, 22),
                bd(4), null, null, mCatBug,
                null, mboard, mColTodo, bd(5000));
        addLabels(m5, mlBug, mlMobile);
        comment(m5, kiet, "Crash log: `RangeError: Maximum call stack size exceeded` trong NotesList. Deal test có 78 notes.");
        comment(m5, huong, "Root cause: FlatList render all items không virtualise đúng. Fix bằng cách thêm `initialNumToRender` và `windowSize`.");

        Task m6 = saveTask(mapp, 6, "Màn hình chi tiết khách hàng",
                "Profile khách hàng: thông tin liên hệ, lịch sử deals, timeline hoạt động, ghi chú nhanh.",
                Task.TaskPriority.MEDIUM, Task.TaskStatus.TODO,
                huong, an,
                null, null,
                bd(20), null, null, mCatFeature,
                null, mboard, mColBacklog, bd(6000));
        addLabels(m6, mlMobile, mlUx);

        watch(m3, an);
        watch(m4, an);
        watch(m5, an);
        watch(m5, bich);

        log.info("Demo data seeded: 2 projects (CRM: 16 tasks / 4 sprints, MAPP: 6 tasks Kanban), 6 users.");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Role findRole(String name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new IllegalStateException(
                        "System role '" + name + "' not found — DataInitializer chưa chạy."));
    }

    private User upsertUser(String username, String fullName, String email, boolean mustChange) {
        return userRepository.findByEmail(email).orElseGet(() ->
                userRepository.save(User.builder()
                        .username(username)
                        .email(email)
                        .passwordHash(passwordEncoder.encode(DEMO_PASSWORD))
                        .fullName(fullName)
                        .isActive(true)
                        .emailVerified(true)
                        .mustChangePassword(mustChange)
                        .build()));
    }

    private void grantSystemRole(User user, Role role) {
        if (!userRoleRepository.existsByUserIdAndRoleId(user.getId(), role.getId())) {
            userRoleRepository.save(UserRole.builder().user(user).role(role).build());
        }
    }

    private void addMember(Project project, User user, String role) {
        projectMemberRepository.save(ProjectMember.builder()
                .project(project).user(user).role(role).build());
    }

    private Label saveLabel(Project project, String name, String color) {
        return labelRepository.save(Label.builder()
                .project(project).name(name).color(color).build());
    }

    private IssueCategory saveCategory(Project project, String name, User defaultAssignee) {
        return issueCategoryRepository.save(IssueCategory.builder()
                .project(project).name(name).defaultAssignee(defaultAssignee).build());
    }

    private Sprint saveSprint(Project project, String name, String goal,
                              Sprint.SprintStatus status, LocalDate start, LocalDate end) {
        int nextPos = boardRepository.findMaxPositionByProjectId(project.getId()).orElse(0) + 1;
        Board sprintBoard = boardRepository.save(Board.builder()
                .project(project).name(name).position(nextPos)
                .boardType(Board.BoardType.SCRUM).isDefault(false)
                .build());

        boardColumnRepository.save(BoardColumn.builder().board(sprintBoard).name("To Do")      .position(0).color("#6B7280").mappedStatus(Task.TaskStatus.TODO)       .isCompleted(false).build());
        boardColumnRepository.save(BoardColumn.builder().board(sprintBoard).name("In Progress").position(1).color("#3B82F6").mappedStatus(Task.TaskStatus.IN_PROGRESS).isCompleted(false).build());
        boardColumnRepository.save(BoardColumn.builder().board(sprintBoard).name("In Review")  .position(2).color("#F59E0B").mappedStatus(Task.TaskStatus.IN_REVIEW)  .isCompleted(false).build());
        boardColumnRepository.save(BoardColumn.builder().board(sprintBoard).name("Resolved")   .position(3).color("#8B5CF6").mappedStatus(Task.TaskStatus.RESOLVED)   .isCompleted(false).build());
        boardColumnRepository.save(BoardColumn.builder().board(sprintBoard).name("Done")       .position(4).color("#10B981").mappedStatus(Task.TaskStatus.DONE)       .isCompleted(true) .build());
        boardColumnRepository.save(BoardColumn.builder().board(sprintBoard).name("Cancelled")  .position(5).color("#EF4444").mappedStatus(Task.TaskStatus.CANCELLED)  .isCompleted(false).build());

        Sprint sprint = Sprint.builder()
                .project(project).board(sprintBoard).name(name).goal(goal)
                .status(status).startDate(start).endDate(end)
                .build();
        if (status == Sprint.SprintStatus.COMPLETED) {
            sprint.setCompletedAt(end.atTime(17, 0));
        }
        return sprintRepository.save(sprint);
    }

    private Map<Task.TaskStatus, BoardColumn> sprintCols(Sprint sprint) {
        List<BoardColumn> cols = boardColumnRepository.findByBoardIdOrderByPositionAsc(sprint.getBoard().getId());
        Map<Task.TaskStatus, BoardColumn> map = new EnumMap<>(Task.TaskStatus.class);
        for (BoardColumn c : cols) {
            if (c.getMappedStatus() != null) map.put(c.getMappedStatus(), c);
        }
        return map;
    }

    private BoardColumn col(Board board, String name, int position, String color,
                            Task.TaskStatus status, boolean isCompleted) {
        return boardColumnRepository.save(BoardColumn.builder()
                .board(board).name(name).position(position).color(color)
                .mappedStatus(status).isCompleted(isCompleted)
                .build());
    }

    private Task saveTask(Project project, int number, String title, String description,
                          Task.TaskPriority priority, Task.TaskStatus status,
                          User assignee, User reporter,
                          LocalDate startDate, LocalDate dueDate,
                          BigDecimal estimated, BigDecimal actual,
                          Task parentTask, IssueCategory category,
                          Sprint sprint, Board board, BoardColumn column, BigDecimal position) {
        Task task = Task.builder()
                .project(project).board(board).taskNumber(number)
                .title(title).description(description)
                .priority(priority).status(status)
                .assignee(assignee).reporter(reporter)
                .startDate(startDate).dueDate(dueDate)
                .estimatedHours(estimated).actualHours(actual)
                .parentTask(parentTask).category(category)
                .sprint(sprint).column(column).position(position)
                .build();
        if (status == Task.TaskStatus.DONE || status == Task.TaskStatus.RESOLVED) {
            task.setCompletedAt(dueDate != null ? dueDate.atTime(16, 30) : LocalDateTime.now());
        }
        return taskRepository.save(task);
    }

    private void addLabels(Task task, Label... labels) {
        for (Label label : labels) {
            taskLabelRepository.save(TaskLabel.builder().task(task).label(label).build());
        }
    }

    private void logTime(Task task, User user, String description, double hours, LocalDate date) {
        timeTrackingRepository.save(TimeTracking.builder()
                .task(task).user(user).description(description)
                .hours(BigDecimal.valueOf(hours)).workDate(date)
                .build());
    }

    private void comment(Task task, User user, String content) {
        commentRepository.save(Comment.builder()
                .task(task).user(user).content(content).build());
    }

    private void watch(Task task, User user) {
        taskWatcherRepository.save(TaskWatcher.builder().task(task).user(user).build());
    }

    private void saveNotification(User user, Notification.NotificationType type,
                                  String title, String message,
                                  Notification.RelatedType relatedType, java.util.UUID relatedId,
                                  boolean isRead) {
        Notification n = Notification.builder()
                .user(user).type(type).title(title).message(message)
                .relatedType(relatedType).relatedId(relatedId)
                .isRead(isRead)
                .build();
        if (isRead) n.setReadAt(LocalDateTime.now().minusHours(2));
        notificationRepository.save(n);
    }

    private static BigDecimal bd(double v) {
        return BigDecimal.valueOf(v);
    }
}
