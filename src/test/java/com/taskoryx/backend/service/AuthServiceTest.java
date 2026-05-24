package com.taskoryx.backend.service;

import com.taskoryx.backend.dto.request.auth.LoginRequest;
import com.taskoryx.backend.dto.request.auth.RefreshTokenRequest;
import com.taskoryx.backend.dto.response.auth.AuthResponse;
import com.taskoryx.backend.entity.User;
import com.taskoryx.backend.exception.BadRequestException;
import com.taskoryx.backend.repository.UserRepository;
import com.taskoryx.backend.security.JwtTokenProvider;
import com.taskoryx.backend.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - Login & Token Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthService authService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashed_password")
                .fullName("Test User")
                .isActive(true)
                .build();
    }

    // ─── login() ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login() với credentials hợp lệ → trả về AuthResponse đầy đủ")
    void login_validCredentials_returnsAuthResponse() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        UserPrincipal principal = UserPrincipal.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .password("hashed")
                .fullName("Test User")
                .active(true)
                .authorities(Collections.emptyList())
                .build();

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tokenProvider.generateToken(auth)).thenReturn("access_token_123");
        when(tokenProvider.generateRefreshToken(userId)).thenReturn("refresh_token_456");
        when(tokenProvider.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access_token_123");
        assertThat(response.getRefreshToken()).isEqualTo("refresh_token_456");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("login() cập nhật lastLoginAt của user")
    void login_updatesLastLoginAt() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        UserPrincipal principal = UserPrincipal.builder()
                .id(userId).username("testuser").email("test@example.com")
                .password("hashed").fullName("Test User").active(true)
                .authorities(Collections.emptyList()).build();

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tokenProvider.generateToken(auth)).thenReturn("token");
        when(tokenProvider.generateRefreshToken(userId)).thenReturn("refresh");
        when(tokenProvider.getExpirationMs()).thenReturn(86400000L);

        authService.login(request);

        verify(userRepository).save(argThat(u -> u.getLastLoginAt() != null));
    }

    @Test
    @DisplayName("login() với credentials sai → ném BadCredentialsException")
    void login_invalidCredentials_throwsBadCredentialsException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrong_password");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("login() gọi authenticationManager với đúng email và password")
    void login_callsAuthManagerWithCorrectCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        UserPrincipal principal = UserPrincipal.builder()
                .id(userId).username("testuser").email("test@example.com")
                .password("hashed").fullName("Test User").active(true)
                .authorities(Collections.emptyList()).build();

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tokenProvider.generateToken(auth)).thenReturn("token");
        when(tokenProvider.generateRefreshToken(userId)).thenReturn("refresh");
        when(tokenProvider.getExpirationMs()).thenReturn(86400000L);

        authService.login(request);

        verify(authenticationManager).authenticate(
                argThat(token -> token instanceof UsernamePasswordAuthenticationToken
                        && "test@example.com".equals(token.getPrincipal())
                        && "password123".equals(token.getCredentials()))
        );
    }

    // ─── refreshToken() ───────────────────────────────────────────────────────

    @Test
    @DisplayName("refreshToken() với token hợp lệ → trả về tokens mới")
    void refreshToken_validToken_returnsNewTokens() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid_refresh_token");

        when(tokenProvider.validateToken("valid_refresh_token")).thenReturn(true);
        when(tokenProvider.getUserIdFromToken("valid_refresh_token")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tokenProvider.generateTokenFromUserId(userId)).thenReturn("new_access_token");
        when(tokenProvider.generateRefreshToken(userId)).thenReturn("new_refresh_token");
        when(tokenProvider.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.refreshToken(request);

        assertThat(response.getAccessToken()).isEqualTo("new_access_token");
        assertThat(response.getRefreshToken()).isEqualTo("new_refresh_token");
    }

    @Test
    @DisplayName("refreshToken() với token không hợp lệ → ném BadRequestException")
    void refreshToken_invalidToken_throwsBadRequestException() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("invalid_token");

        when(tokenProvider.validateToken("invalid_token")).thenReturn(false);

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("không hợp lệ");
    }

    @Test
    @DisplayName("refreshToken() với user không tồn tại → ném BadRequestException")
    void refreshToken_userNotFound_throwsBadRequestException() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid_token");

        when(tokenProvider.validateToken("valid_token")).thenReturn(true);
        when(tokenProvider.getUserIdFromToken("valid_token")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("không tồn tại");
    }

    @Test
    @DisplayName("refreshToken() response có mustChangePassword đúng")
    void refreshToken_mustChangePassword_isPreservedInResponse() {
        user.setMustChangePassword(true);

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid_refresh_token");

        when(tokenProvider.validateToken("valid_refresh_token")).thenReturn(true);
        when(tokenProvider.getUserIdFromToken("valid_refresh_token")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tokenProvider.generateTokenFromUserId(userId)).thenReturn("new_access");
        when(tokenProvider.generateRefreshToken(userId)).thenReturn("new_refresh");
        when(tokenProvider.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.refreshToken(request);

        assertThat(response.isMustChangePassword()).isTrue();
    }
}
