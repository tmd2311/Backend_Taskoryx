package com.taskoryx.backend.config;

import com.taskoryx.backend.entity.*;
import com.taskoryx.backend.repository.*;
import com.taskoryx.backend.repository.TaskLabelRepository;
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
 * Tạo dữ liệu demo thực tế khi app khởi động lần đầu.
 * Chỉ chạy nếu chưa có project nào trong DB.
 *
 * Kịch bản: Dự án "Nền tảng thương mại điện tử" (key: TMDT)
 * - 5 thành viên (admin là OWNER)
 * - 7 labels, 4 issue categories
 * - 4 sprints (3 COMPLETED + 1 ACTIVE)
 * - 25 tasks với comments, time tracking
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(100)
public class DemoDataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final BoardRepository boardRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final SprintRepository sprintRepository;
    private final TaskRepository taskRepository;
    private final LabelRepository labelRepository;
    private final IssueCategoryRepository issueCategoryRepository;
    private final CommentRepository commentRepository;
    private final TimeTrackingRepository timeTrackingRepository;
    private final PasswordEncoder passwordEncoder;
    private final TaskLabelRepository taskLabelRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (projectRepository.count() > 0) {
            log.info("Demo data already exists, skipping.");
            return;
        }

        log.info("Creating demo project data...");

        User admin = userRepository.findByEmail("admin@taskoryx.com")
                .orElseGet(() -> userRepository.save(User.builder()
                        .username("admin")
                        .email("admin@taskoryx.com")
                        .passwordHash(passwordEncoder.encode("Admin@123456"))
                        .fullName("System Administrator")
                        .isActive(true)
                        .emailVerified(true)
                        .build()));

        // ── 1. Tạo thành viên ──────────────────────────────────────────────
        User nam   = createUser("le_namhp",     "Lê Hoàng Nam",     "nam@taskoryx.com");
        User duc   = createUser("nguyen_ducnv",  "Nguyễn Văn Đức",   "duc@taskoryx.com");
        User lan   = createUser("tran_lant",     "Trần Thị Lan",     "lan@taskoryx.com");
        User tuan  = createUser("pham_tuanpm",   "Phạm Minh Tuấn",   "tuan@taskoryx.com");

        // ── 2. Tạo project ─────────────────────────────────────────────────
        Project project = projectRepository.save(Project.builder()
                .name("Nền tảng thương mại điện tử")
                .description("Xây dựng nền tảng mua sắm trực tuyến với đầy đủ tính năng: quản lý sản phẩm, giỏ hàng, thanh toán, đơn hàng và báo cáo.")
                .key("TMDT")
                .owner(admin)
                .color("#7c3aed")
                .icon("shop")
                .isPublic(false)
                .isArchived(false)
                .build());

        // ── 3. Thêm thành viên — role phải thuộc (OWNER|ADMIN|MEMBER|VIEWER) ──
        addMember(project, admin, "OWNER");
        addMember(project, nam,   "ADMIN");
        addMember(project, duc,   "MEMBER");
        addMember(project, lan,   "MEMBER");
        addMember(project, tuan,  "MEMBER");

        // ── 4. Tạo Kanban board + columns (có mapped_status) ───────────────
        Board board = boardRepository.save(Board.builder()
                .project(project).name("Kanban Board").position(0)
                .boardType(Board.BoardType.KANBAN)
                .isDefault(true)
                .build());

        BoardColumn colTodo   = saveColumn(board, "Cần làm",     0, "#6b7280", Task.TaskStatus.TODO,        false);
        BoardColumn colInProg = saveColumn(board, "Đang làm",    1, "#3b82f6", Task.TaskStatus.IN_PROGRESS, false);
        BoardColumn colReview = saveColumn(board, "Đang review", 2, "#f59e0b", Task.TaskStatus.IN_REVIEW,   false);
        BoardColumn colDone   = saveColumn(board, "Đã xong",     3, "#22c55e", Task.TaskStatus.DONE,        true);

        // ── 5. Labels ──────────────────────────────────────────────────────
        Label lFe   = saveLabel(project, "Frontend",    "#3b82f6");
        Label lBe   = saveLabel(project, "Backend",     "#22c55e");
        Label lApi  = saveLabel(project, "API",         "#f59e0b");
        Label lDb   = saveLabel(project, "Database",    "#8b5cf6");
        Label lUx   = saveLabel(project, "UI/UX",       "#ec4899");
        Label lCrit = saveLabel(project, "Critical",    "#ef4444");
        Label lPerf = saveLabel(project, "Performance", "#f97316");

        // ── 6. Issue categories ────────────────────────────────────────────
        IssueCategory catBug     = saveCategory(project, "Bug",        tuan);
        IssueCategory catFeature = saveCategory(project, "Tính năng",  duc);
        IssueCategory catImprove = saveCategory(project, "Cải thiện",  null);
        IssueCategory catDoc     = saveCategory(project, "Tài liệu",   nam);

        // ── 7. Sprints — mỗi sprint tạo SCRUM board riêng ─────────────────
        Sprint sp1 = saveSprint(project, "Sprint 1 — Nền tảng",
                "Thiết lập hạ tầng, xác thực người dùng, trang chủ cơ bản.",
                Sprint.SprintStatus.COMPLETED,
                LocalDate.of(2026, 1, 6), LocalDate.of(2026, 1, 19));

        Sprint sp2 = saveSprint(project, "Sprint 2 — Sản phẩm",
                "Quản lý sản phẩm, tìm kiếm, upload ảnh.",
                Sprint.SprintStatus.COMPLETED,
                LocalDate.of(2026, 1, 20), LocalDate.of(2026, 2, 2));

        Sprint sp3 = saveSprint(project, "Sprint 3 — Mua sắm",
                "Giỏ hàng, thanh toán VNPay, tối ưu query.",
                Sprint.SprintStatus.COMPLETED,
                LocalDate.of(2026, 2, 3), LocalDate.of(2026, 2, 16));

        Sprint sp4 = saveSprint(project, "Sprint 4 — Đơn hàng",
                "Quản lý đơn hàng, email thông báo, dashboard thống kê.",
                Sprint.SprintStatus.ACTIVE,
                LocalDate.of(2026, 3, 17), LocalDate.of(2026, 3, 30));

        Sprint sp5 = saveSprint(project, "Sprint 5 — Tiện ích",
                "Rating sản phẩm, yêu thích, cải thiện UX, xuất báo cáo, khuyến mãi.",
                Sprint.SprintStatus.PLANNED,
                LocalDate.of(2026, 4, 28), LocalDate.of(2026, 5, 11));

        // Load sprint columns từ DB để dùng cho assignTasksToSprint
        Map<Task.TaskStatus, BoardColumn> sp1Cols = loadSprintColumns(sp1);
        Map<Task.TaskStatus, BoardColumn> sp2Cols = loadSprintColumns(sp2);
        Map<Task.TaskStatus, BoardColumn> sp3Cols = loadSprintColumns(sp3);
        Map<Task.TaskStatus, BoardColumn> sp4Cols = loadSprintColumns(sp4);
        Map<Task.TaskStatus, BoardColumn> sp5Cols = loadSprintColumns(sp5);

        // ── 9. Tasks Sprint 1 (COMPLETED) ──────────────────────────────────
        Task t1 = saveTask(project, 1, "Thiết kế database schema",
                "Thiết kế toàn bộ schema cho các bảng: users, products, orders, payments, categories.",
                Task.TaskPriority.HIGH, Task.TaskStatus.DONE,
                duc, admin, LocalDate.of(2026, 1, 6), LocalDate.of(2026, 1, 9),
                BigDecimal.valueOf(16), BigDecimal.valueOf(18),
                null, catFeature, sp1, sp1.getBoard(), sp1Cols.get(Task.TaskStatus.DONE), new BigDecimal("1000"));
        addLabels(t1, lBe, lDb);
        addTimeLog(t1, duc, "Phân tích yêu cầu và thiết kế ER diagram", 8, LocalDate.of(2026, 1, 7));
        addTimeLog(t1, duc, "Viết migration script", 6, LocalDate.of(2026, 1, 8));
        addComment(t1, admin, "Schema trông ổn, tuy nhiên cần thêm index cho cột `product_id` trong bảng `order_items` để tối ưu query thống kê.");
        addComment(t1, duc,   "Đã thêm index. Cũng đã tạo composite index cho (user_id, created_at) trong bảng orders.");

        Task t2 = saveTask(project, 2, "Cài đặt môi trường development",
                "Cấu hình Docker Compose, CI/CD pipeline, môi trường dev/staging.",
                Task.TaskPriority.HIGH, Task.TaskStatus.DONE,
                nam, admin, LocalDate.of(2026, 1, 6), LocalDate.of(2026, 1, 8),
                BigDecimal.valueOf(8), BigDecimal.valueOf(10),
                null, catFeature, sp1, sp1.getBoard(), sp1Cols.get(Task.TaskStatus.DONE), new BigDecimal("2000"));
        addLabels(t2, lBe);
        addTimeLog(t2, nam, "Cấu hình Docker Compose và environment variables", 5, LocalDate.of(2026, 1, 7));
        addComment(t2, nam, "Đã setup xong môi trường. PostgreSQL chạy ở port 5432, Redis port 6379. Mọi người clone repo và chạy `docker-compose up -d` là được.");

        Task t3 = saveTask(project, 3, "Thiết kế giao diện trang chủ",
                "Thiết kế mockup và implement trang chủ: banner, danh mục nổi bật, sản phẩm gợi ý.",
                Task.TaskPriority.MEDIUM, Task.TaskStatus.DONE,
                lan, admin, LocalDate.of(2026, 1, 8), LocalDate.of(2026, 1, 14),
                BigDecimal.valueOf(20), BigDecimal.valueOf(22),
                null, catFeature, sp1, sp1.getBoard(), sp1Cols.get(Task.TaskStatus.DONE), new BigDecimal("3000"));
        addLabels(t3, lFe, lUx);
        addTimeLog(t3, lan, "Thiết kế wireframe và prototype Figma", 6, LocalDate.of(2026, 1, 9));
        addTimeLog(t3, lan, "Implement component Banner và CategoryGrid", 8, LocalDate.of(2026, 1, 13));

        Task t4 = saveTask(project, 4, "API xác thực người dùng",
                "Implement JWT authentication: đăng ký, đăng nhập, refresh token, đổi mật khẩu.",
                Task.TaskPriority.HIGH, Task.TaskStatus.DONE,
                duc, admin, LocalDate.of(2026, 1, 8), LocalDate.of(2026, 1, 15),
                BigDecimal.valueOf(16), BigDecimal.valueOf(14),
                null, catFeature, sp1, sp1.getBoard(), sp1Cols.get(Task.TaskStatus.DONE), new BigDecimal("4000"));
        addLabels(t4, lBe, lApi);
        addTimeLog(t4, duc, "Implement JWT filter và UserDetails", 5, LocalDate.of(2026, 1, 10));
        addTimeLog(t4, duc, "Viết unit test cho auth endpoints", 4, LocalDate.of(2026, 1, 14));
        addComment(t4, tuan, "Đã test tất cả auth endpoints, hoạt động tốt. Tuy nhiên cần xử lý case khi refresh token đã hết hạn trả về 401 thay vì 500.");
        addComment(t4, duc,  "Đã fix. Thêm `TokenExpiredException` vào GlobalExceptionHandler rồi.");

        Task t5 = saveTask(project, 5, "Trang đăng ký / đăng nhập",
                "Implement form đăng ký, đăng nhập với validation, xử lý lỗi và UX flows.",
                Task.TaskPriority.MEDIUM, Task.TaskStatus.DONE,
                lan, admin, LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 17),
                BigDecimal.valueOf(12), BigDecimal.valueOf(13),
                null, catFeature, sp1, sp1.getBoard(), sp1Cols.get(Task.TaskStatus.DONE), new BigDecimal("5000"));
        addLabels(t5, lFe, lUx);
        addTimeLog(t5, lan, "Implement form login/register với React Hook Form", 7, LocalDate.of(2026, 1, 15));

        // ── 10. Tasks Sprint 2 (COMPLETED) ─────────────────────────────────
        Task t6 = saveTask(project, 6, "Module quản lý sản phẩm",
                "CRUD sản phẩm: tạo, sửa, xóa, phân trang, lọc theo category, tìm kiếm.",
                Task.TaskPriority.HIGH, Task.TaskStatus.DONE,
                duc, admin, LocalDate.of(2026, 1, 20), LocalDate.of(2026, 1, 27),
                BigDecimal.valueOf(24), BigDecimal.valueOf(26),
                null, catFeature, sp2, sp2.getBoard(), sp2Cols.get(Task.TaskStatus.DONE), new BigDecimal("1000"));
        addLabels(t6, lBe, lApi);
        addTimeLog(t6, duc, "Implement CRUD endpoints sản phẩm", 10, LocalDate.of(2026, 1, 22));
        addTimeLog(t6, duc, "Viết unit test và integration test", 6, LocalDate.of(2026, 1, 25));

        Task t7 = saveTask(project, 7, "Giao diện danh sách sản phẩm",
                "Trang danh sách sản phẩm: grid/list view, filter sidebar, sort, phân trang.",
                Task.TaskPriority.HIGH, Task.TaskStatus.DONE,
                lan, admin, LocalDate.of(2026, 1, 22), LocalDate.of(2026, 1, 29),
                BigDecimal.valueOf(20), BigDecimal.valueOf(21),
                null, catFeature, sp2, sp2.getBoard(), sp2Cols.get(Task.TaskStatus.DONE), new BigDecimal("2000"));
        addLabels(t7, lFe, lUx);
        addTimeLog(t7, lan, "Implement ProductGrid và FilterSidebar", 9, LocalDate.of(2026, 1, 27));
        addComment(t7, lan, "Đã implement xong grid view. List view sẽ làm trong sprint sau nếu còn thời gian.");
        addComment(t7, nam, "OK, grid view là đủ cho v1.0. List view để sprint sau nhé.");

        Task t8 = saveTask(project, 8, "Tính năng tìm kiếm sản phẩm",
                "Full-text search sản phẩm theo tên, mô tả, category với autocomplete.",
                Task.TaskPriority.MEDIUM, Task.TaskStatus.DONE,
                duc, admin, LocalDate.of(2026, 1, 23), LocalDate.of(2026, 1, 30),
                BigDecimal.valueOf(16), BigDecimal.valueOf(15),
                null, catFeature, sp2, sp2.getBoard(), sp2Cols.get(Task.TaskStatus.DONE), new BigDecimal("3000"));
        addLabels(t8, lBe, lPerf);
        addTimeLog(t8, duc, "Implement full-text search với PostgreSQL tsvector", 8, LocalDate.of(2026, 1, 28));

        Task t9 = saveTask(project, 9, "Fix bug: Lỗi validation form đăng ký",
                "Form đăng ký không hiển thị thông báo lỗi khi email đã tồn tại. Server trả về 400 nhưng FE không bắt được.",
                Task.TaskPriority.HIGH, Task.TaskStatus.DONE,
                lan, tuan, LocalDate.of(2026, 1, 25), LocalDate.of(2026, 1, 27),
                BigDecimal.valueOf(4), BigDecimal.valueOf(3),
                null, catBug, sp2, sp2.getBoard(), sp2Cols.get(Task.TaskStatus.DONE), new BigDecimal("4000"));
        addLabels(t9, lFe, lCrit);
        addComment(t9, tuan, "Bug được tìm thấy khi test flow đăng ký. Response body có `success: false` nhưng FE chỉ check HTTP status code 200.");
        addComment(t9, lan,  "Đã fix. Giờ interceptor axios kiểm tra cả `data.success` trước khi resolve promise.");
        addComment(t9, tuan, "Đã retest, confirmed fixed. Closing.");

        Task t10 = saveTask(project, 10, "Upload ảnh sản phẩm",
                "Tích hợp upload ảnh sản phẩm: multipart/form-data, resize tự động, lưu local storage.",
                Task.TaskPriority.MEDIUM, Task.TaskStatus.DONE,
                duc, admin, LocalDate.of(2026, 1, 27), LocalDate.of(2026, 2, 1),
                BigDecimal.valueOf(12), BigDecimal.valueOf(11),
                null, catFeature, sp2, sp2.getBoard(), sp2Cols.get(Task.TaskStatus.DONE), new BigDecimal("5000"));
        addLabels(t10, lBe);
        addTimeLog(t10, duc, "Implement file upload endpoint với validation", 6, LocalDate.of(2026, 1, 29));

        // ── 11. Tasks Sprint 3 (COMPLETED) ─────────────────────────────────
        Task t11 = saveTask(project, 11, "Module giỏ hàng",
                "API giỏ hàng: thêm/xóa/cập nhật số lượng sản phẩm, tính tổng tiền, kiểm tra tồn kho.",
                Task.TaskPriority.HIGH, Task.TaskStatus.DONE,
                duc, admin, LocalDate.of(2026, 2, 3), LocalDate.of(2026, 2, 10),
                BigDecimal.valueOf(20), BigDecimal.valueOf(22),
                null, catFeature, sp3, sp3.getBoard(), sp3Cols.get(Task.TaskStatus.DONE), new BigDecimal("1000"));
        addLabels(t11, lBe, lApi);
        addTimeLog(t11, duc, "Implement cart service và repository", 8, LocalDate.of(2026, 2, 5));
        addTimeLog(t11, duc, "Xử lý race condition khi cập nhật tồn kho", 4, LocalDate.of(2026, 2, 7));

        Task t12 = saveTask(project, 12, "Giao diện giỏ hàng",
                "Trang giỏ hàng: danh sách sản phẩm, cập nhật số lượng real-time, tính toán giá.",
                Task.TaskPriority.HIGH, Task.TaskStatus.DONE,
                lan, admin, LocalDate.of(2026, 2, 5), LocalDate.of(2026, 2, 12),
                BigDecimal.valueOf(16), BigDecimal.valueOf(17),
                null, catFeature, sp3, sp3.getBoard(), sp3Cols.get(Task.TaskStatus.DONE), new BigDecimal("2000"));
        addLabels(t12, lFe, lUx);
        addTimeLog(t12, lan, "Implement CartPage và CartItem component", 10, LocalDate.of(2026, 2, 10));
        addComment(t12, lan, "Đã xong. Số lượng update optimistic UI, nếu API fail thì revert lại.");

        Task t13 = saveTask(project, 13, "Tích hợp cổng thanh toán VNPay",
                "Tích hợp VNPay: tạo URL thanh toán, xử lý callback, cập nhật trạng thái đơn hàng.",
                Task.TaskPriority.HIGH, Task.TaskStatus.DONE,
                duc, admin, LocalDate.of(2026, 2, 7), LocalDate.of(2026, 2, 14),
                BigDecimal.valueOf(24), BigDecimal.valueOf(28),
                null, catFeature, sp3, sp3.getBoard(), sp3Cols.get(Task.TaskStatus.DONE), new BigDecimal("3000"));
        addLabels(t13, lBe, lApi);
        addTimeLog(t13, duc, "Implement VNPay integration và callback handler", 12, LocalDate.of(2026, 2, 11));
        addTimeLog(t13, duc, "Test và fix edge cases", 6, LocalDate.of(2026, 2, 13));
        addComment(t13, tuan, "Test payment flow xong. Tất cả 4 scenarios (success, fail, timeout, cancel) đều hoạt động đúng.");

        Task t14 = saveTask(project, 14, "Fix bug: Giỏ hàng không cập nhật realtime",
                "Khi nhiều tab cùng mở, thay đổi số lượng ở tab này không sync sang tab kia.",
                Task.TaskPriority.HIGH, Task.TaskStatus.DONE,
                lan, tuan, LocalDate.of(2026, 2, 8), LocalDate.of(2026, 2, 10),
                BigDecimal.valueOf(8), BigDecimal.valueOf(6),
                null, catBug, sp3, sp3.getBoard(), sp3Cols.get(Task.TaskStatus.DONE), new BigDecimal("4000"));
        addLabels(t14, lFe, lCrit);
        addComment(t14, tuan, "Reproduce được. Mở 2 tab, tab 1 thêm sản phẩm vào giỏ → tab 2 không cập nhật số lượng trên icon giỏ hàng.");
        addComment(t14, lan,  "Fix bằng cách dùng BroadcastChannel API để sync state giữa các tab.");

        Task t15 = saveTask(project, 15, "Tối ưu query danh sách sản phẩm",
                "Query lấy danh sách sản phẩm đang chạy 2-3s. Cần tối ưu xuống dưới 200ms.",
                Task.TaskPriority.HIGH, Task.TaskStatus.DONE,
                duc, admin, LocalDate.of(2026, 2, 10), LocalDate.of(2026, 2, 15),
                BigDecimal.valueOf(12), BigDecimal.valueOf(10),
                null, catImprove, sp3, sp3.getBoard(), sp3Cols.get(Task.TaskStatus.DONE), new BigDecimal("5000"));
        addLabels(t15, lBe, lPerf, lDb);
        addTimeLog(t15, duc, "Phân tích query và thêm index", 4, LocalDate.of(2026, 2, 11));
        addTimeLog(t15, duc, "Implement Redis cache layer", 5, LocalDate.of(2026, 2, 13));
        addComment(t15, duc, "Sau khi thêm composite index (category_id, status, price) và cache Redis 5 phút, query xuống còn ~80ms. Đạt mục tiêu.");

        // ── 12. Tasks Sprint 4 (ACTIVE) ────────────────────────────────────
        Task t16 = saveTask(project, 16, "Module quản lý đơn hàng",
                "API đơn hàng: tạo đơn, xem chi tiết, cập nhật trạng thái, hủy đơn, lịch sử.",
                Task.TaskPriority.HIGH, Task.TaskStatus.IN_PROGRESS,
                duc, admin, LocalDate.of(2026, 3, 17), LocalDate.of(2026, 3, 24),
                BigDecimal.valueOf(24), null,
                null, catFeature, sp4, sp4.getBoard(), sp4Cols.get(Task.TaskStatus.IN_PROGRESS), new BigDecimal("1000"));
        addLabels(t16, lBe, lApi);
        addTimeLog(t16, duc, "Implement tạo đơn hàng và danh sách đơn", 8, LocalDate.of(2026, 3, 20));
        addComment(t16, duc, "Đã xong API tạo đơn và lấy danh sách. Đang implement cập nhật trạng thái.");
        addComment(t16, nam, "Nhớ implement state machine cho order status: PENDING → CONFIRMED → SHIPPING → DELIVERED. Không cho phép nhảy cóc.");

        // Subtask của t16 — nằm trong sprint (column = TODO của sp4)
        Task t16a = saveTask(project, 20, "Order state machine",
                "Implement state machine kiểm soát luồng chuyển trạng thái đơn hàng.",
                Task.TaskPriority.HIGH, Task.TaskStatus.TODO,
                duc, admin, LocalDate.of(2026, 3, 22), LocalDate.of(2026, 3, 24),
                BigDecimal.valueOf(6), null,
                t16, catFeature, sp4, sp4.getBoard(), sp4Cols.get(Task.TaskStatus.TODO), BigDecimal.ZERO);
        addLabels(t16a, lBe);

        Task t17 = saveTask(project, 17, "Giao diện quản lý đơn hàng",
                "Trang quản lý đơn hàng cho admin: danh sách, lọc theo trạng thái, xem chi tiết, cập nhật.",
                Task.TaskPriority.HIGH, Task.TaskStatus.TODO,
                lan, admin, LocalDate.of(2026, 3, 21), LocalDate.of(2026, 3, 27),
                BigDecimal.valueOf(20), null,
                null, catFeature, sp4, sp4.getBoard(), sp4Cols.get(Task.TaskStatus.TODO), new BigDecimal("2000"));
        addLabels(t17, lFe, lUx);

        Task t18 = saveTask(project, 18, "Tích hợp email thông báo đơn hàng",
                "Gửi email tự động: xác nhận đơn, cập nhật trạng thái, thông báo giao hàng.",
                Task.TaskPriority.MEDIUM, Task.TaskStatus.IN_REVIEW,
                duc, admin, LocalDate.of(2026, 3, 17), LocalDate.of(2026, 3, 22),
                BigDecimal.valueOf(12), BigDecimal.valueOf(11),
                null, catFeature, sp4, sp4.getBoard(), sp4Cols.get(Task.TaskStatus.IN_REVIEW), new BigDecimal("3000"));
        addLabels(t18, lBe);
        addTimeLog(t18, duc, "Tạo email templates và implement email service", 8, LocalDate.of(2026, 3, 19));
        addComment(t18, duc,   "Đã implement xong. Dùng Thymeleaf template cho email HTML. Gửi async để không block request.");
        addComment(t18, tuan,  "Đang test các email template. Banner ảnh trong email bị vỡ layout trên Gmail mobile.");
        addComment(t18, duc,   "Đã fix CSS inline cho email. Gmail không support external CSS nên cần inline hết.");

        Task t19 = saveTask(project, 19, "Fix bug: Lỗi hiển thị giá khi có discount",
                "Khi sản phẩm có discount, giá hiển thị trong giỏ hàng khác với giá ở trang thanh toán.",
                Task.TaskPriority.HIGH, Task.TaskStatus.IN_PROGRESS,
                lan, tuan, LocalDate.of(2026, 3, 24), LocalDate.of(2026, 3, 26),
                BigDecimal.valueOf(4), null,
                null, catBug, sp4, sp4.getBoard(), sp4Cols.get(Task.TaskStatus.IN_PROGRESS), new BigDecimal("4000"));
        addLabels(t19, lFe, lCrit);
        addComment(t19, tuan, "Reproduce: Thêm sản phẩm có 20% discount vào giỏ → giá ở CartPage hiển thị đúng → sang CheckoutPage giá lại thành giá gốc.");
        addComment(t19, lan,  "Root cause: CartPage tính discount ở frontend, CheckoutPage gọi lại API lấy price gốc. Đang fix để dùng discountedPrice từ API.");

        // ── 13. Tasks Sprint 5 (PLANNED) ───────────────────────────────────
        Task t21 = saveTask(project, 21, "Hệ thống đánh giá sản phẩm",
                "Cho phép người dùng đánh giá sao và viết nhận xét sản phẩm đã mua. Hiển thị trung bình rating.",
                Task.TaskPriority.MEDIUM, Task.TaskStatus.TODO,
                duc, admin, null, null,
                BigDecimal.valueOf(20), null,
                null, catFeature, sp5, sp5.getBoard(), sp5Cols.get(Task.TaskStatus.TODO), new BigDecimal("1000"));
        addLabels(t21, lBe, lApi);

        Task t22 = saveTask(project, 22, "Tính năng yêu thích sản phẩm",
                "Cho phép lưu sản phẩm yêu thích, đồng bộ giữa các thiết bị.",
                Task.TaskPriority.LOW, Task.TaskStatus.TODO,
                lan, admin, null, null,
                BigDecimal.valueOf(8), null,
                null, catFeature, sp5, sp5.getBoard(), sp5Cols.get(Task.TaskStatus.TODO), new BigDecimal("2000"));
        addLabels(t22, lFe);

        Task t23 = saveTask(project, 23, "Cải thiện UX trang checkout",
                "Rút gọn các bước checkout từ 4 bước xuống 2 bước. Thêm guest checkout.",
                Task.TaskPriority.MEDIUM, Task.TaskStatus.TODO,
                lan, admin, null, null,
                BigDecimal.valueOf(16), null,
                null, catImprove, sp5, sp5.getBoard(), sp5Cols.get(Task.TaskStatus.TODO), new BigDecimal("3000"));
        addLabels(t23, lFe, lUx);

        Task t24 = saveTask(project, 24, "API xuất báo cáo Excel",
                "Xuất báo cáo doanh thu, đơn hàng, sản phẩm bán chạy ra file Excel.",
                Task.TaskPriority.LOW, Task.TaskStatus.TODO,
                duc, admin, null, null,
                BigDecimal.valueOf(12), null,
                null, catFeature, sp5, sp5.getBoard(), sp5Cols.get(Task.TaskStatus.TODO), new BigDecimal("4000"));
        addLabels(t24, lBe, lApi);

        Task t25 = saveTask(project, 25, "Module khuyến mãi và voucher",
                "Tạo và quản lý mã giảm giá: theo %, theo số tiền cố định, giới hạn lượt dùng.",
                Task.TaskPriority.MEDIUM, Task.TaskStatus.TODO,
                duc, admin, null, null,
                BigDecimal.valueOf(28), null,
                null, catFeature, sp5, sp5.getBoard(), sp5Cols.get(Task.TaskStatus.TODO), new BigDecimal("5000"));
        addLabels(t25, lBe, lApi);
        addComment(t25, nam, "Cần confirm với stakeholder về các loại voucher trước khi implement. @nguyen.ducnv bạn có thể họp với PM tuần tới không?");

        log.info("Demo data created successfully: project TMDT with 25 tasks across 5 sprints.");
    }

    // ── HELPER METHODS ──────────────────────────────────────────────────────

    private User createUser(String username, String fullName, String email) {
        return userRepository.findByEmail(email).orElseGet(() ->
            userRepository.save(User.builder()
                    .username(username)
                    .email(email)
                    .passwordHash(passwordEncoder.encode("Demo@123456"))
                    .fullName(fullName)
                    .isActive(true)
                    .emailVerified(true)
                    .mustChangePassword(false)
                    .build()));
    }

    private void addMember(Project project, User user, String role) {
        projectMemberRepository.save(ProjectMember.builder()
                .project(project).user(user).role(role).build());
    }

    private BoardColumn saveColumn(Board board, String name, int position, String color,
                                   Task.TaskStatus mappedStatus, boolean isCompleted) {
        return boardColumnRepository.save(BoardColumn.builder()
                .board(board).name(name).position(position).color(color)
                .mappedStatus(mappedStatus)
                .isCompleted(isCompleted)
                .build());
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
        int nextPosition = boardRepository.findMaxPositionByProjectId(project.getId()).orElse(0) + 1;
        Board sprintBoard = boardRepository.save(Board.builder()
                .project(project)
                .name(name)
                .position(nextPosition)
                .boardType(Board.BoardType.SCRUM)
                .isDefault(false)
                .build());

        boardColumnRepository.save(BoardColumn.builder().board(sprintBoard).name("To Do")      .position(0).color("#6B7280").mappedStatus(Task.TaskStatus.TODO)       .isCompleted(false).build());
        boardColumnRepository.save(BoardColumn.builder().board(sprintBoard).name("In Progress").position(1).color("#3B82F6").mappedStatus(Task.TaskStatus.IN_PROGRESS).isCompleted(false).build());
        boardColumnRepository.save(BoardColumn.builder().board(sprintBoard).name("In Review")  .position(2).color("#F59E0B").mappedStatus(Task.TaskStatus.IN_REVIEW)  .isCompleted(false).build());
        boardColumnRepository.save(BoardColumn.builder().board(sprintBoard).name("Resolved")   .position(3).color("#8B5CF6").mappedStatus(Task.TaskStatus.RESOLVED)   .isCompleted(false).build());
        boardColumnRepository.save(BoardColumn.builder().board(sprintBoard).name("Done")       .position(4).color("#10B981").mappedStatus(Task.TaskStatus.DONE)       .isCompleted(true) .build());
        boardColumnRepository.save(BoardColumn.builder().board(sprintBoard).name("Cancelled")  .position(5).color("#EF4444").mappedStatus(Task.TaskStatus.CANCELLED)  .isCompleted(false).build());

        Sprint sprint = Sprint.builder()
                .project(project).board(sprintBoard).name(name).goal(goal).status(status)
                .startDate(start).endDate(end)
                .build();
        if (status == Sprint.SprintStatus.COMPLETED) {
            sprint.setCompletedAt(end.atTime(17, 0));
        }
        return sprintRepository.save(sprint);
    }

    /** Load sprint board columns từ DB thành map TaskStatus → BoardColumn */
    private Map<Task.TaskStatus, BoardColumn> loadSprintColumns(Sprint sprint) {
        List<BoardColumn> cols = boardColumnRepository.findByBoardIdOrderByPositionAsc(sprint.getBoard().getId());
        Map<Task.TaskStatus, BoardColumn> map = new HashMap<>();
        for (BoardColumn col : cols) {
            if (col.getMappedStatus() != null) {
                map.put(col.getMappedStatus(), col);
            }
        }
        return map;
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
                .parentTask(parentTask)
                .category(category)
                .sprint(sprint)
                .column(column).position(position)
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

    private void addTimeLog(Task task, User user, String description, double hours, LocalDate date) {
        timeTrackingRepository.save(TimeTracking.builder()
                .task(task).user(user).description(description)
                .hours(BigDecimal.valueOf(hours)).workDate(date)
                .build());
    }

    private void addComment(Task task, User user, String content) {
        commentRepository.save(Comment.builder()
                .task(task).user(user).content(content).build());
    }
}
