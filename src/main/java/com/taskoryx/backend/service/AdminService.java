package com.taskoryx.backend.service;

import com.taskoryx.backend.dto.request.admin.*;
import com.taskoryx.backend.dto.response.admin.AdminUserResponse;
import com.taskoryx.backend.dto.response.admin.PermissionResponse;
import com.taskoryx.backend.dto.response.admin.RoleResponse;
import com.taskoryx.backend.entity.Permission;
import com.taskoryx.backend.entity.Role;
import com.taskoryx.backend.entity.User;
import com.taskoryx.backend.entity.UserRole;
import com.taskoryx.backend.exception.BadRequestException;
import com.taskoryx.backend.exception.ForbiddenException;
import com.taskoryx.backend.exception.ResourceNotFoundException;
import com.taskoryx.backend.repository.*;
import com.taskoryx.backend.security.UserPrincipal;
import com.taskoryx.backend.util.RoleNameGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // ===================== PERMISSION MANAGEMENT =====================

    @Transactional(readOnly = true)
    public List<PermissionResponse> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(PermissionResponse::from)
                .collect(Collectors.toList());
    }

    // ===================== ROLE MANAGEMENT =====================

    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(RoleResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RoleResponse getRoleById(UUID roleId) {
        Role role = findRoleById(roleId);
        return RoleResponse.from(role);
    }

    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {
        String generatedName = RoleNameGenerator.generate(request.getDisplayName());
        if (roleRepository.existsByName(generatedName)) {
            throw new BadRequestException("Vai trò '" + request.getDisplayName() + "' đã tồn tại");
        }

        Role role = Role.builder()
                .name(generatedName)
                .displayName(request.getDisplayName())
                .description(request.getDescription())
                .isSystemRole(false)
                .build();

        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            Set<Permission> permissions = request.getPermissionIds().stream()
                    .map(this::findPermissionById)
                    .collect(Collectors.toSet());
            role.setPermissions(permissions);
        }

        roleRepository.save(role);
        log.info("Role created: {}", role.getName());
        return RoleResponse.from(role);
    }

    @Transactional
    public RoleResponse updateRole(UUID roleId, UpdateRoleRequest request) {
        Role role = findRoleById(roleId);

        if (role.getIsSystemRole()) {
            throw new ForbiddenException("Không thể sửa system role");
        }

        if (request.getDisplayName() != null && !request.getDisplayName().equals(role.getDisplayName())) {
            String generatedName = RoleNameGenerator.generate(request.getDisplayName());
            if (roleRepository.existsByName(generatedName) && !generatedName.equals(role.getName())) {
                throw new BadRequestException("Vai trò '" + request.getDisplayName() + "' đã tồn tại");
            }
            role.setDisplayName(request.getDisplayName());
            role.setName(generatedName);
        }

        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }

        roleRepository.save(role);
        return RoleResponse.from(role);
    }

    @Transactional
    public RoleResponse addPermissionsToRole(UUID roleId, AssignPermissionsRequest request) {
        Role role = findRoleById(roleId);

        Set<Permission> newPermissions = request.getPermissionIds().stream()
                .map(this::findPermissionById)
                .collect(Collectors.toSet());

        role.getPermissions().addAll(newPermissions);
        roleRepository.save(role);
        log.info("Added {} permissions to role {}", newPermissions.size(), role.getName());
        return RoleResponse.from(role);
    }

    @Transactional
    public RoleResponse removePermissionsFromRole(UUID roleId, AssignPermissionsRequest request) {
        Role role = findRoleById(roleId);

        role.getPermissions().removeIf(p -> request.getPermissionIds().contains(p.getId()));
        roleRepository.save(role);
        return RoleResponse.from(role);
    }

    @Transactional
    public void deleteRole(UUID roleId) {
        Role role = findRoleById(roleId);

        if (role.getIsSystemRole()) {
            throw new ForbiddenException("Không thể xóa system role");
        }

        roleRepository.delete(role);
        log.info("Role deleted: {}", role.getName());
    }

    // ===================== USER MANAGEMENT =====================

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> getAllUsers(String keyword, Pageable pageable) {
        return userRepository.findAllUsers(keyword == null ? "" : keyword, pageable)
                .map(AdminUserResponse::from);
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getUserById(UUID userId) {
        User user = findUserById(userId);
        return AdminUserResponse.from(user);
    }

    @Transactional
    public AdminUserResponse createUser(CreateUserRequest request, UserPrincipal adminPrincipal) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email '" + request.getEmail() + "' đã được sử dụng");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username '" + request.getUsername() + "' đã được sử dụng");
        }

        String tempPassword = generateTemporaryPassword();

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(tempPassword))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .isActive(true)
                .emailVerified(true)
                .mustChangePassword(true)
                .build();

        userRepository.save(user);
        log.info("Admin {} created user: {}", adminPrincipal.getEmail(), user.getEmail());

        // Gửi email chào mừng kèm mật khẩu tạm (async - không block)
        emailService.sendWelcomeEmail(user.getEmail(), user.getFullName(), tempPassword);

        return AdminUserResponse.from(user);
    }

    @Transactional
    public AdminUserResponse toggleUserStatus(UUID userId, UserPrincipal adminPrincipal) {
        User user = findUserById(userId);

        if (user.getId().equals(adminPrincipal.getId())) {
            throw new BadRequestException("Không thể thay đổi trạng thái tài khoản của chính mình");
        }

        user.setIsActive(!user.getIsActive());
        userRepository.save(user);
        log.info("Admin {} set user {} active={}", adminPrincipal.getEmail(), user.getEmail(), user.getIsActive());
        return AdminUserResponse.from(user);
    }

    @Transactional
    public AdminUserResponse softDeleteUser(UUID userId, UserPrincipal adminPrincipal) {
        User user = findUserById(userId);

        if (user.getId().equals(adminPrincipal.getId())) {
            throw new BadRequestException("Không thể tự vô hiệu hóa tài khoản của mình");
        }
        if (user.getDeletedAt() != null) {
            throw new BadRequestException("Tài khoản này đã bị xóa trước đó");
        }

        user.setIsActive(false);
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
        log.info("Admin {} soft-deleted user: {}", adminPrincipal.getEmail(), user.getEmail());
        return AdminUserResponse.from(user);
    }

    @Transactional
    public AdminUserResponse activateUser(UUID userId, UserPrincipal adminPrincipal) {
        User user = findUserById(userId);

        if (user.getId().equals(adminPrincipal.getId())) {
            throw new BadRequestException("Không thể tự kích hoạt tài khoản của mình");
        }
        if (user.getIsActive()) {
            throw new BadRequestException("Tài khoản này đã đang hoạt động");
        }

        user.setIsActive(true);
        user.setDeletedAt(null);
        userRepository.save(user);
        log.info("Admin {} activated user: {}", adminPrincipal.getEmail(), user.getEmail());
        return AdminUserResponse.from(user);
    }

    @Transactional
    public void resetUserPassword(UUID userId) {
        User user = findUserById(userId);
        String tempPassword = generateTemporaryPassword();
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setMustChangePassword(true);
        userRepository.save(user);
        emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), tempPassword);
        log.info("Password reset for user: {}", user.getEmail());
    }

    @Transactional
    public AdminUserResponse assignRoleToUser(UUID userId, AssignRoleRequest request, UserPrincipal adminPrincipal) {
        User user = findUserById(userId);
        Role role = findRoleById(request.getRoleId());

        if (userRoleRepository.existsByUserIdAndRoleId(userId, role.getId())) {
            throw new BadRequestException("User đã có role '" + role.getName() + "' rồi");
        }

        User admin = findUserById(adminPrincipal.getId());
        UserRole userRole = UserRole.builder()
                .user(user)
                .role(role)
                .assignedBy(admin)
                .build();

        userRoleRepository.save(userRole);
        log.info("Admin {} assigned role {} to user {}", adminPrincipal.getEmail(), role.getName(), user.getEmail());

        User updatedUser = findUserById(userId);
        return AdminUserResponse.from(updatedUser);
    }

    @Transactional
    public AdminUserResponse removeRoleFromUser(UUID userId, UUID roleId) {
        UserRole userRole = userRoleRepository.findByUserIdAndRoleId(userId, roleId)
                .orElseThrow(() -> new BadRequestException("User không có role này"));

        userRoleRepository.delete(userRole);
        userRoleRepository.flush();

        User updatedUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        updatedUser.getUserRoles().removeIf(ur -> ur.getId().equals(userRole.getId()));
        return AdminUserResponse.from(updatedUser);
    }

    // ===================== HELPER METHODS =====================

    private String generateTemporaryPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    private Role findRoleById(UUID roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));
    }

    private Permission findPermissionById(UUID permissionId) {
        return permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", "id", permissionId));
    }
}
