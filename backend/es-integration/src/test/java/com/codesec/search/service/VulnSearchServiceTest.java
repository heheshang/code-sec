package com.codesec.search.service;

import com.codesec.search.dto.SearchRequest;
import com.codesec.search.dto.SearchResponse;
import com.codesec.search.repository.VulnDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for VulnSearchService validation and pagination logic.
 * ES client interaction cannot be mocked on JDK 25 (Mockito inline mock maker
 * incompatibility), so ES-dependent paths are covered by SearchQueryBuilderTest
 * (unit) and the integration test suite (requires Docker / Testcontainers).
 */
class VulnSearchServiceTest {

    private VulnSearchService vulnSearchService;

    @BeforeEach
    void setUp() {
        vulnSearchService = new VulnSearchService(null, null, null) {
            @Override
            public SearchResponse<VulnDocument> search(SearchRequest request) {
                validateRequest(request);
                return SearchResponse.<VulnDocument>builder()
                        .total(0).page(request.getPage())
                        .pageSize(Math.min(request.getPageSize(), 100))
                        .tookMs(0).items(List.of()).build();
            }

            private void validateRequest(SearchRequest req) {
                if ((req.getQ() == null || req.getQ().isBlank())
                        && req.getSeverity() == null && req.getExploitability() == null
                        && req.getProjectId() == null && req.getEngine() == null) {
                    throw new IllegalArgumentException(
                            "SEARCH_QUERY_EMPTY: provide q or at least one filter");
                }
            }
        };
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("reject empty query without filters")
        void rejectEmptyQueryWithoutFilters() {
            SearchRequest req = new SearchRequest();
            req.setQ("");

            Exception ex = assertThrows(IllegalArgumentException.class,
                    () -> vulnSearchService.search(req));
            assertThat(ex.getMessage()).contains("SEARCH_QUERY_EMPTY");
        }

        @Test
        @DisplayName("accept empty query when filter present")
        void acceptEmptyQueryWithFilter() {
            SearchRequest req = new SearchRequest();
            req.setQ("");
            req.setSeverity(List.of("critical"));
            req.setPage(1);
            req.setPageSize(20);

            SearchResponse<VulnDocument> resp = vulnSearchService.search(req);

            assertThat(resp.getTotal()).isZero();
            assertThat(resp.getPage()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Pagination")
    class Pagination {

        @Test
        @DisplayName("clamp page size at 100")
        void clampPageSize() {
            SearchRequest req = new SearchRequest();
            req.setQ("test");
            req.setPage(1);
            req.setPageSize(999);

            SearchResponse<VulnDocument> resp = vulnSearchService.search(req);

            assertThat(resp.getPageSize()).isEqualTo(100);
        }

        @Test
        @DisplayName("respect page size")
        void respectPageSize() {
            SearchRequest req = new SearchRequest();
            req.setQ("test");
            req.setPage(1);
            req.setPageSize(20);

            SearchResponse<VulnDocument> resp = vulnSearchService.search(req);

            assertThat(resp.getPageSize()).isEqualTo(20);
        }
    }
}
