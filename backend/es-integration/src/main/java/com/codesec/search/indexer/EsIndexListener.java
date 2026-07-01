package com.codesec.search.indexer;

import com.codesec.search.event.VulnIndexedEvent;
import com.codesec.search.repository.SnippetDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Asynchronous @TransactionalEventListener for VulnIndexedEvent.
 * Runs AFTER the transaction commits, so ES indexing failure
 * does NOT roll back the vuln_finding / vuln_ticket persistence.
 *
 * Change log (M1-S3 opt):
 * - @EventListener → @TransactionalEventListener(phase = AFTER_COMMIT)
 * - Wrapped logic in try-catch so ES errors never propagate to caller
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
     * Transactionally decoupled event handler.
     * Runs only after the publishing transaction commits successfully.
     * ES indexing failures are logged but never propagated,
     * ensuring vuln_finding persistence is never rolled back.
     */
    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT,
        condition = "#event.findingIds != null && !#event.findingIds.isEmpty()"
    )
    public void onVulnIndexed(VulnIndexedEvent event) {
        log.info("EsIndexListener received: {} findingIds for project={}",
                event.getFindingIds().size(), event.getProjectId());

        try {
            // In production, this would:
            // 1. Query vuln_finding table for the given IDs
            // 2. Map via VulnFindingEsMapper to VulnDocument list
            // 3. Call esUpsertService.upsertBatch(docs)
            //
            // The mapper integration (JDBC → VulnDocument) requires E-S2-CRITICAL
            // VulnService + DataSource to be wired. Here we provide the scaffolding.

            log.info("EsIndexListener: event processed. " +
                    "Full mapping requires E-S2-CRITICAL VulnService + DataSource integration.");
        } catch (Exception e) {
            // ES indexing failure is isolated from the transaction.
            // Log the error but never throw it back to the caller.
            log.error("EsIndexListener failed to index vuln findings for project={}: {}",
                    event.getProjectId(), e.getMessage(), e);
        }
    }

    /**
     * Index file snippets associated with findings.
     * v1: only indexes file_path (no content).
     */
    public void indexSnippetsForFindings(VulnIndexedEvent event) {
        try {
            var snippets = snippetReader.readSnippets(event.getProjectId(), event.getFindingIds());
            for (SnippetDocument snippet : snippets) {
                esUpsertService.upsertSnippet(snippet.getFilePath(), snippet);
            }
        } catch (Exception e) {
            log.error("Snippet indexing failed for project={}: {}",
                    event.getProjectId(), e.getMessage(), e);
        }
    }
}
