package com.codesec.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.codesec.search.config.EsClientConfig;
import com.codesec.search.dto.SearchRequest;
import com.codesec.search.repository.SnippetDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic for file_snippet search.
 * v1: prefix query on file_path only (no content full-text search).
 */
@Service
public class SnippetSearchService {

    private static final Logger log = LoggerFactory.getLogger(SnippetSearchService.class);

    private final ElasticsearchClient esClient;
    private final EsClientConfig.EsIndexInitializer indexInitializer;
    private final SearchQueryBuilder queryBuilder;

    public SnippetSearchService(ElasticsearchClient esClient,
                                EsClientConfig.EsIndexInitializer indexInitializer,
                                SearchQueryBuilder queryBuilder) {
        this.esClient = esClient;
        this.indexInitializer = indexInitializer;
        this.queryBuilder = queryBuilder;
    }

    /**
     * Search file_snippet index — v1: file_path prefix match only.
     */
    public com.codesec.search.dto.SearchResponse<SnippetDocument> search(SearchRequest request) {
        validateRequest(request);

        List<String> warnings = new ArrayList<>();
        int effectivePageSize = Math.min(request.getPageSize(), 100);
        if (request.getPageSize() > 100) {
            warnings.add("page_size_clamped: 100");
        }

        try {
            SearchResponse<SnippetDocument> esResponse = esClient.search(s -> s
                            .index(indexInitializer.getSnippetIndex())
                            .query(queryBuilder.buildSnippetQuery(request))
                            .from(queryBuilder.from(request))
                            .size(effectivePageSize)
                            .trackTotalHits(th -> th.enabled(true)),
                    SnippetDocument.class
            );

            long tookMs = esResponse.took();
            long total = esResponse.hits().total() != null ? esResponse.hits().total().value() : 0;

            List<SnippetDocument> items = esResponse.hits().hits().stream()
                    .filter(hit -> hit.source() != null)
                    .map(Hit::source)
                    .collect(Collectors.toList());

            log.info("Snippet search: q='{}', total={}, took={}ms", request.getQ(), total, tookMs);

            return com.codesec.search.dto.SearchResponse.<SnippetDocument>builder()
                    .total(total)
                    .page(request.getPage())
                    .pageSize(effectivePageSize)
                    .tookMs(tookMs)
                    .items(items)
                    .warnings(warnings.isEmpty() ? null : warnings)
                    .build();

        } catch (Exception e) {
            log.error("ES snippet search failed: {}", e.getMessage(), e);
            throw new RuntimeException("Search service unavailable", e);
        }
    }

    private void validateRequest(SearchRequest request) {
        if ((request.getQ() == null || request.getQ().isBlank())
                && (request.getProjectId() == null || request.getProjectId().isEmpty())) {
            throw new IllegalArgumentException("SEARCH_QUERY_EMPTY: provide q or project_id filter");
        }
    }
}
