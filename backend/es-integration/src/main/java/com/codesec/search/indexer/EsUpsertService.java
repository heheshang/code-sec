package com.codesec.search.indexer;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import com.codesec.search.config.EsClientConfig;
import com.codesec.search.exception.VulnIndexingFailedException;
import com.codesec.search.repository.VulnDocument;
import com.codesec.search.repository.VulnFindingEsMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Encapsulates ES bulk upsert logic with retry.
 * Called by EsIndexListener synchronously.
 *
 * v1: max 3 retries with exponential backoff (1s/3s/5s).
 * On final failure → VulnIndexingFailedException → transaction rollback.
 */
@Service
public class EsUpsertService {

    private static final Logger log = LoggerFactory.getLogger(EsUpsertService.class);

    private static final int MAX_RETRIES = 3;
    private static final int[] BACKOFF_MS = {1000, 3000, 5000};

    private final ElasticsearchClient esClient;
    private final EsClientConfig.EsIndexInitializer indexInitializer;

    public EsUpsertService(ElasticsearchClient esClient,
                           EsClientConfig.EsIndexInitializer indexInitializer) {
        this.esClient = esClient;
        this.indexInitializer = indexInitializer;
    }

    /**
     * Bulk upsert VulnDocuments into the vuln index.
     * Retries up to MAX_RETRIES times with exponential backoff.
     *
     * @param docs VulnDocuments to index
     * @throws VulnIndexingFailedException on final failure after all retries
     */
    public void upsertBatch(List<VulnDocument> docs) {
        if (docs == null || docs.isEmpty()) {
            return;
        }

        String index = indexInitializer.getVulnIndex();
        log.info("Upserting {} docs to index '{}'", docs.size(), index);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                BulkRequest.Builder bulkBuilder = new BulkRequest.Builder()
                        .index(index);

                for (VulnDocument doc : docs) {
                    bulkBuilder.operations(op -> op
                            .index(idx -> idx
                                    .id(doc.getId())
                                    .document(doc)
                            )
                    );
                }

                BulkResponse response = esClient.bulk(bulkBuilder.build());

                if (response.errors()) {
                    // Log individual failures
                    for (BulkResponseItem item : response.items()) {
                        if (item.error() != null) {
                            log.error("Bulk item failed: id={}, error={}", item.id(), item.error().reason());
                        }
                    }
                    throw new RuntimeException("Bulk upsert had errors");
                }

                log.info("Bulk upsert successful: {} docs, took {}ms", docs.size(), response.took());
                return; // Success — exit

            } catch (Exception e) {
                if (attempt == MAX_RETRIES) {
                    log.error("Bulk upsert failed after {} attempts: {}", MAX_RETRIES, e.getMessage());
                    throw new VulnIndexingFailedException(
                            "ES bulk upsert failed after " + MAX_RETRIES + " attempts",
                            e, attempt);
                }

                int backoff = BACKOFF_MS[attempt - 1];
                log.warn("Bulk upsert attempt {}/{} failed, retrying in {}ms: {}",
                        attempt, MAX_RETRIES, backoff, e.getMessage());
                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new VulnIndexingFailedException("Retry interrupted", ie, attempt);
                }
            }
        }
    }

    /**
     * Upsert a single SnippetDocument into the file_snippet index.
     */
    public void upsertSnippet(String id, Object snippetDoc) {
        String index = indexInitializer.getSnippetIndex();
        try {
            esClient.index(i -> i
                    .index(index)
                    .id(id)
                    .document(snippetDoc)
            );
            log.debug("Snippet upserted: id={} → index '{}'", id, index);
        } catch (Exception e) {
            log.error("Snippet upsert failed: id={}, error={}", id, e.getMessage());
            throw new VulnIndexingFailedException("Snippet upsert failed: " + id, e, 1);
        }
    }
}
