package com.codesec.gitlab.webhook;

import com.codesec.gitlab.GitLabProperties;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("WebhookSignatureFilter")
class WebhookSignatureFilterTest {

    private WebhookSignatureFilter filter;
    private GitLabProperties properties;
    private FilterChain mockChain;

    @BeforeEach
    void setUp() {
        properties = new GitLabProperties(
            "https://gitlab.example.com", "glpat-token", 10, 30,
            "real", "test-secret-value-123"
        );
        filter = new WebhookSignatureFilter(properties);
        mockChain = mock(FilterChain.class);
    }

    @Nested
    @DisplayName("token validation - success paths")
    class SuccessPaths {

        @Test
        @DisplayName("should pass with valid X-Gitlab-Token")
        void validToken() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Gitlab-Token", "test-secret-value-123");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(request, response, mockChain);

            assertThat(response.getStatus()).isEqualTo(200);
            verify(mockChain).doFilter(request, response);
        }

        @Test
        @DisplayName("should pass with valid token and UUID header")
        void validTokenWithUuid() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Gitlab-Token", "test-secret-value-123");
            request.addHeader("X-Gitlab-Event-UUID", "uuid-001");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(request, response, mockChain);

            assertThat(response.getStatus()).isEqualTo(200);
            verify(mockChain).doFilter(request, response);
            assertThat(filter.eventUuidCache().getIfPresent("uuid-001")).isNotNull();
        }
    }

    @Nested
    @DisplayName("token validation - failure paths")
    class FailurePaths {

        @Test
        @DisplayName("should reject missing X-Gitlab-Token header")
        void missingToken() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(request, response, mockChain);

            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(response.getErrorMessage()).contains("Missing");
            verify(mockChain, never()).doFilter(any(), any());
        }

        @Test
        @DisplayName("should reject empty X-Gitlab-Token header")
        void emptyToken() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Gitlab-Token", "");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(request, response, mockChain);

            assertThat(response.getStatus()).isEqualTo(401);
            verify(mockChain, never()).doFilter(any(), any());
        }

        @Test
        @DisplayName("should reject mismatched X-Gitlab-Token")
        void mismatchedToken() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Gitlab-Token", "wrong-secret");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(request, response, mockChain);

            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(response.getErrorMessage()).contains("Invalid");
            verify(mockChain, never()).doFilter(any(), any());
        }

        @Test
        @DisplayName("should reject when no webhook secret is configured")
        void noSecretConfigured() throws Exception {
            GitLabProperties noSecretProps = new GitLabProperties(
                "https://gitlab.example.com", "token", 10, 30,
                "real", ""
            );
            WebhookSignatureFilter noSecretFilter = new WebhookSignatureFilter(noSecretProps);

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Gitlab-Token", "anything");
            MockHttpServletResponse response = new MockHttpServletResponse();

            noSecretFilter.doFilterInternal(request, response, mockChain);

            assertThat(response.getStatus()).isEqualTo(401);
            verify(mockChain, never()).doFilter(any(), any());
        }
    }

    @Nested
    @DisplayName("UUID replay protection")
    class UuidReplay {

        @Test
        @DisplayName("should detect duplicate UUID and return 200 without processing")
        void duplicateUuid() throws Exception {
            // First request
            MockHttpServletRequest request1 = new MockHttpServletRequest();
            request1.addHeader("X-Gitlab-Token", "test-secret-value-123");
            request1.addHeader("X-Gitlab-Event-UUID", "uuid-replay-001");
            MockHttpServletResponse response1 = new MockHttpServletResponse();
            filter.doFilterInternal(request1, response1, mockChain);
            // Reset mock to verify second call does NOT invoke chain
            reset(mockChain);

            // Second request with same UUID
            MockHttpServletRequest request2 = new MockHttpServletRequest();
            request2.addHeader("X-Gitlab-Token", "test-secret-value-123");
            request2.addHeader("X-Gitlab-Event-UUID", "uuid-replay-001");
            MockHttpServletResponse response2 = new MockHttpServletResponse();

            filter.doFilterInternal(request2, response2, mockChain);

            assertThat(response2.getStatus()).isEqualTo(200);
            verify(mockChain, never()).doFilter(any(), any());
        }

        @Test
        @DisplayName("should allow different UUIDs to pass through")
        void differentUuids() throws Exception {
            MockHttpServletRequest request1 = new MockHttpServletRequest();
            request1.addHeader("X-Gitlab-Token", "test-secret-value-123");
            request1.addHeader("X-Gitlab-Event-UUID", "uuid-uniq-001");
            MockHttpServletResponse response1 = new MockHttpServletResponse();
            filter.doFilterInternal(request1, response1, mockChain);
            reset(mockChain);

            MockHttpServletRequest request2 = new MockHttpServletRequest();
            request2.addHeader("X-Gitlab-Token", "test-secret-value-123");
            request2.addHeader("X-Gitlab-Event-UUID", "uuid-uniq-002");
            MockHttpServletResponse response2 = new MockHttpServletResponse();

            filter.doFilterInternal(request2, response2, mockChain);

            assertThat(response2.getStatus()).isEqualTo(200);
            verify(mockChain).doFilter(request2, response2);
        }

        @Test
        @DisplayName("should skip replay check when no UUID header (old GitLab)")
        void noUuidHeader() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Gitlab-Token", "test-secret-value-123");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(request, response, mockChain);

            assertThat(response.getStatus()).isEqualTo(200);
            verify(mockChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("constant-time comparison")
    class ConstantTime {

        @Test
        @DisplayName("should return true for equal strings")
        void equalStrings() {
            assertThat(WebhookSignatureFilter.constantTimeEquals("abc", "abc")).isTrue();
        }

        @Test
        @DisplayName("should return false for different strings")
        void differentStrings() {
            assertThat(WebhookSignatureFilter.constantTimeEquals("abc", "abd")).isFalse();
        }

        @Test
        @DisplayName("should return false for different length strings")
        void differentLength() {
            assertThat(WebhookSignatureFilter.constantTimeEquals("abc", "abcd")).isFalse();
        }

        @Test
        @DisplayName("should handle null inputs")
        void nullInputs() {
            assertThat(WebhookSignatureFilter.constantTimeEquals(null, "abc")).isFalse();
            assertThat(WebhookSignatureFilter.constantTimeEquals("abc", null)).isFalse();
            assertThat(WebhookSignatureFilter.constantTimeEquals(null, null)).isFalse();
        }
    }
}
