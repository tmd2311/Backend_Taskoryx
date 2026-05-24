package com.taskoryx.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Authentication endpoints.
 *
 * Public endpoints under test:
 *   POST /auth/login
 *   POST /auth/refresh
 *
 * Protected endpoint used to verify token validity:
 *   GET /users/me
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuthIntegrationTest extends BaseIntegrationTest {

    // ── 1. login — valid credentials ─────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("login_validCredentials_returns200WithTokens")
    void login_validCredentials_returns200WithTokens() throws Exception {
        // Arrange
        Map<String, String> body = Map.of(
                "email", "admin@taskoryx.com",
                "password", "Admin@123456"
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        // Act
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/auth/login"), request, String.class);

        // Assert — status
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Assert — body structure
        JsonNode root = objectMapper.readTree(response.getBody());
        assertThat(root.path("success").asBoolean()).isTrue();

        JsonNode data = root.path("data");
        assertThat(data.path("accessToken").asText()).isNotBlank();
        assertThat(data.path("refreshToken").asText()).isNotBlank();
        assertThat(data.path("tokenType").asText()).isEqualTo("Bearer");
    }

    // ── 2. login — wrong password ────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("login_wrongPassword_returns401")
    void login_wrongPassword_returns401() {
        // Arrange
        Map<String, String> body = Map.of(
                "email", "admin@taskoryx.com",
                "password", "WrongPassword!"
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        // Act
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/auth/login"), request, String.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── 3. login — missing email field ───────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("login_missingEmail_returns400")
    void login_missingEmail_returns400() {
        // Arrange — only password in body, email is missing
        Map<String, String> body = Map.of("password", "x");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        // Act
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/auth/login"), request, String.class);

        // Assert — @NotBlank on email should trigger 400
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── 4. login — invalid email format ──────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("login_invalidEmailFormat_returns400")
    void login_invalidEmailFormat_returns400() {
        // Arrange — email without '@' fails @Email validation
        Map<String, String> body = Map.of(
                "email", "notanemail",
                "password", "Admin@123456"
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        // Act
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/auth/login"), request, String.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── 5. refresh — valid refresh token ─────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("refresh_validToken_returns200")
    void refresh_validToken_returns200() throws Exception {
        // Arrange — obtain a refresh token via login
        String refreshToken = loginAsAdminGetRefreshToken();
        assertThat(refreshToken).isNotBlank();

        Map<String, String> body = Map.of("refreshToken", refreshToken);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        // Act
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/auth/refresh"), request, String.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode data = objectMapper.readTree(response.getBody()).path("data");
        assertThat(data.path("accessToken").asText()).isNotBlank();
    }

    // ── 6. refresh — invalid/garbage token ───────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("refresh_invalidToken_returns400")
    void refresh_invalidToken_returns400() {
        // Arrange — deliberately pass a garbage token
        Map<String, String> body = Map.of("refreshToken", "this.is.not.a.valid.jwt");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        // Act
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/auth/refresh"), request, String.class);

        // Assert — service should reject the bad token with 400
        assertThat(response.getStatusCode().value()).isIn(400, 401);
    }

    // ── 7. protected endpoint — no token ─────────────────────────────────────

    @Test
    @Order(7)
    @DisplayName("accessProtectedEndpoint_withoutToken_returns401")
    void accessProtectedEndpoint_withoutToken_returns401() {
        // Act — no Authorization header
        ResponseEntity<String> response = restTemplate.getForEntity(
                url("/users/me"), String.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── 8. protected endpoint — with valid token ──────────────────────────────

    @Test
    @Order(8)
    @DisplayName("accessProtectedEndpoint_withValidToken_returns200")
    void accessProtectedEndpoint_withValidToken_returns200() throws Exception {
        // Arrange
        String token = loginAsAdmin();
        HttpEntity<Void> request = new HttpEntity<>(getHeaders(token));

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                url("/users/me"), HttpMethod.GET, request, String.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode data = objectMapper.readTree(response.getBody()).path("data");
        assertThat(data.path("email").asText()).isEqualTo("admin@taskoryx.com");
    }
}
