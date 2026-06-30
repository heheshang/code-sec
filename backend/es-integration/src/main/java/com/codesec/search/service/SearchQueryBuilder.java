package com.codesec.search.service;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.elasticsearch.core.search.HighlighterType;
import com.codesec.search.dto.SearchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Constructs ES Query DSL from SearchRequest.
 * Supports multi-field query (title^3 / description^2 / code_snippet),
 * filters (severity / exploitability / project_id / engine / date range),
 * highlight, pagination, sort, and prefix query for file_path.
 */
@Component
public class SearchQueryBuilder {

    private static final Logger log = LoggerFactory.getLogger(SearchQueryBuilder.class);

    /**
     * Build a BoolQuery for vuln index search.
     */
    public Query buildVulnQuery(SearchRequest request) {
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();

        // --- 1. Full-text multi-field query ---
        if (request.getQ() != null && !request.getQ().isBlank()) {
            String query = sanitizeQuery(request.getQ());
            boolQuery.must(MultiMatchQuery.of(m -> m
                    .query(query)
                    .fields("title^3", "description^2", "code_snippet",
                            "title.standard^3", "description.standard^2")
                    .type(TextQueryType.BestFields)
                    .tieBreaker(0.3)
            )._toQuery());
        }

        // --- 2. Term filters ---
        addTermsFilter(boolQuery, "severity", request.getSeverity());
        addTermsFilter(boolQuery, "exploitability", request.getExploitability());
        addTermsFilter(boolQuery, "project_id", request.getProjectId());
        addTermsFilter(boolQuery, "engine", request.getEngine());

        // --- 3. Date range filter ---
        if (request.getDiscoveredAtFrom() != null || request.getDiscoveredAtTo() != null) {
            boolQuery.filter(buildDateRangeFilter(request.getDiscoveredAtFrom(), request.getDiscoveredAtTo()));
        }

        return boolQuery.build()._toQuery();
    }

    /**
     * Build a BoolQuery for file_snippet index search.
     * v1: prefix query on file_path only (no content search).
     */
    public Query buildSnippetQuery(SearchRequest request) {
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();

        if (request.getQ() != null && !request.getQ().isBlank()) {
            String query = sanitizeQuery(request.getQ());
            // Prefix query for file_path (simulates "starts with" search)
            boolQuery.must(PrefixQuery.of(p -> p
                    .field("file_path")
                    .value(query)
            )._toQuery());
        }

        addTermsFilter(boolQuery, "project_id", request.getProjectId());

        return boolQuery.build()._toQuery();
    }

    /**
     * Build highlight configuration for vuln search.
     */
    public Map<String, HighlightField> buildVulnHighlight() {
        Map<String, HighlightField> highlights = new HashMap<>();
        highlights.put("title", HighlightField.of(h -> h
                .type(HighlighterType.Unified)
                .numberOfFragments(1)
                .fragmentSize(150)
                .preTags("<em>")
                .postTags("</em>")
        ));
        highlights.put("description", HighlightField.of(h -> h
                .type(HighlighterType.Unified)
                .numberOfFragments(2)
                .fragmentSize(200)
                .preTags("<em>")
                .postTags("</em>")
        ));
        highlights.put("code_snippet", HighlightField.of(h -> h
                .type(HighlighterType.Unified)
                .numberOfFragments(1)
                .fragmentSize(300)
                .preTags("<em>")
                .postTags("</em>")
        ));
        return highlights;
    }

    /**
     * Build sort specification.
     */
    public List<co.elastic.clients.elasticsearch._types.SortOptions> buildSort(SearchRequest request) {
        List<co.elastic.clients.elasticsearch._types.SortOptions> sorts = new ArrayList<>();
        String sortBy = request.getSortBy();
        SortOrder order = "asc".equalsIgnoreCase(request.getSortOrder()) ? SortOrder.Asc : SortOrder.Desc;

        if ("_score".equals(sortBy)) {
            sorts.add(co.elastic.clients.elasticsearch._types.SortOptions.of(s -> s
                    .score(sc -> sc.order(order))));
        } else if ("discovered_at".equals(sortBy)) {
            sorts.add(co.elastic.clients.elasticsearch._types.SortOptions.of(s -> s
                    .field(f -> f.field("discovered_at").order(order))));
        }

        return sorts;
    }

    /**
     * Calculate pagination from / size.
     */
    public int from(SearchRequest request) {
        return (request.getPage() - 1) * request.getPageSize();
    }

    // --- Private helpers ---

    private void addTermsFilter(BoolQuery.Builder boolQuery, String field, List<String> values) {
        if (values != null && !values.isEmpty()) {
            boolQuery.filter(TermsQuery.of(t -> t
                    .field(field)
                    .terms(terms -> terms.value(
                            values.stream()
                                    .map(co.elastic.clients.elasticsearch._types.FieldValue::of)
                                    .toList()
                    ))
            )._toQuery());
        }
    }

    private Query buildDateRangeFilter(String from, String to) {
        return RangeQuery.of(r -> {
            RangeQuery.Builder builder = r.field("discovered_at");
            if (from != null && !from.isBlank()) {
                builder.gte(co.elastic.clients.json.JsonData.of(from));
            }
            if (to != null && !to.isBlank()) {
                builder.lte(co.elastic.clients.json.JsonData.of(to));
            }
            return builder;
        })._toQuery();
    }

    /**
     * Sanitize query string: strip ES special chars, enforce max 200 chars.
     */
    private String sanitizeQuery(String raw) {
        if (raw == null) return "";
        String sanitized = raw
                .replaceAll("[+\\-=&|><!(){}\\[\\]^\"~*?/:\\\\]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (sanitized.length() > 200) {
            sanitized = sanitized.substring(0, 200);
        }
        log.debug("Sanitized query: '{}' → '{}'", raw, sanitized);
        return sanitized;
    }
}
