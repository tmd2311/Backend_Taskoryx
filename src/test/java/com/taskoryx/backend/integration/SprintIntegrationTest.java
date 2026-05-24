package com.taskoryx.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Sprint endpoints.
 *
 * Endpoints under test:
 *   POST   /projects/{projectId}/sprints   — create sprint
 *   POST   /sprints/{id}/start             — PLANNED → ACTIVE
 *   POST   /sprints/{id}/complete          — ACTIVE  → COMPLETED
 *   DELETE /sprints/{id}                   — delete (PLANNED only)
 *
 * The tests are ordered to manage the "only one ACTIVE sprint per project"
 * constraint:
 *
 *   1. createSprint_validData_returns201
 *   2. startSprint_planned_returns200          → sprint-2 becomes ACTIVE
 *   3. startSprint_whenAnotherActive_returns400 → sprint-3 cannot start while sprint-2 is ACTIVE
 *   4. completeSprint_active_returns200        → sprint-2 transitions to COMPLETED (frees ACTIVE slot)
 *   5. deleteSprint_planned_returns200         → sprint-5 deleted while PLANNED
 *   6. deleteSprint_active_returns400          → sprint-6 started → ACTIVE, then DELETE must fail
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SprintIntegrationTest extends BaseIntegrationTest {

    private String adminToken;
    private String projectId;

    /** The sprint created and started in @Order(2); shared with @Order(3) and @Order(4). */
    private String activeSprint2Id;

    // ── Suite-level setup ─────────────────────────────────────────────────────

    @BeforeAll
    void setUpSuite() throws Exception {
        adminToken = loginAsAdmin();

        // Create a dedicated project for all sprint tests.
        // Omitting projectConfig → enabledModules defaults to "all enabled".
        Map<String, Object> projectBody = Map.of(
                "name", "Sprint Test Project",
                "key", "SPRT"
        );
        HttpEntity<Map<String, Object>> projectReq = new HttpEntity<>(projectBody, authHeaders(adminToken));
        ResponseEntity<String> projectResp = restTemplate.postForEntity(
                url("/projects"), projectReq, String.class);

        assertThat(projectResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        projectId = objectMapper.readTree(projectResp.getBody())
                .path("data").path("id").asText();
        assertThat(projectId).isNotBlank();
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * Creates a PLANNED sprint with the given name in the suite project.
     * Uses a 14-day window so startDate < endDate.
     *
     * @return the new sprint's id
     */
    private String createSprintAndGetId(String name) throws Exception {
        Map<String, Object> body = Map.of(
                "name", name,
                "startDate", LocalDate.now().toString(),
                "endDate", LocalDate.now().plusDays(14).toString()
        );
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, authHeaders(adminToken));
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/projects/" + projectId + "/sprints"), request, String.class);

        assertThat(response.getStatusCode())
                .as("Expected 201 when creating sprint '%s', got %s. Body: %s",
                        name, response.getStatusCode(), response.getBody())
                .isEqualTo(HttpStatus.CREATED);

        String id = objectMapper.readTree(response.getBody())
                .path("data").path("id").asText();
        assertThat(id).isNotBlank();
        return id;
    }

    // ── 1. createSprint — valid data → 201 ───────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("createSprint_validData_returns201")
    void createSprint_validData_returns201() throws Exception {
        // Arrange
        Map<String, Object> body = Map.of(
                "name", "Sprint 1",
                "goal", "Deliver MVP",
                "startDate", LocalDate.now().toString(),
                "endDate", LocalDate.now().plusDays(14).toString()
        );
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, authHeaders(adminToken));

        // Act
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/projects/" + projectId + "/sprints"), request, String.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        JsonNode data = objectMapper.readTree(response.getBody()).path("data");
        assertThat(data.path("id").asText()).isNotBlank();
        assertThat(data.path("name").asText()).isEqualTo("Sprint 1");
        assertThat(data.path("status").asText()).isEqualTo("PLANNED");
        assertThat(data.path("projectId").asText()).isEqualTo(projectId);
    }

    // ── 2. startSprint — PLANNED → ACTIVE → 200 ──────────────────────────────

    @Test
    @Order(2)
    @DisplayName("startSprint_planned_returns200")
    void startSprint_planned_returns200() throws Exception {
        // Arrange — create a new PLANNED sprint
        activeSprint2Id = createSprintAndGetId("Sprint To Start");

        // Act
        HttpEntity<Void> request = new HttpEntity<>(authHeaders(adminToken));
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/sprints/" + activeSprint2Id + "/start"), request, String.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode data = objectMapper.readTree(response.getBody()).path("data");
        assertThat(data.path("status").asText()).isEqualTo("ACTIVE");
    }

    // ── 3. startSprint — second sprint while one is ACTIVE → 400 ─────────────

    @Test
    @Order(3)
    @DisplayName("startSprint_whenAnotherActive_returns400")
    void startSprint_whenAnotherActive_returns400() throws Exception {
        // At this point activeSprint2Id (from test 2) is still ACTIVE.
        // Create another PLANNED sprint and attempt to start it.

        String sprintConflict = createSprintAndGetId("Conflict Sprint");

        // Act — try to start while activeSprint2Id is ACTIVE
        HttpEntity<Void> startReq = new HttpEntity<>(authHeaders(adminToken));
        ResponseEntity<String> startResp = restTemplate.postForEntity(
                url("/sprints/" + sprintConflict + "/start"), startReq, String.class);

        // Assert — business rule: only one ACTIVE sprint per project at a time
        assertThat(startResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── 4. completeSprint — ACTIVE → COMPLETED → 200 ─────────────────────────

    @Test
    @Order(4)
    @DisplayName("completeSprint_active_returns200")
    void completeSprint_active_returns200() throws Exception {
        // activeSprint2Id is still ACTIVE from test 2 (test 3 only attempted, did not change it).

        // Act — complete the active sprint
        HttpEntity<Void> completeReq = new HttpEntity<>(authHeaders(adminToken));
        ResponseEntity<String> completeResp = restTemplate.postForEntity(
                url("/sprints/" + activeSprint2Id + "/complete"), completeReq, String.class);

        // Assert
        assertThat(completeResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode data = objectMapper.readTree(completeResp.getBody()).path("data");
        assertThat(data.path("status").asText()).isEqualTo("COMPLETED");
    }

    // ── 5. deleteSprint — PLANNED sprint → 200 ───────────────────────────────

    @Test
    @Order(5)
    @DisplayName("deleteSprint_planned_returns200")
    void deleteSprint_planned_returns200() throws Exception {
        // Arrange — create a PLANNED sprint (do NOT start it)
        String sprintId = createSprintAndGetId("Sprint To Delete");

        // Act
        HttpEntity<Void> deleteReq = new HttpEntity<>(authHeaders(adminToken));
        ResponseEntity<String> deleteResp = restTemplate.exchange(
                url("/sprints/" + sprintId), HttpMethod.DELETE, deleteReq, String.class);

        // Assert
        assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ── 6. deleteSprint — ACTIVE sprint → 400 ────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("deleteSprint_active_returns400")
    void deleteSprint_active_returns400() throws Exception {
        // Arrange — create a new sprint and start it (no ACTIVE sprint in project after test 4 completed it)
        String sprintId = createSprintAndGetId("Active Sprint Cannot Delete");

        HttpEntity<Void> startReq = new HttpEntity<>(authHeaders(adminToken));
        ResponseEntity<String> startResp = restTemplate.postForEntity(
                url("/sprints/" + sprintId + "/start"), startReq, String.class);
        assertThat(startResp.getStatusCode())
                .as("Sprint must start successfully before testing delete constraint")
                .isEqualTo(HttpStatus.OK);

        // Act — attempt to delete the now-ACTIVE sprint
        HttpEntity<Void> deleteReq = new HttpEntity<>(authHeaders(adminToken));
        ResponseEntity<String> deleteResp = restTemplate.exchange(
                url("/sprints/" + sprintId), HttpMethod.DELETE, deleteReq, String.class);

        // Assert — deleting an ACTIVE sprint must be rejected
        assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
