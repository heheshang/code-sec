package com.codesec.common.dto;

import java.time.Instant;
import java.util.Map;

public record FindingDto(
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
    Instant discoveredAt,
    String aiVerdict,
    Double aiConfidence,
    String aiExplanation,
    String aiGeneratedPatch
) {

    public FindingDto withScanId(String newScanId) {
        return new FindingDto(
            vulnId, projectId, newScanId, engine, ruleId, title, severity,
            filePath, lineStart, lineEnd, codeSnippet, description,
            fixSuggestion, cwe, cve, exploitability, exploitReason,
            engineRaw, discoveredAt,
            aiVerdict, aiConfidence, aiExplanation, aiGeneratedPatch
        );
    }
}
