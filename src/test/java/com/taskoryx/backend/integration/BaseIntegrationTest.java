package com.taskoryx.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Map;

/**
 * Base class for all integration tests.
 * Provides helper utilities: base URL construction, login helper, and auth header builder.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class BaseIntegrationTest {

    @LocalServerPort
    protected int port;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected ObjectMapper objectMapper;

    // ── URL helpers ──────────────────────────────────────────────────────────

    protected String url(String path) {
        return "http://localhost:" + port + path;
    }

    // ── Auth helpers ─────────────────────────────────────────────────────────

    /**
     * Logs in as the default admin user and returns the access token.
     */
    protected String loginAsAdmin() {
        return loginAs("admin@taskoryx.com", "Admin@123456");
    }

    /**
     * Logs in with the given credentials and returns the access token.
     * Throws if login fails.
     */
    protected String loginAs(String email, String password) {
        try {
            Map<String, String> body = Map.of("email", email, "password", password);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    url("/auth/login"), request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Login failed with status: " + response.getStatusCode()
                        + " body: " + response.getBody());
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("data").path("accessToken").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to login as " + email, e);
        }
    }

    /**
     * Logs in and returns the refresh token.
     */
    protected String loginAsAdminGetRefreshToken() {
        try {
            Map<String, String> body = Map.of("email", "admin@taskoryx.com", "password", "Admin@123456");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    url("/auth/login"), request, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("data").path("refreshToken").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to login as admin", e);
        }
    }

    /**
     * Builds HttpHeaders with JSON content type and Bearer token.
     */
    protected HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }

    /**
     * Builds HttpHeaders with Bearer token but no explicit content type (for GET).
     */
    protected HttpHeaders getHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    /**
     * Extracts a string field from a JSON response body at path data.<fieldName>.
     */
    protected String extractDataField(String responseBody, String fieldName) {
        try {
            return objectMapper.readTree(responseBody).path("data").path(fieldName).asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract field '" + fieldName + "' from: " + responseBody, e);
        }
    }

    /**
     * Extracts a JsonNode at path data from a JSON response body.
     */
    protected JsonNode extractData(String responseBody) {
        try {
            return objectMapper.readTree(responseBody).path("data");
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse response: " + responseBody, e);
        }
    }
}
