package com.codesec.domain.service.search;

import com.codesec.common.dto.SearchRequest;
import com.codesec.common.dto.SearchResponse;
import com.codesec.domain.model.VulnDocument;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VulnSearchServiceTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query countQuery;

    @Mock
    private Query dataQuery;

    private VulnSearchService service;

    private List<Object[]> singleResult;

    @BeforeEach
    void setUp() {
        service = new VulnSearchService();
        ReflectionTestUtils.setField(service, "entityManager", entityManager);
        singleResult = new ArrayList<>();
        singleResult.add(new Object[]{"id1", "proj1", "rule1", "high", "yes", "SQL Injection",
                "Description", "code", "path/Main.java", "89", "codeql",
                "2026-01-01T00:00:00Z", "admin",
                "SQL <em>Injection</em>", "<em>Description</em>", "<em>code</em>"});
    }

    @Test
    void search_withQuery_returnsResults() {
        SearchRequest request = new SearchRequest();
        request.setQ("sql injection");
        request.setPage(1);
        request.setPageSize(20);

        when(entityManager.createNativeQuery(anyString())).thenReturn(countQuery, dataQuery);
        when(countQuery.getSingleResult()).thenReturn(1L);
        when(dataQuery.getResultList()).thenReturn(singleResult);

        SearchResponse<VulnDocument> response = service.search(request);

        assertEquals(1, response.getTotal());
        assertEquals(1, response.getItems().size());
        assertEquals("id1", response.getItems().get(0).getId());
        assertEquals("SQL Injection", response.getItems().get(0).getTitle());
        assertNotNull(response.getHighlights());
        assertTrue(response.getTookMs() >= 0);
    }

    @Test
    void search_withoutQueryAndFilters_throwsException() {
        SearchRequest request = new SearchRequest();
        request.setQ(null);
        assertThrows(IllegalArgumentException.class, () -> service.search(request));
    }

    @Test
    void search_withFiltersOnly_returnsResults() {
        SearchRequest request = new SearchRequest();
        request.setSeverity(List.of("high", "critical"));

        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{"id1", "proj1", "rule1", "high", "yes", "Title",
                "Desc", "code", "path/Main.java", "89", "codeql",
                "2026-01-01T00:00:00Z", "admin",
                "Title", "Desc", "code"});
        rows.add(new Object[]{"id2", "proj1", "rule1", "critical", "yes", "Title2",
                "Desc2", "code2", "path/App.java", "89", "semgrep",
                "2026-01-02T00:00:00Z", "admin",
                "Title2", "Desc2", "code2"});

        when(entityManager.createNativeQuery(anyString())).thenReturn(countQuery, dataQuery);
        when(countQuery.getSingleResult()).thenReturn(2L);
        when(dataQuery.getResultList()).thenReturn(rows);

        SearchResponse<VulnDocument> response = service.search(request);

        assertEquals(2, response.getTotal());
        assertEquals(2, response.getItems().size());
        assertNull(response.getHighlights());
    }

    @Test
    void search_withAllFilters_appliesThem() {
        SearchRequest request = new SearchRequest();
        request.setQ("password");
        request.setSeverity(List.of("high"));
        request.setExploitability(List.of("yes"));
        request.setProjectId(List.of("proj1"));
        request.setEngine(List.of("codeql"));

        when(entityManager.createNativeQuery(anyString())).thenReturn(countQuery, dataQuery);
        when(countQuery.getSingleResult()).thenReturn(1L);
        when(dataQuery.getResultList()).thenReturn(singleResult);

        service.search(request);

        verify(entityManager, times(2)).createNativeQuery(anyString());
    }

    @Test
    void search_withDateRange_appliesRangeFilter() {
        SearchRequest request = new SearchRequest();
        request.setQ("test");
        request.setDiscoveredAtFrom("2026-01-01");
        request.setDiscoveredAtTo("2026-12-31");

        when(entityManager.createNativeQuery(anyString())).thenReturn(countQuery, dataQuery);
        when(countQuery.getSingleResult()).thenReturn(0L);
        when(dataQuery.getResultList()).thenReturn(List.of());

        SearchResponse<VulnDocument> response = service.search(request);

        assertEquals(0, response.getTotal());
        assertTrue(response.getItems().isEmpty());
    }

    @Test
    void search_withPageSizeOver100_clampsTo100() {
        SearchRequest request = new SearchRequest();
        request.setQ("test");
        request.setPageSize(500);

        when(entityManager.createNativeQuery(anyString())).thenReturn(countQuery, dataQuery);
        when(countQuery.getSingleResult()).thenReturn(0L);
        when(dataQuery.getResultList()).thenReturn(List.of());

        SearchResponse<VulnDocument> response = service.search(request);

        assertEquals(100, response.getPageSize());
        assertNotNull(response.getWarnings());
        assertTrue(response.getWarnings().get(0).contains("page_size_clamped"));
    }

    @Test
    void search_withDiscoveredAtSort_appliesSort() {
        SearchRequest request = new SearchRequest();
        request.setQ("test");
        request.setSortBy("discovered_at");
        request.setSortOrder("asc");

        when(entityManager.createNativeQuery(anyString())).thenReturn(countQuery, dataQuery);
        when(countQuery.getSingleResult()).thenReturn(0L);
        when(dataQuery.getResultList()).thenReturn(List.of());

        service.search(request);

        verify(entityManager, times(2)).createNativeQuery(anyString());
    }

    @Test
    void search_withInvalidSortBy_fallsBackToScore() {
        SearchRequest request = new SearchRequest();
        request.setQ("test");
        request.setSortBy("injected; DROP TABLE");

        when(entityManager.createNativeQuery(anyString())).thenReturn(countQuery, dataQuery);
        when(countQuery.getSingleResult()).thenReturn(0L);
        when(dataQuery.getResultList()).thenReturn(List.of());

        assertDoesNotThrow(() -> service.search(request));
    }

    @Test
    void search_withInvalidSortOrder_fallsBackToDesc() {
        SearchRequest request = new SearchRequest();
        request.setQ("test");
        request.setSortOrder("injected; DROP TABLE");

        when(entityManager.createNativeQuery(anyString())).thenReturn(countQuery, dataQuery);
        when(countQuery.getSingleResult()).thenReturn(0L);
        when(dataQuery.getResultList()).thenReturn(List.of());

        assertDoesNotThrow(() -> service.search(request));
    }

    @Test
    void search_withMultipleSeverityValues_expandsAllParams() {
        SearchRequest request = new SearchRequest();
        request.setSeverity(List.of("high", "critical", "medium"));

        when(entityManager.createNativeQuery(anyString())).thenReturn(countQuery, dataQuery);
        when(countQuery.getSingleResult()).thenReturn(0L);
        when(dataQuery.getResultList()).thenReturn(List.of());

        service.search(request);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(entityManager, times(2)).createNativeQuery(sqlCaptor.capture());
        String countSql = sqlCaptor.getAllValues().get(0);
        assertTrue(countSql.contains("IN (?1, ?2, ?3)"));
    }

    @Test
    void search_highlightsOnlyContainEmTags() {
        SearchRequest request = new SearchRequest();
        request.setQ("sql");

        when(entityManager.createNativeQuery(anyString())).thenReturn(countQuery, dataQuery);
        when(countQuery.getSingleResult()).thenReturn(1L);
        when(dataQuery.getResultList()).thenReturn(singleResult);

        SearchResponse<VulnDocument> response = service.search(request);

        Map<String, List<String>> highlights = response.getHighlights();
        assertNotNull(highlights);
        assertTrue(highlights.containsKey("id1:title"));
        assertTrue(highlights.containsKey("id1:description"));
        assertTrue(highlights.get("id1:title").get(0).contains("<em>"));
    }

    @Test
    void search_withEmptyResult_returnsEmptyItems() {
        SearchRequest request = new SearchRequest();
        request.setQ("nonexistent");

        when(entityManager.createNativeQuery(anyString())).thenReturn(countQuery, dataQuery);
        when(countQuery.getSingleResult()).thenReturn(0L);
        when(dataQuery.getResultList()).thenReturn(List.of());

        SearchResponse<VulnDocument> response = service.search(request);

        assertEquals(0, response.getTotal());
        assertTrue(response.getItems().isEmpty());
    }
}
