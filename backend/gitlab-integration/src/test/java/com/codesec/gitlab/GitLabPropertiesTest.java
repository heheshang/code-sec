package com.codesec.gitlab;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GitLabProperties")
class GitLabPropertiesTest {

    @Nested
    @DisplayName("defaults")
    class Defaults {

        @Test
        @DisplayName("should default base URL to gitlab.example.com")
        void defaultBaseUrl() {
            GitLabProperties props = new GitLabProperties(null, "token", 0, 0, null, null);
            assertThat(props.baseUrl()).isEqualTo("https://gitlab.example.com");
        }

        @Test
        @DisplayName("should default mode to auto")
        void defaultMode() {
            GitLabProperties props = new GitLabProperties("https://x", "token", 10, 30, null, "secret");
            assertThat(props.mode()).isEqualTo("auto");
            assertThat(props.isAutoMode()).isTrue();
        }

        @Test
        @DisplayName("should default connect timeout to 10s")
        void defaultConnectTimeout() {
            GitLabProperties props = new GitLabProperties("https://x", "token", 0, 0, "real", "secret");
            assertThat(props.connectTimeoutSeconds()).isEqualTo(10);
        }

        @Test
        @DisplayName("should default read timeout to 30s")
        void defaultReadTimeout() {
            GitLabProperties props = new GitLabProperties("https://x", "token", 10, 0, "real", "secret");
            assertThat(props.readTimeoutSeconds()).isEqualTo(30);
        }
    }

    @Nested
    @DisplayName("mode detection")
    class ModeDetection {

        @Test
        @DisplayName("isRealMode should return true for real")
        void realMode() {
            GitLabProperties props = new GitLabProperties("https://x", "token", 10, 30, "real", "secret");
            assertThat(props.isRealMode()).isTrue();
            assertThat(props.isMockMode()).isFalse();
            assertThat(props.isAutoMode()).isFalse();
        }

        @Test
        @DisplayName("isMockMode should return true for mock")
        void mockMode() {
            GitLabProperties props = new GitLabProperties("https://x", "token", 10, 30, "mock", "secret");
            assertThat(props.isMockMode()).isTrue();
            assertThat(props.isRealMode()).isFalse();
        }

        @Test
        @DisplayName("should be case-insensitive")
        void caseInsensitive() {
            GitLabProperties props = new GitLabProperties("https://x", "token", 10, 30, "REAL", "secret");
            assertThat(props.isRealMode()).isTrue();
        }
    }
}
