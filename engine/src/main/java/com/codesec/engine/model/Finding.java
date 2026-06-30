package com.codesec.engine.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "vuln_id", "project_id", "scan_id", "engine", "rule_id",
    "title", "severity", "file_path", "line_start", "line_end",
    "code_snippet", "description", "fix_suggestion", "cwe", "cve",
    "exploitability", "exploit_reason", "engine_raw", "discovered_at"
})
public record Finding(
    String vulnId,
    Integer projectId,
    String scanId,
    String engine,
    String ruleId,
    String title,
    String severity,
    String filePath,
    int lineStart,
    int lineEnd,
    String codeSnippet,
    String description,
    String fixSuggestion,
    String cwe,
    String cve,
    String exploitability,
    String exploitReason,
    Map<String, Object> engineRaw,
    Instant discoveredAt
) {
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String vulnId = UUID.randomUUID().toString();
        private Integer projectId;
        private String scanId;
        private String engine = "self_sast";
        private String ruleId;
        private String title;
        private String severity;
        private String filePath;
        private int lineStart;
        private int lineEnd;
        private String codeSnippet;
        private String description;
        private String fixSuggestion;
        private String cwe;
        private String cve;
        private String exploitability = "potentially_exploitable";
        private String exploitReason;
        private Map<String, Object> engineRaw;
        private Instant discoveredAt = Instant.now();

        public Builder vulnId(String vulnId) { this.vulnId = vulnId; return this; }
        public Builder projectId(Integer projectId) { this.projectId = projectId; return this; }
        public Builder scanId(String scanId) { this.scanId = scanId; return this; }
        public Builder engine(String engine) { this.engine = engine; return this; }
        public Builder ruleId(String ruleId) { this.ruleId = ruleId; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder severity(String severity) { this.severity = severity; return this; }
        public Builder filePath(String filePath) { this.filePath = filePath; return this; }
        public Builder lineStart(int lineStart) { this.lineStart = lineStart; return this; }
        public Builder lineEnd(int lineEnd) { this.lineEnd = lineEnd; return this; }
        public Builder codeSnippet(String codeSnippet) { this.codeSnippet = codeSnippet; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder fixSuggestion(String fixSuggestion) { this.fixSuggestion = fixSuggestion; return this; }
        public Builder cwe(String cwe) { this.cwe = cwe; return this; }
        public Builder cve(String cve) { this.cve = cve; return this; }
        public Builder exploitability(String exploitability) { this.exploitability = exploitability; return this; }
        public Builder exploitReason(String exploitReason) { this.exploitReason = exploitReason; return this; }
        public Builder engineRaw(Map<String, Object> engineRaw) { this.engineRaw = engineRaw; return this; }
        public Builder discoveredAt(Instant discoveredAt) { this.discoveredAt = discoveredAt; return this; }

        public Finding build() {
            return new Finding(
                vulnId, projectId, scanId, engine, ruleId, title, severity,
                filePath, lineStart, lineEnd, codeSnippet, description,
                fixSuggestion, cwe, cve, exploitability, exploitReason,
                engineRaw, discoveredAt
            );
        }
    }
}
