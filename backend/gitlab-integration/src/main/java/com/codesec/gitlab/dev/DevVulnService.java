package com.codesec.gitlab.dev;

import com.codesec.engine.model.Finding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Development/stub implementation of VulnService.
 *
 * <p><b>TEMPORARY:</b> In-memory vulnerability store for development.
 * Replace with real VulnService backed by database and Elasticsearch
 * when E-S2-CRITICAL delivers the production implementation.
 *
 * @deprecated Replace with real VulnService per E-S2-CRITICAL.
 *     Scheduled removal: M2 engine integration sprint.
 */
@Deprecated(forRemoval = true)
@Component
public class DevVulnService {

    private static final Logger log = LoggerFactory.getLogger(DevVulnService.class);

    private final List<Finding> stored = Collections.synchronizedList(new ArrayList<>());

    /**
     * Persists a batch of findings (E-S2-CRITICAL § 3.5.4 locked signature).
     *
     * @param scanId   the scan identifier
     * @param findings the findings to persist
     * @return number of persisted findings
     */
    public int persistBatch(String scanId, List<Finding> findings) {
        if (findings == null || findings.isEmpty()) {
            return 0;
        }

        List<Finding> tagged = findings.stream()
            .map(f -> Finding.builder()
                .vulnId(f.vulnId())
                .scanId(scanId)
                .ruleId(f.ruleId())
                .title(f.title())
                .severity(f.severity())
                .filePath(f.filePath())
                .lineStart(f.lineStart())
                .lineEnd(f.lineEnd())
                .codeSnippet(f.codeSnippet())
                .description(f.description())
                .fixSuggestion(f.fixSuggestion())
                .cwe(f.cwe())
                .cve(f.cve())
                .exploitability(f.exploitability())
                .exploitReason(f.exploitReason())
                .engineRaw(f.engineRaw())
                .discoveredAt(f.discoveredAt())
                .build())
            .toList();

        stored.addAll(tagged);
        log.info("DevVulnService persisted {} findings for scanId={}", tagged.size(), scanId);
        return tagged.size();
    }

    /** Returns all stored findings (for test assertions). */
    public List<Finding> getAllFindings() {
        return List.copyOf(stored);
    }

    /** Clears all stored findings. */
    public void clear() {
        stored.clear();
    }
}
