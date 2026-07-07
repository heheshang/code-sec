package com.codesec.engineadapter;

import java.time.Instant;
import java.util.Map;

/**
 * DTO mirroring {@code engine.model.Finding} for cross-module communication.
 * Domain and worker should depend on this type, not the engine's internal record,
 * so the anti-corruption layer is effective.
 */
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
    /**
     * Returns a copy with the scanId replaced.
     */
    public FindingDto withScanId(String newScanId) {
        return new FindingDto(
            vulnId, projectId, newScanId, engine, ruleId, title, severity,
            filePath, lineStart, lineEnd, codeSnippet, description,
            fixSuggestion, cwe, cve, exploitability, exploitReason,
            engineRaw, discoveredAt,
            aiVerdict, aiConfidence, aiExplanation, aiGeneratedPatch
        );
    }

    /**
     * Maps an engine {@link com.codesec.engine.model.Finding} to this DTO.
     */
    public static FindingDto from(com.codesec.engine.model.Finding f) {
        return new FindingDto(
            f.vulnId(), f.projectId(), f.scanId(), f.engine(), f.ruleId(),
            f.title(), f.severity(), f.filePath(), f.lineStart(), f.lineEnd(),
            f.codeSnippet(), f.description(), f.fixSuggestion(), f.cwe(), f.cve(),
            f.exploitability(), f.exploitReason(), f.engineRaw(), f.discoveredAt(),
            f.aiVerdict(), f.aiConfidence(), f.aiExplanation(), f.aiGeneratedPatch()
        );
    }
}
