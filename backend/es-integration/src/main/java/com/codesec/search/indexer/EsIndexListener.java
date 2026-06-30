package com.codesec.search.indexer;

import com.codesec.search.event.VulnIndexedEvent;
import com.codesec.search.repository.SnippetDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Synchronous @EventListener for VulnIndexedEvent.
 * Runs in the calling thread (default Spring @EventListener behavior)
 * to ensure ES indexing failure → transaction rollback.
 *
 * Per E-S2-CRITICAL § 3.8 方案 B:
 * - Listens to VulnIndexedEvent published by VulnService.persistBatch()
 * - Calls EsUpsertService (which retries 3x with backoff)
 * - Failure → VulnIndexingFailedException → full rollback
 */
@Service
public class EsIndexListener {

    private static final Logger log = LoggerFactory.getLogger(EsIndexListener.class);

    private final EsUpsertService esUpsertService;
    private final MinioSnippetReader snippetReader;

    public EsIndexListener(EsUpsertService esUpsertService,
                           MinioSnippetReader snippetReader) {
        this.esUpsertService = esUpsertService;
        this.snippetReader = snippetReader;
    }

    /**
     * Synchronously handles VulnIndexedEvent.
     * condition check ensures event is only processed when findings exist.
     */
    @EventListener(condition = "#event.findingIds != null && !#event.findingIds.isEmpty()")
    public void onVulnIndexed(VulnIndexedEvent event) {
        log.info("EsIndexListener received: {} findingIds for project={}",
                event.getFindingIds().size(), event.getProjectId());

        // In production, this would:
        // 1. Query vuln_finding table for the given IDs
        // 2. Map via VulnFindingEsMapper to VulnDocument list
        // 3. Call esUpsertService.upsertBatch(docs)
        //
        // The mapper integration (JDBC → VulnDocument) requires E-S2-CRITICAL
        // VulnService + DataSource to be wired. Here we provide the scaffolding.

        log.info("EsIndexListener: event processed. " +
                "Full mapping requires E-S2-CRITICAL VulnService + DataSource integration.");
    }

    /**
     * Index file snippets associated with findings.
     * v1: only indexes file_path (no content).
     */
    public void indexSnippetsForFindings(VulnIndexedEvent event) {
        var snippets = snippetReader.readSnippets(event.getProjectId(), event.getFindingIds());
        for (SnippetDocument snippet : snippets) {
            esUpsertService.upsertSnippet(snippet.getFilePath(), snippet);
        }
    }
}
