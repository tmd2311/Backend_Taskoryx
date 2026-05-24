package com.taskoryx.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Project endpoints.
 *
 * Endpoints under test:
 *   POST /projects
 *   GET  /projects
 *   GET  /projects/{id}
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ProjectIntegrationTest extends BaseIntegrationTest {

    /** Admin access token — refreshed before the suite runs. */
    private String adminToken;

    @BeforeEach
    void setUp() {
        if (adminToken == null) {
            adminToken = loginAsAdmin();
        }
    }

    // ── 1. createProject — valid data → 201 ──────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("createProject_validData_returns201")
    void createProject_validData_returns201() throws Exception {
        // Arrange
        Map<String, Object> body = Map.of(
                "name", "Test Project",
                "key", "TST"
        );
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, authHeaders(adminToken));

        // Act
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/projects"), request, String.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        JsonNode data = objectMapper.readTree(response.getBody()).path("data");
        assertThat(data.path("id").asText()).isNotBlank();
        assertThat(data.path("key").asText()).isEqualTo("TST");
        assertThat(data.path("name").asText()).isEqualTo("Test Project");
    }

    // ── 2. createProject — invalid key (lowercase) → 400 ────────────────────

    @Test
    @Order(2)
    @DisplayName("createProject_invalidKey_returns400")
    void createProject_invalidKey_returns400() {
        // Arrange — key must match ^[A-Z0-9]{2,10}$; "abc" is lowercase → invalid
        Map<String, Object> body = Map.of(
                "name", "Lower Key Project",
                "key", "abc"
        );
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, authHeaders(adminToken));

        // Act
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/projects"), request, String.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── 3. createProject — duplicate key → 400 ───────────────────────────────

    @Test
    @Order(3)
    @DisplayName("createProject_duplicate_key_returns400")
    void createProject_duplicate_key_returns400() {
        // Arrange — create the first project
        Map<String, Object> firstBody = Map.of(
                "name", "First Project DUP",
                "key", "DUP1"
        );
        HttpEntity<Map<String, Object>> firstRequest = new HttpEntity<>(firstBody, authHeaders(adminToken));
        ResponseEntity<String> firstResponse = restTemplate.postForEntity(
                url("/projects"), firstRequest, String.class);
        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Act — attempt to create a second project with the same key
        Map<String, Object> dupBody = Map.of(
                "name", "Second Project DUP",
                "key", "DUP1"
        );
        HttpEntity<Map<String, Object>> dupRequest = new HttpEntity<>(dupBody, authHeaders(adminToken));
        ResponseEntity<String> dupResponse = restTemplate.postForEntity(
                url("/projects"), dupRequest, String.class);

        // Assert — duplicate key should be rejected
        assertThat(dupResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── 4. createProject — without auth → 401 ────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("createProject_withoutAuth_returns401")
    void createProject_withoutAuth_returns401() {
        // Arrange — no Authorization header
        Map<String, Object> body = Map.of(
                "name", "Anon Project",
                "key", "ANO"
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        // Act
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/projects"), request, String.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── 5. getProject — existing id → 200 ────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("getProject_existingId_returns200")
    void getProject_existingId_returns200() throws Exception {
        // Arrange — create a project first
        Map<String, Object> createBody = Map.of(
                "name", "Fetch Me Project",
                "key", "FMP"
        );
        HttpEntity<Map<String, Object>> createRequest = new HttpEntity<>(createBody, authHeaders(adminToken));
        ResponseEntity<String> createResponse = restTemplate.postForEntity(
                url("/projects"), createRequest, String.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        String projectId = objectMapper.readTree(createResponse.getBody())
                .path("data").path("id").asText();
        assertThat(projectId).isNotBlank();

        // Act
        HttpEntity<Void> getRequest = new HttpEntity<>(getHeaders(adminToken));
        ResponseEntity<String> response = restTemplate.exchange(
                url("/projects/" + projectId), HttpMethod.GET, getRequest, String.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode data = objectMapper.readTree(response.getBody()).path("data");
        assertThat(data.path("id").asText()).isEqualTo(projectId);
        assertThat(data.path("key").asText()).isEqualTo("FMP");
    }

    // ── 6. getProject — non-existent id → 404 ────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("getProject_nonExistentId_returns404")
    void getProject_nonExistentId_returns404() {
        // Arrange — random UUID that does not exist
        String randomId = UUID.randomUUID().toString();
        HttpEntity<Void> request = new HttpEntity<>(getHeaders(adminToken));

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                url("/projects/" + randomId), HttpMethod.GET, request, String.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── 7. getMyProjects — authenticated → 200 with array ────────────────────

    @Test
    @Order(7)
    @DisplayName("getMyProjects_returns200WithList")
    void getMyProjects_returns200WithList() throws Exception {
        // Act
        HttpEntity<Void> request = new HttpEntity<>(getHeaders(adminToken));
        ResponseEntity<String> response = restTemplate.exchange(
                url("/projects"), HttpMethod.GET, request, String.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode data = objectMapper.readTree(response.getBody()).path("data");
        assertThat(data.isArray()).isTrue();
    }
}
