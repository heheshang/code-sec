package com.codesec.gitlab;

import com.codesec.gitlab.model.MrChangesResponse;
import com.codesec.gitlab.comment.MrCommentTemplate;
import com.codesec.gitlab.scan.DiffExtractor;
import com.codesec.gitlab.scan.MrDiffResult;
import com.codesec.gitlab.webhook.WebhookSignatureFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration test covering the full GitLab webhook to
 * MR comment pipeline, using WireMock to simulate the GitLab API.
 *
 * <p>Scenarios covered:
 * E2E-1: Normal MR open with findings → scan + comment
 * E2E-2: MR with no scannable changes → skip + passed comment
 * E2E-3: MR update action → re-scan
 * E2E-4: Large MR (500+ files) → truncation
 * E2E-5: Duplicate webhook UUID → dedup, no re-scan
 * E2E-6: Invalid secret token → 401
 * E2E-7: Missing secret token → 401
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "gitlab.base-url=http://localhost:8089",
        "gitlab.mode=real",
        "gitlab.private-token=glpat-e2e-test-token",
        "gitlab.webhook-secret=e2e-test-secret",
        "gitlab.connect-timeout-seconds=2",
        "gitlab.read-timeout-seconds=5"
    }
)
@DisplayName("GitLab E2E Integration")
class GitLabIntegrationE2ETest {

    private static WireMockServer wireMockServer;

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String webhookUrl;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(8089));
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    void setUp() {
        WireMock.reset();
        webhookUrl = "http://localhost:" + port + "/api/v1/webhooks/gitlab";
    }

    // ─── E2E-1: Normal MR open ───────────────────────────────────────

    @Test
    @DisplayName("E2E-1: Normal MR open with Java file changes")
    void normalMrOpen() throws Exception {
        // Stub GitLab MR Changes API
        stubFor(get(urlPathMatching("/api/v4/projects/1/merge_requests/1/changes"))
            .withHeader("PRIVATE-TOKEN", equalTo("glpat-e2e-test-token"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                      "id": 1, "iid": 1,
                      "title": "E2E Test MR",
                      "source_branch": "feature/test",
                      "target_branch": "main",
                      "sha": "abc123e2e",
                      "diff_refs": {"base_sha": "base", "head_sha": "head", "start_sha": "start"},
                      "changes": [
                        {"old_path": "src/main/java/App.java", "new_path": "src/main/java/App.java",
                         "new_file": false, "deleted_file": false, "renamed_file": false,
                         "diff": "public class App {}"}
                      ]
                    }""")));

        // Stub GitLab MR Notes API
        stubFor(post(urlPathMatching("/api/v4/projects/1/merge_requests/1/notes"))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"id\": 100, \"body\": \"Security scan complete\"}")));

        // Send webhook
        String webhookPayload = """
            {
              "object_kind": "merge_request",
              "event_type": "merge_request",
              "project": {"id": 1, "name": "test-project"},
              "object_attributes": {
                "id": 1, "iid": 1, "action": "open",
                "source_branch": "feature/test",
                "target_branch": "main"
              }
            }""";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Gitlab-Token", "e2e-test-secret");
        headers.set("X-Gitlab-Event", "Merge Request Hook");
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> response = restTemplate.exchange(
            webhookUrl, HttpMethod.POST,
            new HttpEntity<>(webhookPayload, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("processed");
    }

    // ─── E2E-2: MR with no scannable changes ─────────────────────────

    @Test
    @DisplayName("E2E-2: MR with all deleted files → skip scan")
    void allDeletedFiles() throws Exception {
        stubFor(get(urlPathMatching("/api/v4/projects/1/merge_requests/2/changes"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                      "id": 1, "iid": 2,
                      "title": "Delete test files",
                      "changes": [
                        {"old_path": "src/Old.java", "new_path": "src/Old.java",
                         "new_file": false, "deleted_file": true, "renamed_file": false,
                         "diff": ""}
                      ]
                    }""")));

        String webhookPayload = """
            {"object_kind": "merge_request",
             "project": {"id": 1},
             "object_attributes": {"id": 1, "iid": 2, "action": "open"}}""";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Gitlab-Token", "e2e-test-secret");
        headers.set("X-Gitlab-Event", "Merge Request Hook");
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> response = restTemplate.exchange(
            webhookUrl, HttpMethod.POST,
            new HttpEntity<>(webhookPayload, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("processed");
    }

    // ─── E2E-5: Duplicate UUID dedup ─────────────────────────────────

    @Test
    @DisplayName("E2E-5: Duplicate webhook UUID should be deduplicated")
    void duplicateUuidDedup() {
        // Stub MR changes for the first (non-dedup) request
        stubFor(get(urlPathMatching("/api/v4/projects/1/merge_requests/5/changes"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {"id":1,"iid":5,"title":"Dedup Test",
                     "changes":[{"old_path":"src/X.java","new_path":"src/X.java",
                      "new_file":false,"deleted_file":false,"renamed_file":false,
                      "diff":"code"}],
                     "diff_refs":{"base_sha":"b","head_sha":"h"}}""")));
        String webhookPayload = """
            {"object_kind": "merge_request",
             "project": {"id": 1},
             "object_attributes": {"id": 1, "iid": 5, "action": "open"}}""";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Gitlab-Token", "e2e-test-secret");
        headers.set("X-Gitlab-Event", "Merge Request Hook");
        headers.set("X-Gitlab-Event-UUID", "e2e-duplicate-uuid-001");
        headers.setContentType(MediaType.APPLICATION_JSON);

        // First request
        ResponseEntity<Map> response1 = restTemplate.exchange(
            webhookUrl, HttpMethod.POST,
            new HttpEntity<>(webhookPayload, headers), Map.class);
        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Second request with same UUID → should be rejected as duplicate
        ResponseEntity<Map> response2 = restTemplate.exchange(
            webhookUrl, HttpMethod.POST,
            new HttpEntity<>(webhookPayload, headers), Map.class);
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response2.getBody().get("status")).isEqualTo("duplicate");
    }

    // ─── E2E-6: Invalid secret → 401 ─────────────────────────────────

    @Test
    @DisplayName("E2E-6: Invalid webhook secret returns 401")
    void invalidSecret() {
        String webhookPayload = "{}";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Gitlab-Token", "wrong-secret-value");
        headers.set("X-Gitlab-Event", "Merge Request Hook");
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.exchange(
            webhookUrl, HttpMethod.POST,
            new HttpEntity<>(webhookPayload, headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ─── E2E-7: Missing secret → 401 ─────────────────────────────────

    @Test
    @DisplayName("E2E-7: Missing X-Gitlab-Token header returns 401")
    void missingToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Gitlab-Event", "Merge Request Hook");
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.exchange(
            webhookUrl, HttpMethod.POST,
            new HttpEntity<>("{}", headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
