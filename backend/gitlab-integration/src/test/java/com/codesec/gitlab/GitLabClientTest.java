package com.codesec.gitlab;

import com.codesec.gitlab.model.MrChange;
import com.codesec.gitlab.model.MrChangesResponse;
import com.codesec.gitlab.scan.DiffExtractor;
import com.codesec.gitlab.scan.MrDiffResult;
import tools.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for GitLabClient using WireMock to simulate the GitLab API.
 */
@DisplayName("GitLabClient")
class GitLabClientTest {

    private static WireMockServer wireMockServer;

    private GitLabClient client;
    private GitLabProperties properties;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(8090));
        wireMockServer.start();
        WireMock.configureFor("localhost", 8090);
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
        properties = new GitLabProperties(
            "http://localhost:8090", "glpat-test-token", 5, 10,
            "real", "test-secret"
        );
        client = new GitLabClient(properties);
    }

    @Test
    @DisplayName("should return MR changes for 200 response")
    void getMrChangesSuccess() throws Exception {
        stubFor(get(urlEqualTo("/api/v4/projects/1/merge_requests/1/changes"))
            .withHeader("PRIVATE-TOKEN", equalTo("glpat-test-token"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                      "id": 1, "iid": 1, "title": "Test MR",
                      "source_branch": "feature", "target_branch": "main",
                      "sha": "abc123",
                      "diff_refs": {"base_sha": "base", "head_sha": "head"},
                      "changes": [
                        {"old_path": "src/App.java", "new_path": "src/App.java",
                         "new_file": false, "deleted_file": false, "renamed_file": false,
                         "diff": "public class App {}"}
                      ]
                    }""")));

        MrChangesResponse response = client.getMrChanges(1, 1);

        assertThat(response.title()).isEqualTo("Test MR");
        assertThat(response.changes()).hasSize(1);
        assertThat(response.changes().get(0).newPath()).isEqualTo("src/App.java");
    }

    @Test
    @DisplayName("should throw GitLabApiException for 401")
    void unauthorizedThrows() {
        stubFor(get(urlEqualTo("/api/v4/projects/1/merge_requests/1/changes"))
            .willReturn(aResponse().withStatus(401)));

        assertThatThrownBy(() -> client.getMrChanges(1, 1))
            .isInstanceOf(com.codesec.gitlab.model.GitLabApiException.class)
            .matches(e -> ((com.codesec.gitlab.model.GitLabApiException) e).isUnauthorized());
    }

    @Test
    @DisplayName("should throw GitLabApiException for 403")
    void forbiddenThrows() {
        stubFor(get(urlEqualTo("/api/v4/projects/1/merge_requests/1/changes"))
            .willReturn(aResponse().withStatus(403)));

        assertThatThrownBy(() -> client.getMrChanges(1, 1))
            .isInstanceOf(com.codesec.gitlab.model.GitLabApiException.class)
            .matches(e -> ((com.codesec.gitlab.model.GitLabApiException) e).isForbidden());
    }

    @Test
    @DisplayName("should throw GitLabApiException for 404")
    void notFoundThrows() {
        stubFor(get(urlEqualTo("/api/v4/projects/1/merge_requests/1/changes"))
            .willReturn(aResponse().withStatus(404)));

        assertThatThrownBy(() -> client.getMrChanges(1, 1))
            .isInstanceOf(com.codesec.gitlab.model.GitLabApiException.class)
            .matches(e -> ((com.codesec.gitlab.model.GitLabApiException) e).isNotFound());
    }

    @Test
    @DisplayName("should retry on 429 then succeed")
    void rateLimitRetry() throws Exception {
        stubFor(get(urlEqualTo("/api/v4/projects/1/merge_requests/1/changes"))
            .inScenario("rate-limit-then-success")
            .whenScenarioStateIs(STARTED)
            .willReturn(aResponse().withStatus(429))
            .willSetStateTo("rate-limited"));

        stubFor(get(urlEqualTo("/api/v4/projects/1/merge_requests/1/changes"))
            .inScenario("rate-limit-then-success")
            .whenScenarioStateIs("rate-limited")
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {"id":1,"iid":1,"title":"After Retry",
                     "changes":[],
                     "diff_refs": {"base_sha":"b","head_sha":"h"}}""")));

        MrChangesResponse response = client.getMrChanges(1, 1);

        assertThat(response.title()).isEqualTo("After Retry");
    }

    @Test
    @DisplayName("should retry on 5xx then succeed")
    void serverErrorRetry() throws Exception {
        stubFor(get(urlEqualTo("/api/v4/projects/1/merge_requests/1/changes"))
            .inScenario("server-error-then-success")
            .whenScenarioStateIs(STARTED)
            .willReturn(aResponse().withStatus(500))
            .willSetStateTo("recovered"));

        stubFor(get(urlEqualTo("/api/v4/projects/1/merge_requests/1/changes"))
            .inScenario("server-error-then-success")
            .whenScenarioStateIs("recovered")
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {"id":1,"iid":1,"title":"After Recovery",
                     "changes":[],
                     "diff_refs": {"base_sha":"b","head_sha":"h"}}""")));

        MrChangesResponse response = client.getMrChanges(1, 1);

        assertThat(response.title()).isEqualTo("After Recovery");
    }
}
