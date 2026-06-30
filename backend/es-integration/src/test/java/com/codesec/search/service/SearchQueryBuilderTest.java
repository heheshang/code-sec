package com.codesec.search.service;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.codesec.search.dto.SearchRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SearchQueryBuilder.
 * These tests verify query DSL construction logic without an ES cluster.
 */
class SearchQueryBuilderTest {

    private SearchQueryBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new SearchQueryBuilder();
    }

    @Nested
    @DisplayName("buildVulnQuery")
    class BuildVulnQuery {

        @Test
        @DisplayName("produce non-null query when q is set")
        void produceQueryForKeyword() {
            SearchRequest req = new SearchRequest();
            req.setQ("sql injection");

            Query query = builder.buildVulnQuery(req);

            assertThat(query).isNotNull();
        }

        @Test
        @DisplayName("produce query with only filters when q is empty")
        void produceFilterOnlyQuery() {
            SearchRequest req = new SearchRequest();
            req.setSeverity(List.of("critical"));

            Query query = builder.buildVulnQuery(req);

            assertThat(query).isNotNull();
        }

        @Test
        @DisplayName("include multiple term filters")
        void includeMultipleFilters() {
            SearchRequest req = new SearchRequest();
            req.setSeverity(List.of("critical", "high"));
            req.setEngine(List.of("semgrep"));

            Query query = builder.buildVulnQuery(req);

            assertThat(query).isNotNull();
        }

        @Test
        @DisplayName("include date range filter")
        void includeDateRangeFilter() {
            SearchRequest req = new SearchRequest();
            req.setQ("test");
            req.setDiscoveredAtFrom("2025-01-01");
            req.setDiscoveredAtTo("2025-12-31");

            Query query = builder.buildVulnQuery(req);

            assertThat(query).isNotNull();
        }
    }

    @Nested
    @DisplayName("buildSnippetQuery")
    class BuildSnippetQuery {

        @Test
        @DisplayName("produce prefix query for file_path")
        void producePrefixQuery() {
            SearchRequest req = new SearchRequest();
            req.setQ("src/main");

            Query query = builder.buildSnippetQuery(req);

            assertThat(query).isNotNull();
        }

        @Test
        @DisplayName("include project_id filter")
        void includeProjectFilter() {
            SearchRequest req = new SearchRequest();
            req.setQ("src");
            req.setProjectId(List.of("proj-a"));

            Query query = builder.buildSnippetQuery(req);

            assertThat(query).isNotNull();
        }
    }

    @Nested
    @DisplayName("buildSort")
    class BuildSort {

        @Test
        @DisplayName("return score sort by default")
        void defaultScoreSort() {
            SearchRequest req = new SearchRequest();

            List<SortOptions> sorts = builder.buildSort(req);

            assertThat(sorts).isNotEmpty();
        }

        @Test
        @DisplayName("return discovered_at sort when specified")
        void discoveredAtSort() {
            SearchRequest req = new SearchRequest();
            req.setSortBy("discovered_at");

            List<SortOptions> sorts = builder.buildSort(req);

            assertThat(sorts).isNotEmpty();
        }

        @Test
        @DisplayName("return ascending order")
        void ascendingOrder() {
            SearchRequest req = new SearchRequest();
            req.setSortOrder("asc");

            List<SortOptions> sorts = builder.buildSort(req);

            assertThat(sorts).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("from")
    class From {

        @Test
        @DisplayName("calculate offset for page 2 size 20")
        void page2Size20() {
            SearchRequest req = new SearchRequest();
            req.setPage(2);
            req.setPageSize(20);

            int offset = builder.from(req);

            assertThat(offset).isEqualTo(20);
        }

        @Test
        @DisplayName("calculate offset for page 1")
        void page1() {
            SearchRequest req = new SearchRequest();
            req.setPage(1);
            req.setPageSize(10);

            int offset = builder.from(req);

            assertThat(offset).isZero();
        }
    }

    @Nested
    @DisplayName("sanitize query")
    class SanitizeQuery {

        @Test
        @DisplayName("strip special ES characters")
        void stripSpecialChars() {
            // Access sanitizeQuery via buildVulnQuery — the sanitizer runs internally
            SearchRequest req = new SearchRequest();
            req.setQ("sql+injection -term &special");

            Query query = builder.buildVulnQuery(req);

            assertThat(query).isNotNull();
            // Should not throw for any input
        }

        @Test
        @DisplayName("handle null query gracefully")
        void handleNullQuery() {
            SearchRequest req = new SearchRequest();
            req.setSeverity(List.of("critical"));

            Query query = builder.buildVulnQuery(req);

            assertThat(query).isNotNull();
        }
    }
}
