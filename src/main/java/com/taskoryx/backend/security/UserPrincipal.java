package com.taskoryx.backend.security;

import com.taskoryx.backend.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Builder
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private UUID id;
    private String username;
    private String email;
    private String password;
    private String fullName;
    private boolean active;
    private Collection<? extends GrantedAuthority> authorities;

    public static UserPrincipal create(User user) {
        // Lấy tất cả permissions từ các roles của user
        Set<GrantedAuthority> authorities = user.getUserRoles().stream()
                .flatMap(userRole -> userRole.getRole().getPermissions().stream())
                .map(permission -> (GrantedAuthority) new SimpleGrantedAuthority(permission.getName()))
                .collect(Collectors.toSet());

        return UserPrincipal.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .password(user.getPasswordHash())
                .fullName(user.getFullName())
                .active(Boolean.TRUE.equals(user.getIsActive()))
                .authorities(authorities)
                .build();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email; // Use email as username for Spring Security
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return active; }
}
