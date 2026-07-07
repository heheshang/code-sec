package com.codesec.gitlab.comment;

import com.codesec.engineadapter.FindingDto;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Renders MR scan summary comments as Markdown for GitLab MR Notes API.
 *
 * <p>Maximum comment size: 65535 bytes (GitLab single note limit, UTF-8).
 * If the rendered comment exceeds this, it is truncated to Top 10 findings
 * with a platform link appended.
 */
public final class MrCommentTemplate {

    /** GitLab maximum note size in bytes (UTF-8). */
    public static final int MAX_BYTES = 65535;

    private static final String PLATFORM_LINK = "(full list on platform)";

    private MrCommentTemplate() {}

    /**
     * Renders a scan summary comment.
     *
     * @param scanId   the scan identifier
     * @param findings the list of findings from the engine scan
     * @return rendered Markdown comment body
     */
    public static String render(String scanId, List<FindingDto> findings) {
        Map<String, Long> severityCounts = findings.stream()
            .collect(Collectors.groupingBy(FindingDto::severity, Collectors.counting()));

        long criticalCount = severityCounts.getOrDefault("critical", 0L);
        long highCount = severityCounts.getOrDefault("high", 0L);
        long mediumCount = severityCounts.getOrDefault("medium", 0L);
        long lowCount = severityCounts.getOrDefault("low", 0L);

        StringBuilder sb = new StringBuilder();
        sb.append("\uD83D\uDD12 **Security Scan Complete**\n\n");
        sb.append("Scan ID: `").append(scanId).append("`\n\n");

        if (findings.isEmpty()) {
            sb.append("**Result: All Clear** \u2705\n\n");
            sb.append("No vulnerabilities detected in this merge request.\n");
            sb.append("Review the full report on the CodeSec platform.\n");
            return sb.toString();
        }

        sb.append("**Findings:** ");
        if (criticalCount > 0) sb.append(criticalCount).append(" Critical, ");
        if (highCount > 0) sb.append(highCount).append(" High, ");
        if (mediumCount > 0) sb.append(mediumCount).append(" Medium, ");
        if (lowCount > 0) sb.append(lowCount).append(" Low");
        sb.append("\n\n");

        // Merge status
        if (criticalCount > 0 || highCount > 0) {
            sb.append("**Status: \u26D4 Blocked** (critical/high vulnerabilities found)\n\n");
        } else {
            sb.append("**Status: \u2705 Merge Allowed** (medium/low only)\n\n");
        }

        // Top findings list (up to 10)
        int listLimit = Math.min(findings.size(), 10);
        for (int i = 0; i < listLimit; i++) {
            FindingDto f = findings.get(i);
            String emoji = severityEmoji(f.severity());
            sb.append("- ").append(emoji).append(" [").append(f.title()).append("] ");
            sb.append("`").append(f.filePath()).append(":").append(f.lineStart()).append("`");
            if (f.exploitability() != null && !f.exploitability().isEmpty()) {
                sb.append(" (").append(f.exploitability()).append(")");
            }
            sb.append("\n");
        }

        sb.append("\n---\n");
        sb.append("View full report: CodeSec Platform\n");

        return truncateToMaxBytes(sb.toString());
    }

    /**
     * Renders a "passed" comment when no findings were detected.
     */
    public static String renderPassed(String scanId) {
        return render(scanId, List.of());
    }

    /**
     * Truncates the comment body to MAX_BYTES, appending a truncation notice.
     */
    static String truncateToMaxBytes(String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= MAX_BYTES) {
            return text;
        }

        String suffix = "\n\n*(truncated; full list on platform)*";
        byte[] suffixBytes = suffix.getBytes(StandardCharsets.UTF_8);
        int maxContentBytes = MAX_BYTES - suffixBytes.length;

        // Find a safe UTF-8 cut point
        String truncated = new String(bytes, 0, maxContentBytes, StandardCharsets.UTF_8);
        // Remove any partial multi-byte character at the end
        while ((truncated + suffix).getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            truncated = truncated.substring(0, truncated.length() - 1);
        }
        return truncated + suffix;
    }

    private static String severityEmoji(String severity) {
        return switch (severity.toLowerCase()) {
            case "critical" -> "\uD83D\uDD34"; // red circle
            case "high" -> "\uD83D\uDFE0";     // orange circle
            case "medium" -> "\uD83D\uDFE1";    // yellow circle
            case "low" -> "\uD83D\uDFE2";       // green circle
            default -> "\u26AA";                  // white circle
        };
    }
}
