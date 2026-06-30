package com.codesec.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.codesec.search.config.EsClientConfig;
import com.codesec.search.dto.SearchRequest;
import com.codesec.search.repository.VulnDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Business logic for vuln search: constructs query DSL, calls ES, transforms results.
 */
@Service
public class VulnSearchService {

    private static final Logger log = LoggerFactory.getLogger(VulnSearchService.class);

    private final ElasticsearchClient esClient;
    private final EsClientConfig.EsIndexInitializer indexInitializer;
    private final SearchQueryBuilder queryBuilder;

    public VulnSearchService(ElasticsearchClient esClient,
                             EsClientConfig.EsIndexInitializer indexInitializer,
                             SearchQueryBuilder queryBuilder) {
        this.esClient = esClient;
        this.indexInitializer = indexInitializer;
        this.queryBuilder = queryBuilder;
    }

    /**
     * Search vuln index with full-text query + filters + highlight.
     */
    public com.codesec.search.dto.SearchResponse<VulnDocument> search(SearchRequest request) {
        validateRequest(request);

        List<String> warnings = new ArrayList<>();

        // Clamp page_size if over 100
        int pageSize = request.getPageSize();
        if (pageSize > 100) {
            pageSize = 100;
            warnings.add("page_size_clamped: 100");
        }
        final int effectivePageSize = pageSize;

        try {
            SearchResponse<VulnDocument> esResponse = esClient.search(s -> s
                            .index(indexInitializer.getVulnIndex())
                            .query(queryBuilder.buildVulnQuery(request))
                            .from(queryBuilder.from(request))
                            .size(effectivePageSize)
                            .highlight(h -> h.fields(queryBuilder.buildVulnHighlight()))
                            .sort(queryBuilder.buildSort(request))
                            .trackTotalHits(th -> th.enabled(true)),
                    VulnDocument.class
            );

            long tookMs = esResponse.took();
            long total = esResponse.hits().total() != null ? esResponse.hits().total().value() : 0;

            List<VulnDocument> items = esResponse.hits().hits().stream()
                    .filter(hit -> hit.source() != null)
                    .map(Hit::source)
                    .collect(Collectors.toList());

            Map<String, List<String>> highlights = extractHighlights(esResponse);

            log.info("Vuln search: q='{}', total={}, took={}ms", request.getQ(), total, tookMs);

            return com.codesec.search.dto.SearchResponse.<VulnDocument>builder()
                    .total(total)
                    .page(request.getPage())
                    .pageSize(effectivePageSize)
                    .tookMs(tookMs)
                    .items(items)
                    .highlights(highlights)
                    .warnings(warnings.isEmpty() ? null : warnings)
                    .build();

        } catch (Exception e) {
            log.error("ES vuln search failed: {}", e.getMessage(), e);
            throw new RuntimeException("Search service unavailable", e);
        }
    }

    private void validateRequest(SearchRequest request) {
        if (request.getQ() == null || request.getQ().isBlank()) {
            // Empty query is allowed — returns all results with filters only
            // But spec says 400 for empty q? Let's allow empty q with at least one filter
            if (request.getSeverity() == null && request.getExploitability() == null
                    && request.getProjectId() == null && request.getEngine() == null) {
                throw new IllegalArgumentException("SEARCH_QUERY_EMPTY: provide q or at least one filter");
            }
        }
    }

    private Map<String, List<String>> extractHighlights(SearchResponse<VulnDocument> esResponse) {
        Map<String, List<String>> result = new HashMap<>();
        for (Hit<VulnDocument> hit : esResponse.hits().hits()) {
            if (hit.highlight() != null && hit.source() != null) {
                hit.highlight().forEach((field, fragments) -> {
                    result.computeIfAbsent(hit.source().getId() + ":" + field,
                            k -> new ArrayList<>()).addAll(fragments);
                });
            }
        }
        return result;
    }
}
