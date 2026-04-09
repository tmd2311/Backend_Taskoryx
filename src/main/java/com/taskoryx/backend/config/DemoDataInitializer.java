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
 * - 7 labels, 4 issue categories, 3 versions
 * - 4 sprints (3 COMPLETED + 1 ACTIVE)
 * - 25 tasks với comments, checklist, time tracking
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
    private final VersionRepository versionRepository;
    private final CommentRepository commentRepository;
    private final ChecklistItemRepository checklistItemRepository;
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
                .icon("🛍️")
                .isPublic(false)
                .isArchived(false)
                .build());

        // ── 3. Thêm thành viên ─────────────────────────────────────────────
        addMember(project, admin, "OWNER");
        addMember(project, nam,   "ADMIN");
        addMember(project, duc,   "DEVELOPER");
        addMember(project, lan,   "DEVELOPER");
        addMember(project, tuan,  "TESTER");

        // ── 4. Tạo board + columns ─────────────────────────────────────────
        Board board = boardRepository.save(Board.builder()
                .project(project).name("Kanban Board").position(0).isDefault(true)
                .build());

        BoardColumn colTodo     = saveColumn(board, "Cần làm",     1000, "#6b7280", false);
        BoardColumn colInProg   = saveColumn(board, "Đang làm",    2000, "#3b82f6", false);
        BoardColumn colReview   = saveColumn(board, "Đang review", 3000, "#f59e0b", false);
        BoardColumn colDone     = saveColumn(board, "Đã xong",     4000, "#22c55e", true);

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

        // ── 7. Versions ────────────────────────────────────────────────────
        Version v100 = versionRepository.save(Version.builder()
                .project(project).name("v1.0.0").description("Phiên bản MVP đầu tiên — xác thực, sản phẩm cơ bản.")
                .status(Version.VersionStatus.CLOSED)
                .dueDate(LocalDate.of(2026, 1, 19))
                .releaseDate(LocalDate.of(2026, 1, 20))
                .build());

        Version v110 = versionRepository.save(Version.builder()
                .project(project).name("v1.1.0").description("Giỏ hàng, thanh toán VNPay, tối ưu hiệu năng.")
                .status(Version.VersionStatus.CLOSED)
                .dueDate(LocalDate.of(2026, 2, 16))
                .releaseDate(LocalDate.of(2026, 2, 18))
                .build());

        Version v200 = versionRepository.save(Version.builder()
                .project(project).name("v2.0.0").description("Quản lý đơn hàng, dashboard doanh thu, hệ thống khuyến mãi.")
                .status(Version.VersionStatus.OPEN)
                .dueDate(LocalDate.of(2026, 4, 30))
                .build());

        // ── 8. Sprints ─────────────────────────────────────────────────────
        Sprint sp1 = saveSprint(project, board, "Sprint 1 — Nền tảng",
                "Thiết lập hạ tầng, xác thực người dùng, trang chủ cơ bản.",
                Sprint.SprintStatus.COMPLETED,
                LocalDate.of(2026, 1, 6), LocalDate.of(2026, 1, 19));

        Sprint sp2 = saveSprint(project, board, "Sprint 2 — Sản phẩm",
                "Quản lý sản phẩm, tìm kiếm, upload ảnh.",
                Sprint.SprintStatus.COMPLETED,
                LocalDate.of(2026, 1, 20), LocalDate.of(2026, 2, 2));

        Sprint sp3 = saveSprint(project, board, "Sprint 3 — Mua sắm",
                "Giỏ hàng, thanh toán VNPay, tối ưu query.",
                Sprint.SprintStatus.COMPLETED,
                LocalDate.of(2026, 2, 3), LocalDate.of(2026, 2, 16));

        Sprint sp4 = saveSprint(project, board, "Sprint 4 — Đơn hàng",
                "Quản lý đơn hàng, email thông báo, dashboard thống kê.",
                Sprint.SprintStatus.ACTIVE,
                LocalDate.of(2026, 3, 17), LocalDate.of(2026, 3, 30));

        // ── 9. Tasks Sprint 1 (COMPLETED) ──────────────────────────────────
        Task t1 = saveTask(project, board, 1, "Thiết kế database schema",
                "Thiết kế toàn bộ schema cho các bảng: users, products, orders, payments, categories.",
                Task.TaskPriority.HIGH, Task.TaskStatus.DONE,
                duc, admin, LocalDate.of(2026, 1, 6), LocalDate.of(2026, 1, 9),
                BigDecimal.valueOf(16), BigDecimal.valueOf(18),
                null, catFeature, v100, colDone, new BigDecimal("1000"));
        addLabels(t1, lBe, lDb);
        addChecklist(t1, admin, new String[]{
            "Bảng users và phân quyền",
            "Bảng products và categories",
            "Bảng orders và order_items",
            "Bảng payments",
            "Thiết lập index cho query thường dùng"
        }, new boolean[]{true, true, true, true, true});
        addTimeLog(t1, duc, "Phân tích yêu cầu và thiết kế ER diagram", 8, LocalDate.of(2026, 1, 7));
        addTimeLog(t1, duc, "Viết migration script", 6, LocalDate.of(2026, 1, 8));
        addComment(t1, admin, "Schema trông ổn, tuy nhiên cần thêm index cho cột `product_id` trong bảng `order_items` để tối ưu query thống kê.");
        addComment(t1, duc,   "Đã thêm index. Cũng đã tạo composite index cho (user_id, created_at) trong bảng orders.");

        Task t2 = saveTask(project, board, 2, "Cài đặt môi trường development",
                "Cấu hình Docker Compose, CI/CD pipeline, môi trường dev/staging.",
                Task.TaskPriority.HIGH, Task.TaskStatus.DONE,
                nam, admin, LocalDate.of(2026, 1, 6), LocalDate.of(2026, 1, 8),
                BigDecimal.valueOf(8), BigDecimal.valueOf(10),
                null, catFeature, v100, colDone, new BigDecimal("2000"));
        addLabels(t2, lBe);
        addTimeLog(t2, nam, "Cấu hình Docker Compose và environment variables", 5, LocalDate.of(2026, 1, 7));
        addComment(t2, nam, "Đã setup xong môi trường. PostgreSQL chạy ở port 5432, Redis port 6379. Mọi người clone repo và chạy `docker-compose up -d` là được.");

        Task t3 = saveTask(project, board, 3, "Thiết kế giao diện trang chủ",
                "Thiết kế mockup và implement trang chủ: banner, danh mục nổi bật, sản phẩm gợi ý.",
                Task.TaskPriority.MEDIUM, Task.TaskStatus.DONE,
                lan, admin, LocalDate.of(2026, 1, 8), LocalDate.of(2026, 1, 14),
                BigDecimal.valueOf(20), BigDecimal.valueOf(22),
                null, catFeature, v100, colDone, new BigDecimal("3000"));
        addLabels(t3, lFe, lUx);
        addChecklist(t3, lan, new String[]{
            "Wireframe trang chủ",
            "Component Banner slideshow",
            "Component CategoryGrid",
            "Component ProductCard",
            "Responsive mobile"
        }, new boolean[]{true, true, true, true, true});
        addTimeLog(t3, lan, "Thiết kế wireframe và prototype Figma", 6, LocalDate.of(2026, 1, 9));
        addTimeLog(t3, lan, "Implement component Banner và CategoryGrid", 8, LocalDate.of(2026, 1, 13));

        Task t4 = saveTask(project, board, 4, "API xác thực người dùng",
                "Implement JWT authentication: đăng ký, đăng nhập, refresh token, đổi mật khẩu.",
                Task.TaskPriority.HIGH, Task.TaskStatus.DONE,
                duc, admin, LocalDate.of(2026, 1, 8), LocalDate.of(2026, 1, 15),
                BigDecimal.valueOf(16), BigDecimal.valueOf(14),
                null, catFeature, v100, colDone, new BigDecimal("4000"));
        addLabels(t4, lBe, lApi);
        addTimeLog(t4, duc, "Implement JWT filter và UserDetails", 5, LocalDate.of(2026, 1, 10));
        addTimeLog(t4, duc, "Viết unit test cho auth endpoints", 4, LocalDate.of(2026, 1, 14));
        addComment(t4, tuan, "Đã test tất cả auth endpoints, hoạt động tốt. Tuy nhiên cần xử lý case khi refresh token đã hết hạn trả về 401 thay vì 500.");
        addComment(t4, duc,  "Đã fix. Thêm `TokenExpiredException` vào GlobalExceptionHandler rồi.");

        Task t5 = saveTask(project, board, 5, "Trang đăng ký / đăng nhập",
                "Implement form đăng ký, đăng nhập với validation, xử lý lỗi và UX flows.",
                Task.TaskPriority.MEDIUM, Task.TaskStatus.DONE,
                lan, admin, LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 17),
                BigDecimal.valueOf(12), BigDecimal.valueOf(13),
                null, catFeature, v100, colDone, new BigDecimal("5000"));
        addLabels(t5, lFe, lUx);
        addTimeLog(t5, lan, "Implement form login/register với React Hook Form", 7, LocalDate.of(2026, 1, 15));

        sp1.getTasks().addAll(List.of(t1, t2, t3, t4, t5));
        sprintRepository.save(sp1);

        // ── 10. Tasks Sprint 2 (COMPLETED) ─────────────────────────────────
        Task t6 = saveTask(project, board, 6, "Module quản lý sản phẩm",
                "CRUD sản phẩm: tạo, sửa, xóa, phân trang, lọc theo category, tìm kiếm.",
                Task.TaskPriority.HIGH, Task.TaskStatus.DONE,
                duc, admin, LocalDate.of(2026, 1, 20), LocalDate.of(2026, 1, 27),
                BigDecimal.valueOf(24), BigDecimal.valueOf(26),
                null, catFeature, v100, colDone, new BigDecimal("6000"));
        addLabels(t6, lBe, lApi);
        addChecklist(t6, duc, new String[]{
            "API GET /products (phân trang, filter, sort)",
            "API POST /products",
            "API PUT /products/{id}",
            "API DELETE /products/{id}",
            "Phân quyền theo role"
        }, new boolean[]{true, true, true, true, true});
        addTimeLog(t6, duc, "Implement CRUD endpoints sản phẩm", 10, LocalDate.of(2026, 1, 22));
        addTimeLog(t6, duc, "Viết unit test và integration test", 6, LocalDate.of(2026, 1, 25));

        Task t7 = saveTask(project, board, 7, "Giao diện danh sách sản phẩm",
                "Trang danh sách sản phẩm: grid/list view, filter sidebar, sort, phân trang.",
                Task.TaskPriority.HIGH, Task.TaskStatus.DONE,
                lan, admin, LocalDate.of(2026, 1, 22), LocalDate.of(2026, 1, 29),
                BigDecimal.valueOf(20), BigDecimal.valueOf(21),
                null, catFeature, v100, colDone, new BigDecimal("7000"));
        addLabels(t7, lFe, lUx);
        addTimeLog(t7, lan, "Implement ProductGrid và FilterSidebar", 9, LocalDate.of(2026, 1, 27));
        addComment(t7, lan, "Đã implement xong grid view. List view sẽ làm trong sprint sau nếu còn thời gian.");
        addComment(t7, nam, "OK, grid view là đủ cho v1.0. List view đưa vào backlog nhé.");

        Task t8 = saveTask(project, board, 8, "Tính năng tìm kiếm sản phẩm",
                "Full-text search sản phẩm theo tên, mô tả, category với autocomplete.",
                Task.TaskPriority.MEDIUM, Task.TaskStatus.DONE,
                duc, admin, LocalDate.of(2026, 1, 23), LocalDate.of(2026, 1, 30),
                BigDecimal.valueOf(16), BigDecimal.valueOf(15),
                null, catFeature, v100, colDone, new BigDecimal("8000"));
        addLabels(t8, lBe, lPerf);
        addTimeLog(t8, duc, "Implement full-text search với PostgreSQL tsvector", 8, LocalDate.of(2026, 1, 28));

        Task t9 = saveTask(project, board, 9, "Fix bug: Lỗi validation form đăng ký",
                "Form đăng ký không hiển thị thông báo lỗi khi email đã tồn tại. Server trả về 400 nhưng FE không bắt được.",
                Task.TaskPriority.HIGH, Task.TaskStatus.DONE,
                lan, tuan, LocalDate.of(2026, 1, 25), LocalDate.of(2026, 1, 27),
                BigDecimal.valueOf(4), BigDecimal.valueOf(3),
                null, catBug, v100, colDone, new BigDecimal("9000"));
        addLabels(t9, lFe, lCrit);
        addComment(t9, tuan, "Bug được tìm thấy khi test flow đăng ký. Response body có `success: false` nhưng FE chỉ check HTTP status code 200.");
        addComment(t9, lan,  "Đã fix. Giờ interceptor axios kiểm tra cả `data.success` trước khi resolve promise.");
        addComment(t9, tuan, "Đã retest, confirmed fixed. Closing.");

        Task t10 = saveTask(project, board, 10, "Upload ảnh sản phẩm",
                "Tích hợp upload ảnh sản phẩm: multipart/form-data, resize tự động, lưu local storage.",
                Task.TaskPriority.MEDIUM, Task.TaskStatus.DONE,
                duc, admin, LocalDate.of(2026, 1, 27), LocalDate.of(2026, 2, 1),
                BigDecimal.valueOf(12), BigDecimal.valueOf(11),
                null, catFeature, v100, colDone, new BigDecimal("10000"));
        addLabels(t10, lBe);
        addTimeLog(t10, duc, "Implement file upload endpoint với validation", 6, LocalDate.of(2026, 1, 29));

        sp2.getTasks().addAll(List.of(t6, t7, t8, t9, t10));
        sprintRepository.save(sp2);

        // ── 11. Tasks Sprint 3 (COMPLETED) ─────────────────────────────────
        Task t11 = saveTask(project, board, 11, "Module giỏ hàng",
                "API giỏ hàng: thêm/xóa/cập nhật số lượng sản phẩm, tính tổng tiền, kiểm tra tồn kho.",
                Task.TaskPriority.HIGH, Task.TaskStatus.DONE,
                duc, admin, LocalDate.of(2026, 2, 3), LocalDate.of(2026, 2, 10),
                BigDecimal.valueOf(20), BigDecimal.valueOf(22),
                null, catFeature, v110, colDone, new BigDecimal("11000"));
        addLabels(t11, lBe, lApi);
        addChecklist(t11, duc, new String[]{
            "API POST /cart/items — thêm sản phẩm",
            "API PUT /cart/items/{id} — cập nhật số lượng",
            "API DELETE /cart/items/{id} — xóa sản phẩm",
            "Kiểm tra tồn kho khi thêm vào giỏ",
            "Tính tổng tiền bao gồm discount"
        }, new boolean[]{true, true, true, true, true});
        addTimeLog(t11, duc, "Implement cart service và repository", 8, LocalDate.of(2026, 2, 5));
        addTimeLog(t11, duc, "Xử lý race condition khi cập nhật tồn kho", 4, LocalDate.of(2026, 2, 7));

        Task t12 = saveTask(project, board, 12, "Giao diện giỏ hàng",
                "Trang giỏ hàng: danh sách sản phẩm, cập nhật số lượng real-time, tính toán giá.",
                Task.TaskPriority.HIGH, Task.TaskStatus.DONE,
                lan, admin, LocalDate.of(2026, 2, 5), LocalDate.of(2026, 2, 12),
                BigDecimal.valueOf(16), BigDecimal.valueOf(17),
                null, catFeature, v110, colDone, new BigDecimal("12000"));
        addLabels(t12, lFe, lUx);
        addTimeLog(t12, lan, "Implement CartPage và CartItem component", 10, LocalDate.of(2026, 2, 10));
        addComment(t12, lan, "Đã xong. Số lượng update optimistic UI, nếu API fail thì revert lại.");

        Task t13 = saveTask(project, board, 13, "Tích hợp cổng thanh toán VNPay",
                "Tích hợp VNPay: tạo URL thanh toán, xử lý callback, cập nhật trạng thái đơn hàng.",
                Task.TaskPriority.HIGH, Task.TaskStatus.DONE,
                duc, admin, LocalDate.of(2026, 2, 7), LocalDate.of(2026, 2, 14),
                BigDecimal.valueOf(24), BigDecimal.valueOf(28),
                null, catFeature, v110, colDone, new BigDecimal("13000"));
        addLabels(t13, lBe, lApi);
        addChecklist(t13, duc, new String[]{
            "Đọc tài liệu tích hợp VNPay sandbox",
            "Implement tạo URL thanh toán",
            "Implement xử lý IPN callback",
            "Test với thẻ test VNPay",
            "Handle timeout và lỗi network"
        }, new boolean[]{true, true, true, true, true});
        addTimeLog(t13, duc, "Implement VNPay integration và callback handler", 12, LocalDate.of(2026, 2, 11));
        addTimeLog(t13, duc, "Test và fix edge cases", 6, LocalDate.of(2026, 2, 13));
        addComment(t13, tuan, "Test payment flow xong. Tất cả 4 scenarios (success, fail, timeout, cancel) đều hoạt động đúng.");

        Task t14 = saveTask(project, board, 14, "Fix bug: Giỏ hàng không cập nhật realtime",
                "Khi nhiều tab cùng mở, thay đổi số lượng ở tab này không sync sang tab kia.",
                Task.TaskPriority.HIGH, Task.TaskStatus.DONE,
                lan, tuan, LocalDate.of(2026, 2, 8), LocalDate.of(2026, 2, 10),
                BigDecimal.valueOf(8), BigDecimal.valueOf(6),
                null, catBug, v110, colDone, new BigDecimal("14000"));
        addLabels(t14, lFe, lCrit);
        addComment(t14, tuan, "Reproduce được. Mở 2 tab, tab 1 thêm sản phẩm vào giỏ → tab 2 không cập nhật số lượng trên icon giỏ hàng.");
        addComment(t14, lan,  "Fix bằng cách dùng BroadcastChannel API để sync state giữa các tab.");

        Task t15 = saveTask(project, board, 15, "Tối ưu query danh sách sản phẩm",
                "Query lấy danh sách sản phẩm đang chạy 2-3s. Cần tối ưu xuống dưới 200ms.",
                Task.TaskPriority.HIGH, Task.TaskStatus.DONE,
                duc, admin, LocalDate.of(2026, 2, 10), LocalDate.of(2026, 2, 15),
                BigDecimal.valueOf(12), BigDecimal.valueOf(10),
                null, catImprove, v110, colDone, new BigDecimal("15000"));
        addLabels(t15, lBe, lPerf, lDb);
        addChecklist(t15, duc, new String[]{
            "Profile query hiện tại bằng EXPLAIN ANALYZE",
            "Thêm index còn thiếu",
            "Implement query caching với Redis",
            "Benchmark sau tối ưu"
        }, new boolean[]{true, true, true, true});
        addTimeLog(t15, duc, "Phân tích query và thêm index", 4, LocalDate.of(2026, 2, 11));
        addTimeLog(t15, duc, "Implement Redis cache layer", 5, LocalDate.of(2026, 2, 13));
        addComment(t15, duc, "Sau khi thêm composite index (category_id, status, price) và cache Redis 5 phút, query xuống còn ~80ms. Đạt mục tiêu.");

        sp3.getTasks().addAll(List.of(t11, t12, t13, t14, t15));
        sprintRepository.save(sp3);

        // ── 12. Tasks Sprint 4 (ACTIVE) ────────────────────────────────────
        Task t16 = saveTask(project, board, 16, "Module quản lý đơn hàng",
                "API đơn hàng: tạo đơn, xem chi tiết, cập nhật trạng thái, hủy đơn, lịch sử.",
                Task.TaskPriority.HIGH, Task.TaskStatus.IN_PROGRESS,
                duc, admin, LocalDate.of(2026, 3, 17), LocalDate.of(2026, 3, 24),
                BigDecimal.valueOf(24), null,
                null, catFeature, v200, colInProg, new BigDecimal("1000"));
        addLabels(t16, lBe, lApi);
        addChecklist(t16, duc, new String[]{
            "API POST /orders — tạo đơn hàng",
            "API GET /orders — danh sách đơn (phân trang, filter)",
            "API GET /orders/{id} — chi tiết đơn",
            "API PATCH /orders/{id}/status — cập nhật trạng thái",
            "API POST /orders/{id}/cancel — hủy đơn",
            "Gửi notification khi đơn thay đổi trạng thái"
        }, new boolean[]{true, true, false, false, false, false});
        addTimeLog(t16, duc, "Implement tạo đơn hàng và danh sách đơn", 8, LocalDate.of(2026, 3, 20));
        addComment(t16, duc, "Đã xong API tạo đơn và lấy danh sách. Đang implement cập nhật trạng thái.");
        addComment(t16, nam, "Nhớ implement state machine cho order status: PENDING → CONFIRMED → SHIPPING → DELIVERED. Không cho phép nhảy cóc.");

        // Subtask của t16
        Task t16a = saveTask(project, board, 20, "Order state machine",
                "Implement state machine kiểm soát luồng chuyển trạng thái đơn hàng.",
                Task.TaskPriority.HIGH, Task.TaskStatus.TODO,
                duc, admin, LocalDate.of(2026, 3, 22), LocalDate.of(2026, 3, 24),
                BigDecimal.valueOf(6), null,
                t16, catFeature, v200, null, BigDecimal.ZERO);
        addLabels(t16a, lBe);

        Task t17 = saveTask(project, board, 17, "Giao diện quản lý đơn hàng",
                "Trang quản lý đơn hàng cho admin: danh sách, lọc theo trạng thái, xem chi tiết, cập nhật.",
                Task.TaskPriority.HIGH, Task.TaskStatus.TODO,
                lan, admin, LocalDate.of(2026, 3, 21), LocalDate.of(2026, 3, 27),
                BigDecimal.valueOf(20), null,
                null, catFeature, v200, colTodo, new BigDecimal("2000"));
        addLabels(t17, lFe, lUx);
        addChecklist(t17, lan, new String[]{
            "OrderList page với filter và pagination",
            "OrderDetail modal/page",
            "Cập nhật trạng thái từ UI",
            "Export đơn hàng ra Excel"
        }, new boolean[]{false, false, false, false});

        Task t18 = saveTask(project, board, 18, "Tích hợp email thông báo đơn hàng",
                "Gửi email tự động: xác nhận đơn, cập nhật trạng thái, thông báo giao hàng.",
                Task.TaskPriority.MEDIUM, Task.TaskStatus.IN_REVIEW,
                duc, admin, LocalDate.of(2026, 3, 17), LocalDate.of(2026, 3, 22),
                BigDecimal.valueOf(12), BigDecimal.valueOf(11),
                null, catFeature, v200, colReview, new BigDecimal("1000"));
        addLabels(t18, lBe);
        addTimeLog(t18, duc, "Tạo email templates và implement email service", 8, LocalDate.of(2026, 3, 19));
        addComment(t18, duc,   "Đã implement xong. Dùng Thymeleaf template cho email HTML. Gửi async để không block request.");
        addComment(t18, tuan,  "Đang test các email template. Banner ảnh trong email bị vỡ layout trên Gmail mobile.");
        addComment(t18, duc,   "Đã fix CSS inline cho email. Gmail không support external CSS nên cần inline hết.");

        Task t19 = saveTask(project, board, 19, "Fix bug: Lỗi hiển thị giá khi có discount",
                "Khi sản phẩm có discount, giá hiển thị trong giỏ hàng khác với giá ở trang thanh toán.",
                Task.TaskPriority.HIGH, Task.TaskStatus.IN_PROGRESS,
                lan, tuan, LocalDate.of(2026, 3, 24), LocalDate.of(2026, 3, 26),
                BigDecimal.valueOf(4), null,
                null, catBug, v200, colInProg, new BigDecimal("3000"));
        addLabels(t19, lFe, lCrit);
        addComment(t19, tuan, "Reproduce: Thêm sản phẩm có 20% discount vào giỏ → giá ở CartPage hiển thị đúng → sang CheckoutPage giá lại thành giá gốc.");
        addComment(t19, lan,  "Root cause: CartPage tính discount ở frontend, CheckoutPage gọi lại API lấy price gốc. Đang fix để dùng discountedPrice từ API.");

        sp4.getTasks().addAll(List.of(t16, t17, t18, t19));
        sprintRepository.save(sp4);

        // ── 13. Tasks Backlog ───────────────────────────────────────────────
        Task t21 = saveTask(project, board, 21, "Hệ thống đánh giá sản phẩm",
                "Cho phép người dùng đánh giá sao và viết nhận xét sản phẩm đã mua. Hiển thị trung bình rating.",
                Task.TaskPriority.MEDIUM, Task.TaskStatus.TODO,
                duc, admin, null, null,
                BigDecimal.valueOf(20), null,
                null, catFeature, v200, null, BigDecimal.ZERO);
        addLabels(t21, lBe, lApi);

        Task t22 = saveTask(project, board, 22, "Tính năng yêu thích sản phẩm",
                "Cho phép lưu sản phẩm yêu thích, đồng bộ giữa các thiết bị.",
                Task.TaskPriority.LOW, Task.TaskStatus.TODO,
                lan, admin, null, null,
                BigDecimal.valueOf(8), null,
                null, catFeature, v200, null, BigDecimal.ZERO);
        addLabels(t22, lFe);

        Task t23 = saveTask(project, board, 23, "Cải thiện UX trang checkout",
                "Rút gọn các bước checkout từ 4 bước xuống 2 bước. Thêm guest checkout.",
                Task.TaskPriority.MEDIUM, Task.TaskStatus.TODO,
                lan, admin, null, null,
                BigDecimal.valueOf(16), null,
                null, catImprove, v200, null, BigDecimal.ZERO);
        addLabels(t23, lFe, lUx);

        Task t24 = saveTask(project, board, 24, "API xuất báo cáo Excel",
                "Xuất báo cáo doanh thu, đơn hàng, sản phẩm bán chạy ra file Excel.",
                Task.TaskPriority.LOW, Task.TaskStatus.TODO,
                duc, admin, null, null,
                BigDecimal.valueOf(12), null,
                null, catFeature, v200, null, BigDecimal.ZERO);
        addLabels(t24, lBe, lApi);

        Task t25 = saveTask(project, board, 25, "Module khuyến mãi và voucher",
                "Tạo và quản lý mã giảm giá: theo %, theo số tiền cố định, giới hạn lượt dùng.",
                Task.TaskPriority.MEDIUM, Task.TaskStatus.TODO,
                duc, admin, null, null,
                BigDecimal.valueOf(28), null,
                null, catFeature, v200, null, BigDecimal.ZERO);
        addLabels(t25, lBe, lApi);
        addComment(t25, nam, "Cần confirm với stakeholder về các loại voucher trước khi implement. @nguyen.ducnv bạn có thể họp với PM tuần tới không?");

        log.info("Demo data created successfully: project TMDT with 25 tasks.");
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

    private BoardColumn saveColumn(Board board, String name, int position, String color, boolean isCompleted) {
        return boardColumnRepository.save(BoardColumn.builder()
                .board(board).name(name).position(position).color(color).isCompleted(isCompleted)
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

    private Sprint saveSprint(Project project, Board board, String name, String goal,
                              Sprint.SprintStatus status, LocalDate start, LocalDate end) {
        Sprint sprint = Sprint.builder()
                .project(project).board(board).name(name).goal(goal).status(status)
                .startDate(start).endDate(end)
                .build();
        if (status == Sprint.SprintStatus.COMPLETED) {
            sprint.setCompletedAt(end.atTime(17, 0));
        }
        return sprintRepository.save(sprint);
    }

    private Task saveTask(Project project, Board board, int number, String title, String description,
                          Task.TaskPriority priority, Task.TaskStatus status,
                          User assignee, User reporter,
                          LocalDate startDate, LocalDate dueDate,
                          BigDecimal estimated, BigDecimal actual,
                          Task parentTask, IssueCategory category, Version version,
                          BoardColumn column, BigDecimal position) {
        Task task = Task.builder()
                .project(project).board(board).taskNumber(number)
                .title(title).description(description)
                .priority(priority).status(status)
                .assignee(assignee).reporter(reporter)
                .startDate(startDate).dueDate(dueDate)
                .estimatedHours(estimated).actualHours(actual)
                .parentTask(parentTask)
                .category(category).version(version)
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

    private void addChecklist(Task task, User checkedBy, String[] items, boolean[] checked) {
        for (int i = 0; i < items.length; i++) {
            ChecklistItem item = ChecklistItem.builder()
                    .task(task).content(items[i]).position(i)
                    .isChecked(checked[i])
                    .build();
            if (checked[i]) {
                item.setCheckedBy(checkedBy);
                item.setCheckedAt(LocalDateTime.now().minusDays(1));
            }
            checklistItemRepository.save(item);
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
