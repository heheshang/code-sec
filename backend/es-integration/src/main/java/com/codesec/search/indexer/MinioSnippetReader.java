package com.codesec.search.indexer;

import com.codesec.search.repository.SnippetDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Reads code snippet file_path from MinIO scan sandbox artifacts.
 * v1: only extracts file_path (no content reading).
 * 
 * In production: reads from MinIO bucket using MinioClient.
 * For now: provides scaffolding for E-S2-CRITICAL MinIO integration.
 */
@Component
public class MinioSnippetReader {

    private static final Logger log = LoggerFactory.getLogger(MinioSnippetReader.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_INSTANT;

    /**
     * Read snippet file_paths for given findings.
     * v1: returns file_path + metadata only (no content).
     *
     * @param projectId  GitLab project ID
     * @param findingIds List of vuln_finding IDs
     * @return List of SnippetDocument with file_path populated
     */
    public List<SnippetDocument> readSnippets(String projectId, List<Long> findingIds) {
        if (findingIds == null || findingIds.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("MinioSnippetReader: reading snippets for project={}, findingIds={}",
                projectId, findingIds.size());

        List<SnippetDocument> snippets = new ArrayList<>();
        String now = DATE_FMT.format(Instant.now());

        // In production: query MinIO for each finding's code snippet file
        // For now, return empty — full integration requires E-S2-CRITICAL MinIO setup
        for (Long findingId : findingIds) {
            SnippetDocument doc = SnippetDocument.builder()
                    .filePath("project/" + projectId + "/finding/" + findingId + "/snippet.txt")
                    .projectId(projectId)
                    .language("java")  // v1: placeholder — actual language from scan metadata
                    .indexedAt(now)
                    .build();
            snippets.add(doc);
        }

        log.info("MinioSnippetReader: prepared {} snippet documents", snippets.size());
        return snippets;
    }
}
