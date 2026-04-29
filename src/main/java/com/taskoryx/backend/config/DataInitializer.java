package com.taskoryx.backend.config;

import com.taskoryx.backend.entity.Permission;
import com.taskoryx.backend.entity.Role;
import com.taskoryx.backend.entity.User;
import com.taskoryx.backend.entity.UserRole;
import com.taskoryx.backend.repository.PermissionRepository;
import com.taskoryx.backend.repository.RoleRepository;
import com.taskoryx.backend.repository.UserRepository;
import com.taskoryx.backend.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Khởi tạo dữ liệu cố định khi ứng dụng start:
 *
 * PERMISSIONS (system permissions — không xóa được qua API):
 *   Nhóm ADMIN   : ADMIN_ACCESS, TEMPLATE_MANAGE
 *   Nhóm USER    : USER_VIEW, USER_CREATE, USER_UPDATE, USER_DELETE
 *   Nhóm PROJECT : PROJECT_VIEW, PROJECT_CREATE, PROJECT_UPDATE, PROJECT_DELETE, PROJECT_MANAGE_MEMBERS
 *   Nhóm BOARD   : BOARD_VIEW, BOARD_CREATE, BOARD_UPDATE, BOARD_DELETE
 *   Nhóm TASK    : TASK_VIEW, TASK_CREATE, TASK_UPDATE, TASK_DELETE, TASK_ASSIGN
 *   Nhóm COMMENT : COMMENT_CREATE, COMMENT_DELETE
 *   Nhóm SPRINT  : SPRINT_MANAGE
 *   Nhóm REPORT  : REPORT_VIEW
 *
 * SYSTEM ROLES (isSystemRole = true — không xóa/sửa qua API):
 *   SUPER_ADMIN      — toàn quyền hệ thống
 *   ADMIN            — quản lý người dùng (USER_*) + ADMIN_ACCESS, không có TEMPLATE_MANAGE
 *   PROJECT_MANAGER  — toàn quyền dự án + thành viên, không có quyền admin hệ thống
 *   TEAM_LEAD        — quản lý task/sprint/báo cáo trong dự án, không xóa dự án
 *   MEMBER           — xem và làm việc với task/comment/board
 *
 * USER MẶC ĐỊNH:
 *   admin@taskoryx.com / Admin@123456  →  gán SUPER_ADMIN
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class DataInitializer implements ApplicationRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    // ── Permission definitions ──────────────────────────────────────────────

    private static final List<PermissionDef> ALL_PERMISSIONS = List.of(

        // ADMIN group
        new PermissionDef("ADMIN_ACCESS",
                "Truy cập trang quản trị hệ thống",
                "ADMIN"),
        new PermissionDef("TEMPLATE_MANAGE",
                "Tạo, chỉnh sửa và xóa template dự án",
                "ADMIN"),

        // USER group
        new PermissionDef("USER_VIEW",
                "Xem danh sách và thông tin người dùng",
                "USER"),
        new PermissionDef("USER_CREATE",
                "Tạo tài khoản người dùng mới",
                "USER"),
        new PermissionDef("USER_UPDATE",
                "Cập nhật thông tin người dùng",
                "USER"),
        new PermissionDef("USER_DELETE",
                "Xóa tài khoản người dùng",
                "USER"),
        new PermissionDef("USER_RESET_PASSWORD",
                "Đặt lại mật khẩu người dùng",
                "USER"),
        new PermissionDef("USER_ASSIGN_ROLE",
                "Gán và thu hồi role cho người dùng",
                "USER"),

        // PROJECT group
        new PermissionDef("PROJECT_VIEW",
                "Xem danh sách và thông tin dự án",
                "PROJECT"),
        new PermissionDef("PROJECT_CREATE",
                "Tạo dự án mới",
                "PROJECT"),
        new PermissionDef("PROJECT_UPDATE",
                "Cập nhật thông tin dự án",
                "PROJECT"),
        new PermissionDef("PROJECT_DELETE",
                "Xóa dự án",
                "PROJECT"),
        new PermissionDef("PROJECT_ARCHIVE",
                "Lưu trữ / khôi phục dự án",
                "PROJECT"),
        new PermissionDef("PROJECT_MANAGE_MEMBERS",
                "Thêm, xóa và phân quyền thành viên dự án",
                "PROJECT"),

        // BOARD group
        new PermissionDef("BOARD_VIEW",
                "Xem bảng Kanban / Scrum",
                "BOARD"),
        new PermissionDef("BOARD_CREATE",
                "Tạo bảng mới trong dự án",
                "BOARD"),
        new PermissionDef("BOARD_UPDATE",
                "Cập nhật cột và cấu hình bảng",
                "BOARD"),
        new PermissionDef("BOARD_DELETE",
                "Xóa bảng",
                "BOARD"),

        // TASK group
        new PermissionDef("TASK_VIEW",
                "Xem danh sách và chi tiết công việc",
                "TASK"),
        new PermissionDef("TASK_CREATE",
                "Tạo công việc mới",
                "TASK"),
        new PermissionDef("TASK_UPDATE",
                "Cập nhật thông tin công việc",
                "TASK"),
        new PermissionDef("TASK_DELETE",
                "Xóa công việc",
                "TASK"),
        new PermissionDef("TASK_ASSIGN",
                "Giao công việc cho thành viên khác",
                "TASK"),

        // COMMENT group
        new PermissionDef("COMMENT_CREATE",
                "Tạo bình luận trên công việc",
                "COMMENT"),
        new PermissionDef("COMMENT_DELETE",
                "Xóa bình luận",
                "COMMENT"),

        // SPRINT group
        new PermissionDef("SPRINT_MANAGE",
                "Tạo, bắt đầu, kết thúc và xóa sprint",
                "SPRINT"),

        // REPORT group
        new PermissionDef("REPORT_VIEW",
                "Xem báo cáo và thống kê hiệu suất",
                "REPORT")
    );

    // ── Role definitions ────────────────────────────────────────────────────

    /**
     * SUPER_ADMIN — toàn quyền hệ thống, bao gồm quản lý template và admin.
     */
    private static final List<String> SUPER_ADMIN_PERMISSIONS = ALL_PERMISSIONS.stream()
            .map(PermissionDef::name)
            .collect(Collectors.toList());

    /**
     * ADMIN — quản lý người dùng và truy cập trang admin.
     * KHÔNG có TEMPLATE_MANAGE, PROJECT_DELETE, PROJECT_ARCHIVE.
     */
    private static final List<String> ADMIN_PERMISSIONS = List.of(
            "ADMIN_ACCESS",
            "USER_VIEW", "USER_CREATE", "USER_UPDATE", "USER_DELETE",
            "USER_RESET_PASSWORD", "USER_ASSIGN_ROLE",
            "PROJECT_VIEW",
            "REPORT_VIEW"
    );

    /**
     * PROJECT_MANAGER — toàn quyền dự án và thành viên.
     * Không có quyền admin hệ thống hay quản lý user.
     */
    private static final List<String> PROJECT_MANAGER_PERMISSIONS = List.of(
            "PROJECT_VIEW", "PROJECT_CREATE", "PROJECT_UPDATE", "PROJECT_DELETE", "PROJECT_ARCHIVE",
            "PROJECT_MANAGE_MEMBERS",
            "BOARD_VIEW", "BOARD_CREATE", "BOARD_UPDATE", "BOARD_DELETE",
            "TASK_VIEW", "TASK_CREATE", "TASK_UPDATE", "TASK_DELETE", "TASK_ASSIGN",
            "COMMENT_CREATE", "COMMENT_DELETE",
            "SPRINT_MANAGE",
            "REPORT_VIEW"
    );

    /**
     * TEAM_LEAD — quản lý task/sprint/báo cáo, không xóa dự án, không quản lý thành viên.
     */
    private static final List<String> TEAM_LEAD_PERMISSIONS = List.of(
            "PROJECT_VIEW", "PROJECT_UPDATE",
            "BOARD_VIEW", "BOARD_UPDATE",
            "TASK_VIEW", "TASK_CREATE", "TASK_UPDATE", "TASK_DELETE", "TASK_ASSIGN",
            "COMMENT_CREATE", "COMMENT_DELETE",
            "SPRINT_MANAGE",
            "REPORT_VIEW"
    );

    /**
     * MEMBER — xem và làm việc với task/comment trong dự án được phân công.
     */
    private static final List<String> MEMBER_PERMISSIONS = List.of(
            "PROJECT_VIEW",
            "BOARD_VIEW",
            "TASK_VIEW", "TASK_CREATE", "TASK_UPDATE",
            "COMMENT_CREATE"
    );

    // ── ApplicationRunner entry point ────────────────────────────────────────

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("=== DataInitializer: starting system data initialization ===");

        Map<String, Permission> permissions = initPermissions();
        initRoles(permissions);
        initDefaultAdminUser();

        log.info("=== DataInitializer: complete ===");
    }

    // ── Permission init ──────────────────────────────────────────────────────

    private Map<String, Permission> initPermissions() {
        Map<String, Permission> result = new LinkedHashMap<>();
        int created = 0;

        for (PermissionDef def : ALL_PERMISSIONS) {
            Permission permission = permissionRepository.findByName(def.name())
                    .orElseGet(() -> {
                        Permission p = Permission.builder()
                                .name(def.name())
                                .description(def.description())
                                .resource(def.resource())
                                .build();
                        return permissionRepository.save(p);
                    });

            // Đồng bộ description/resource nếu đã tồn tại nhưng khác
            boolean dirty = false;
            if (!def.description().equals(permission.getDescription())) {
                permission.setDescription(def.description());
                dirty = true;
            }
            if (!def.resource().equals(permission.getResource())) {
                permission.setResource(def.resource());
                dirty = true;
            }
            if (dirty) {
                permissionRepository.save(permission);
            }

            result.put(def.name(), permission);
            created++;
        }

        log.info("Permissions ready: {} total", created);
        return result;
    }

    // ── Role init ────────────────────────────────────────────────────────────

    private void initRoles(Map<String, Permission> permissions) {
        upsertSystemRole("SUPER_ADMIN",
                "Quản trị viên cấp cao — toàn quyền hệ thống bao gồm quản lý template",
                SUPER_ADMIN_PERMISSIONS, permissions);

        upsertSystemRole("ADMIN",
                "Quản trị viên — quản lý thông tin người dùng (thêm, sửa, xóa tài khoản)",
                ADMIN_PERMISSIONS, permissions);

        upsertSystemRole("PROJECT_MANAGER",
                "Quản lý dự án — toàn quyền dự án, thêm/xóa thành viên, quản lý sprint",
                PROJECT_MANAGER_PERMISSIONS, permissions);

        upsertSystemRole("TEAM_LEAD",
                "Trưởng nhóm — quản lý công việc và sprint, xem báo cáo trong dự án",
                TEAM_LEAD_PERMISSIONS, permissions);

        upsertSystemRole("MEMBER",
                "Thành viên — xem và thực hiện công việc được giao trong dự án",
                MEMBER_PERMISSIONS, permissions);
    }

    /**
     * Tạo role nếu chưa tồn tại; nếu đã có thì chỉ cập nhật description và permissions.
     * Luôn đặt isSystemRole = true (bảo vệ khỏi xóa qua API).
     */
    private void upsertSystemRole(String name, String description,
                                   List<String> permissionNames,
                                   Map<String, Permission> allPermissions) {
        Set<Permission> perms = permissionNames.stream()
                .filter(allPermissions::containsKey)
                .map(allPermissions::get)
                .collect(Collectors.toSet());

        Role role = roleRepository.findByName(name).orElseGet(() ->
                Role.builder().name(name).build());

        role.setDescription(description);
        role.setIsSystemRole(true);
        role.setPermissions(perms);
        roleRepository.save(role);

        log.info("Role [{}] ready — {} permissions", name, perms.size());
    }

    // ── Default admin user ───────────────────────────────────────────────────

    private void initDefaultAdminUser() {
        Role superAdminRole = roleRepository.findByName("SUPER_ADMIN")
                .orElseThrow(() -> new IllegalStateException("SUPER_ADMIN role not found after init"));

        User adminUser = userRepository.findByEmail("admin@taskoryx.com")
                .orElseGet(() -> {
                    User u = User.builder()
                            .username("admin")
                            .email("admin@taskoryx.com")
                            .passwordHash(passwordEncoder.encode("Admin@123456"))
                            .fullName("System Administrator")
                            .isActive(true)
                            .emailVerified(true)
                            .mustChangePassword(false)
                            .build();
                    userRepository.save(u);
                    log.warn("╔══════════════════════════════════════════╗");
                    log.warn("║        DEFAULT ADMIN ACCOUNT CREATED     ║");
                    log.warn("║  Email   : admin@taskoryx.com            ║");
                    log.warn("║  Password: Admin@123456                  ║");
                    log.warn("║  !! CHANGE PASSWORD AFTER FIRST LOGIN !! ║");
                    log.warn("╚══════════════════════════════════════════╝");
                    return u;
                });

        // Gán SUPER_ADMIN nếu chưa có
        if (!userRoleRepository.existsByUserIdAndRoleId(adminUser.getId(), superAdminRole.getId())) {
            userRoleRepository.save(UserRole.builder()
                    .user(adminUser)
                    .role(superAdminRole)
                    .build());
            log.info("SUPER_ADMIN role assigned to admin@taskoryx.com");
        }
    }

    // ── Internal record ──────────────────────────────────────────────────────

    private record PermissionDef(String name, String description, String resource) {}
}
