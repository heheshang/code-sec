package com.codesec.engineadapter;

import com.codesec.common.dto.FindingDto;
import com.codesec.engine.model.Finding;

public final class FindingDtoMapper {

    private FindingDtoMapper() {}

    public static FindingDto from(Finding f) {
        return new FindingDto(
            f.vulnId(), f.projectId(), f.scanId(), f.engine(), f.ruleId(),
            f.title(), f.severity(), f.filePath(), f.lineStart(), f.lineEnd(),
            f.codeSnippet(), f.description(), f.fixSuggestion(), f.cwe(), f.cve(),
            f.exploitability(), f.exploitReason(), f.engineRaw(), f.discoveredAt(),
            f.aiVerdict(), f.aiConfidence(), f.aiExplanation(), f.aiGeneratedPatch()
        );
    }
}
