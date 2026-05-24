package com.taskoryx.backend.exception;

import com.taskoryx.backend.dto.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler - HTTP Status Mapping Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // ─── ResourceNotFoundException → 404 ─────────────────────────────────────

    @Test
    @DisplayName("ResourceNotFoundException → 404 NOT FOUND")
    void resourceNotFound_returns404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Task", "id", UUID.randomUUID());
        ResponseEntity<ApiResponse<Void>> response = handler.handleResourceNotFoundException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
    }

    @Test
    @DisplayName("ResourceNotFoundException message chứa tên resource")
    void resourceNotFound_messageContainsResourceName() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Task", "id", UUID.randomUUID());
        ResponseEntity<ApiResponse<Void>> response = handler.handleResourceNotFoundException(ex);

        assertThat(response.getBody().getMessage()).contains("Task");
    }

    // ─── ForbiddenException → 403 ────────────────────────────────────────────

    @Test
    @DisplayName("ForbiddenException → 403 FORBIDDEN")
    void forbidden_returns403() {
        ForbiddenException ex = new ForbiddenException("Bạn không có quyền");
        ResponseEntity<ApiResponse<Void>> response = handler.handleForbiddenException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().isSuccess()).isFalse();
    }

    // ─── BadRequestException → 400 ───────────────────────────────────────────

    @Test
    @DisplayName("BadRequestException → 400 BAD REQUEST")
    void badRequest_returns400() {
        BadRequestException ex = new BadRequestException("Dữ liệu không hợp lệ");
        ResponseEntity<ApiResponse<Void>> response = handler.handleBadRequestException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("Dữ liệu không hợp lệ");
    }

    // ─── BadCredentialsException → 401 ───────────────────────────────────────

    @Test
    @DisplayName("BadCredentialsException → 401 UNAUTHORIZED")
    void badCredentials_returns401() {
        BadCredentialsException ex = new BadCredentialsException("Bad creds");
        ResponseEntity<ApiResponse<Void>> response = handler.handleBadCredentialsException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getMessage()).contains("mật khẩu");
    }

    // ─── AccessDeniedException → 403 ─────────────────────────────────────────

    @Test
    @DisplayName("AccessDeniedException → 403 FORBIDDEN")
    void accessDenied_returns403() {
        AccessDeniedException ex = new AccessDeniedException("Access denied");
        ResponseEntity<ApiResponse<Void>> response = handler.handleAccessDeniedException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().isSuccess()).isFalse();
    }

    // ─── Generic Exception → 500 ─────────────────────────────────────────────

    @Test
    @DisplayName("Exception không xử lý được → 500 INTERNAL SERVER ERROR")
    void genericException_returns500() throws Exception {
        Exception ex = new RuntimeException("Unexpected error");
        ResponseEntity<ApiResponse<Void>> response = handler.handleGlobalException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().isSuccess()).isFalse();
    }

    // ─── ResourceNotFoundException với custom message ─────────────────────────

    @Test
    @DisplayName("ResourceNotFoundException với direct message")
    void resourceNotFound_directMessage_isPreserved() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Không tìm thấy task này");
        ResponseEntity<ApiResponse<Void>> response = handler.handleResourceNotFoundException(ex);

        assertThat(response.getBody().getMessage()).isEqualTo("Không tìm thấy task này");
    }
}
