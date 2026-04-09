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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Khởi tạo dữ liệu ban đầu khi ứng dụng start:
 * - Tạo tất cả permissions
 * - Tạo ADMIN role (system role) với tất cả permissions
 * - Tạo user admin mặc định nếu chưa có
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    // ===================== ĐỊNH NGHĨA PERMISSIONS =====================
    private static final List<PermissionDef> ALL_PERMISSIONS = List.of(
        // ADMIN
        new PermissionDef("ADMIN_ACCESS",       "Truy cập trang quản trị",              "ADMIN"),

        // USER
        new PermissionDef("USER_VIEW",          "Xem danh sách người dùng",             "USER"),
        new PermissionDef("USER_CREATE",        "Tạo tài khoản người dùng",             "USER"),
        new PermissionDef("USER_UPDATE",        "Cập nhật thông tin người dùng",        "USER"),
        new PermissionDef("USER_DELETE",        "Xóa tài khoản người dùng",             "USER"),

        // PROJECT
        new PermissionDef("PROJECT_VIEW",       "Xem dự án",                            "PROJECT"),
        new PermissionDef("PROJECT_CREATE",     "Tạo dự án mới",                        "PROJECT"),
        new PermissionDef("PROJECT_UPDATE",     "Cập nhật thông tin dự án",             "PROJECT"),
        new PermissionDef("PROJECT_DELETE",     "Xóa dự án",                            "PROJECT"),
        new PermissionDef("PROJECT_MANAGE_MEMBERS", "Quản lý thành viên dự án",         "PROJECT"),

        // BOARD
        new PermissionDef("BOARD_VIEW",         "Xem bảng kanban",                      "BOARD"),
        new PermissionDef("BOARD_CREATE",       "Tạo bảng mới",                         "BOARD"),
        new PermissionDef("BOARD_UPDATE",       "Cập nhật bảng",                        "BOARD"),
        new PermissionDef("BOARD_DELETE",       "Xóa bảng",                             "BOARD"),

        // TASK
        new PermissionDef("TASK_VIEW",          "Xem công việc",                        "TASK"),
        new PermissionDef("TASK_CREATE",        "Tạo công việc mới",                    "TASK"),
        new PermissionDef("TASK_UPDATE",        "Cập nhật công việc",                   "TASK"),
        new PermissionDef("TASK_DELETE",        "Xóa công việc",                        "TASK"),
        new PermissionDef("TASK_ASSIGN",        "Giao công việc cho người khác",        "TASK"),

        // COMMENT
        new PermissionDef("COMMENT_CREATE",     "Tạo bình luận",                        "COMMENT"),
        new PermissionDef("COMMENT_DELETE",     "Xóa bình luận",                        "COMMENT"),

        // REPORT
        new PermissionDef("REPORT_VIEW",        "Xem báo cáo thống kê",                 "REPORT")
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Initializing system data...");
        Map<String, Permission> permissions = initPermissions();
        Role adminRole = initAdminRole(permissions);
        initPmRole(permissions);
        initDefaultAdminUser(adminRole);
        log.info("System data initialization complete.");
    }

    private Map<String, Permission> initPermissions() {
        Map<String, Permission> result = new LinkedHashMap<>();
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
            result.put(def.name(), permission);
        }
        log.info("Permissions initialized: {} total", result.size());
        return result;
    }

    private Role initAdminRole(Map<String, Permission> permissions) {
        return roleRepository.findByName("ADMIN").orElseGet(() -> {
            Role adminRole = Role.builder()
                    .name("ADMIN")
                    .description("Quản trị viên hệ thống - có toàn quyền")
                    .isSystemRole(true)
                    .permissions(new HashSet<>(permissions.values()))
                    .build();
            Role saved = roleRepository.save(adminRole);
            log.info("ADMIN role created with {} permissions", saved.getPermissions().size());
            return saved;
        });
    }

    private void initPmRole(Map<String, Permission> permissions) {
        roleRepository.findByName("PM").orElseGet(() -> {
            Set<Permission> pmPermissions = new HashSet<>();
            List<String> pmPermissionNames = List.of(
                "PROJECT_VIEW", "PROJECT_UPDATE", "PROJECT_MANAGE_MEMBERS",
                "BOARD_VIEW", "BOARD_CREATE", "BOARD_UPDATE", "BOARD_DELETE",
                "TASK_VIEW", "TASK_CREATE", "TASK_UPDATE", "TASK_DELETE", "TASK_ASSIGN",
                "COMMENT_CREATE", "COMMENT_DELETE",
                "REPORT_VIEW"
            );
            for (String name : pmPermissionNames) {
                if (permissions.containsKey(name)) {
                    pmPermissions.add(permissions.get(name));
                }
            }
            Role pmRole = Role.builder()
                    .name("PM")
                    .description("Project Manager - Quản lý dự án")
                    .isSystemRole(true)
                    .permissions(pmPermissions)
                    .build();
            Role saved = roleRepository.save(pmRole);
            log.info("PM role created with {} permissions", saved.getPermissions().size());
            return saved;
        });
    }

    private void initDefaultAdminUser(Role adminRole) {
        User adminUser = userRepository.findByEmail("admin@taskoryx.com")
                .orElseGet(() -> {
                    User u = User.builder()
                            .username("admin")
                            .email("admin@taskoryx.com")
                            .passwordHash(passwordEncoder.encode("Admin@123456"))
                            .fullName("System Administrator")
                            .isActive(true)
                            .emailVerified(true)
                            .build();
                    userRepository.save(u);
                    log.warn("========================================");
                    log.warn("Default admin created!");
                    log.warn("Email   : admin@taskoryx.com");
                    log.warn("Password: Admin@123456");
                    log.warn("PLEASE CHANGE PASSWORD AFTER FIRST LOGIN");
                    log.warn("========================================");
                    return u;
                });

        // Gán ADMIN role nếu chưa có
        boolean hasAdminRole = userRoleRepository.findAll().stream()
                .anyMatch(ur -> ur.getUser().getId().equals(adminUser.getId())
                        && ur.getRole().getName().equals("ADMIN"));
        if (!hasAdminRole) {
            userRoleRepository.save(UserRole.builder()
                    .user(adminUser)
                    .role(adminRole)
                    .build());
        }
    }

    private record PermissionDef(String name, String description, String resource) {}
}
