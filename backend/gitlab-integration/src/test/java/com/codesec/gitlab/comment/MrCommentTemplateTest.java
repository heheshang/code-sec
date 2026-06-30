package com.codesec.gitlab.comment;

import com.codesec.engine.model.Finding;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MrCommentTemplate")
class MrCommentTemplateTest {

    @Nested
    @DisplayName("rendering")
    class Rendering {

        @Test
        @DisplayName("should render passed comment when no findings")
        void renderPassed() {
            String result = MrCommentTemplate.render("scan-001", List.of());

            assertThat(result).contains("Security Scan Complete");
            assertThat(result).contains("scan-001");
            assertThat(result).contains("All Clear");
            assertThat(result).contains("No vulnerabilities detected");
        }

        @Test
        @DisplayName("should render findings by severity")
        void renderWithFindings() {
            List<Finding> findings = List.of(
                createFinding("SQL Injection", "critical"),
                createFinding("XSS", "high"),
                createFinding("Hardcoded Password", "medium"),
                createFinding("Weak Crypto", "low")
            );

            String result = MrCommentTemplate.render("scan-002", findings);

            assertThat(result).contains("Security Scan Complete");
            assertThat(result).contains("scan-002");
            assertThat(result).contains("1 Critical");
            assertThat(result).contains("1 High");
            assertThat(result).contains("1 Medium");
            assertThat(result).contains("1 Low");
            assertThat(result).contains("Blocked");
        }

        @Test
        @DisplayName("should show merge allowed for medium/low only")
        void renderMergeAllowed() {
            List<Finding> findings = List.of(
                createFinding("Minor Issue", "medium"),
                createFinding("Info", "low")
            );

            String result = MrCommentTemplate.render("scan-003", findings);

            assertThat(result).contains("Merge Allowed");
            assertThat(result).doesNotContain("Blocked");
        }
    }

    @Nested
    @DisplayName("truncation")
    class Truncation {

        @Test
        @DisplayName("should not truncate when under max bytes")
        void noTruncationWhenSmall() {
            String result = MrCommentTemplate.render("scan-004", List.of());

            byte[] bytes = result.getBytes(StandardCharsets.UTF_8);
            assertThat(bytes.length).isLessThanOrEqualTo(MrCommentTemplate.MAX_BYTES);
            assertThat(result).doesNotContain("truncated");
        }

        @Test
        @DisplayName("should truncate when exceeding 65535 bytes")
        void truncateWhenTooLarge() {
            String hugeInput = "A".repeat(67000);
            String result = MrCommentTemplate.truncateToMaxBytes(hugeInput);

            byte[] bytes = result.getBytes(StandardCharsets.UTF_8);
            assertThat(bytes.length).isLessThanOrEqualTo(MrCommentTemplate.MAX_BYTES);
            assertThat(result).contains("truncated");
        }

        @Test
        @DisplayName("should truncate with safe UTF-8 boundary")
        void truncateSafeUtf8() {
            // Create content that would cross a multi-byte boundary
            String result = MrCommentTemplate.truncateToMaxBytes("A".repeat(67000));

            byte[] bytes = result.getBytes(StandardCharsets.UTF_8);
            assertThat(bytes.length).isLessThanOrEqualTo(MrCommentTemplate.MAX_BYTES);
            assertThat(result).contains("truncated");
        }

        @Test
        @DisplayName("should handle zero findings gracefully")
        void zeroFindings() {
            String result = MrCommentTemplate.render("scan-006", List.of());

            assertThat(result).contains("All Clear");
            assertThat(result).doesNotContain("Blocked");
        }
    }

    @Nested
    @DisplayName("emoji rendering")
    class Emoji {

        @Test
        @DisplayName("should render correct emojis for each severity")
        void emojis() {
            List<Finding> findings = List.of(
                createFinding("Crit", "critical"),
                createFinding("High", "high"),
                createFinding("Med", "medium"),
                createFinding("Low", "low")
            );

            String result = MrCommentTemplate.render("scan-007", findings);

            assertThat(result).contains("\uD83D\uDD34"); // red
            assertThat(result).contains("\uD83D\uDFE0"); // orange
            assertThat(result).contains("\uD83D\uDFE1"); // yellow
            assertThat(result).contains("\uD83D\uDFE2"); // green
        }
    }

    private static Finding createFinding(String title, String severity) {
        return Finding.builder()
            .vulnId("vuln-" + java.util.UUID.randomUUID().toString().substring(0, 8))
            .scanId("scan-test")
            .ruleId("java/test-001")
            .title(title)
            .severity(severity)
            .filePath("src/main/java/com/example/Test.java")
            .lineStart(42)
            .lineEnd(42)
            .codeSnippet("some code")
            .description("Test finding")
            .fixSuggestion("Fix it")
            .exploitability("exploitable")
            .exploitReason("taint source reachable")
            .discoveredAt(Instant.now())
            .build();
    }
}
