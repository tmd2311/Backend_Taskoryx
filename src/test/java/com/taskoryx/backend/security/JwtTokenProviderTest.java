package com.taskoryx.backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtTokenProvider - Token Generation & Validation Tests")
class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    // Secret đủ dài (Base64, >= 256 bit cho HS256)
    private static final String TEST_SECRET =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION = 86400000L;       // 24h
    private static final long REFRESH_EXPIRATION = 604800000L; // 7d

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(tokenProvider, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(tokenProvider, "jwtExpiration", EXPIRATION);
        ReflectionTestUtils.setField(tokenProvider, "refreshExpiration", REFRESH_EXPIRATION);
    }

    // ─── generateTokenFromUserId ──────────────────────────────────────────────

    @Test
    @DisplayName("generateTokenFromUserId → trả về token không null/rỗng")
    void generateToken_returnsNonBlankToken() {
        UUID userId = UUID.randomUUID();
        String token = tokenProvider.generateTokenFromUserId(userId);
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("generateTokenFromUserId → token có 3 phần JWT (header.payload.sig)")
    void generateToken_hasThreeParts() {
        String token = tokenProvider.generateTokenFromUserId(UUID.randomUUID());
        assertThat(token.split("\\.")).hasSize(3);
    }

    // ─── validateToken ────────────────────────────────────────────────────────

    @Test
    @DisplayName("validateToken với token hợp lệ → true")
    void validateToken_validToken_returnsTrue() {
        String token = tokenProvider.generateTokenFromUserId(UUID.randomUUID());
        assertThat(tokenProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("validateToken với token rác → false")
    void validateToken_malformedToken_returnsFalse() {
        assertThat(tokenProvider.validateToken("not.a.jwt")).isFalse();
    }

    @Test
    @DisplayName("validateToken với chuỗi rỗng → false")
    void validateToken_emptyString_returnsFalse() {
        assertThat(tokenProvider.validateToken("")).isFalse();
    }

    @Test
    @DisplayName("validateToken với token đã hết hạn → false")
    void validateToken_expiredToken_returnsFalse() {
        JwtTokenProvider expiredProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(expiredProvider, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(expiredProvider, "jwtExpiration", -1000L); // hết hạn ngay
        ReflectionTestUtils.setField(expiredProvider, "refreshExpiration", -1000L);

        String expiredToken = expiredProvider.generateTokenFromUserId(UUID.randomUUID());
        assertThat(tokenProvider.validateToken(expiredToken)).isFalse();
    }

    @Test
    @DisplayName("validateToken với token ký bằng secret khác → false")
    void validateToken_differentSecret_returnsFalse() {
        JwtTokenProvider otherProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(otherProvider, "jwtSecret",
                "5A7134743777217A25432A462D4A614E645267556B58703273357638792F423F");
        ReflectionTestUtils.setField(otherProvider, "jwtExpiration", EXPIRATION);
        ReflectionTestUtils.setField(otherProvider, "refreshExpiration", REFRESH_EXPIRATION);

        String token = otherProvider.generateTokenFromUserId(UUID.randomUUID());
        assertThat(tokenProvider.validateToken(token)).isFalse();
    }

    // ─── getUserIdFromToken ───────────────────────────────────────────────────

    @Test
    @DisplayName("getUserIdFromToken → trả về đúng userId đã ký")
    void getUserIdFromToken_validToken_returnsCorrectId() {
        UUID expectedId = UUID.randomUUID();
        String token = tokenProvider.generateTokenFromUserId(expectedId);
        UUID actualId = tokenProvider.getUserIdFromToken(token);
        assertThat(actualId).isEqualTo(expectedId);
    }

    // ─── generateRefreshToken ─────────────────────────────────────────────────

    @Test
    @DisplayName("generateRefreshToken → token hợp lệ")
    void generateRefreshToken_isValid() {
        String refreshToken = tokenProvider.generateRefreshToken(UUID.randomUUID());
        assertThat(tokenProvider.validateToken(refreshToken)).isTrue();
    }

    @Test
    @DisplayName("generateRefreshToken → có thể extract userId")
    void generateRefreshToken_extractsUserId() {
        UUID userId = UUID.randomUUID();
        String refreshToken = tokenProvider.generateRefreshToken(userId);
        assertThat(tokenProvider.getUserIdFromToken(refreshToken)).isEqualTo(userId);
    }

    // ─── getExpirationMs ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getExpirationMs → trả về giá trị đã config")
    void getExpirationMs_returnsConfiguredValue() {
        assertThat(tokenProvider.getExpirationMs()).isEqualTo(EXPIRATION);
    }

    // ─── Token uniqueness ────────────────────────────────────────────────────

    @Test
    @DisplayName("Hai userId khác nhau → hai token khác nhau")
    void differentUsers_generateDifferentTokens() {
        String t1 = tokenProvider.generateTokenFromUserId(UUID.randomUUID());
        String t2 = tokenProvider.generateTokenFromUserId(UUID.randomUUID());
        assertThat(t1).isNotEqualTo(t2);
    }
}
