package com.codesec.domain.service.search;

import com.codesec.common.dto.SearchRequest;
import com.codesec.common.dto.SearchResponse;
import com.codesec.domain.model.SnippetDocument;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SnippetSearchServiceTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query countQuery;

    @Mock
    private Query dataQuery;

    private SnippetSearchService service;

    @BeforeEach
    void setUp() {
        service = new SnippetSearchService();
        ReflectionTestUtils.setField(service, "entityManager", entityManager);
    }

    private List<Object[]> snippetRows(String filePath, String projectId, String language, String indexedAt) {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{filePath, projectId, language, indexedAt});
        return rows;
    }

    @Test
    void search_withQuery_returnsMatchingFilePaths() {
        SearchRequest request = new SearchRequest();
        request.setQ("src/main");
        request.setPage(1);
        request.setPageSize(20);

        when(entityManager.createNativeQuery(anyString())).thenReturn(countQuery, dataQuery);
        when(countQuery.getSingleResult()).thenReturn(2L);
        when(dataQuery.getResultList()).thenReturn(snippetRows("src/main/java/App.java", "proj1", "java", "2026-01-01T00:00:00Z"));

        SearchResponse<SnippetDocument> response = service.search(request);

        assertEquals(2, response.getTotal());
        assertEquals(1, response.getItems().size());
        assertEquals("src/main/java/App.java", response.getItems().get(0).getFilePath());
        assertEquals("java", response.getItems().get(0).getLanguage());
        assertNotNull(response.getItems().get(0).getIndexedAt());
    }

    @Test
    void search_withoutQueryAndProjectId_throwsException() {
        SearchRequest request = new SearchRequest();
        request.setQ(null);
        assertThrows(IllegalArgumentException.class, () -> service.search(request));
    }

    @Test
    void search_withProjectIdFilter_returnsResults() {
        SearchRequest request = new SearchRequest();
        request.setQ(null);
        request.setProjectId(List.of("proj1"));

        when(entityManager.createNativeQuery(anyString())).thenReturn(countQuery, dataQuery);
        when(countQuery.getSingleResult()).thenReturn(1L);
        when(dataQuery.getResultList()).thenReturn(snippetRows("src/main/java/App.java", "proj1", "java", "2026-01-01T00:00:00Z"));

        SearchResponse<SnippetDocument> response = service.search(request);

        assertEquals(1, response.getTotal());
    }

    @Test
    void search_escapesLikeWildcards() {
        SearchRequest request = new SearchRequest();
        request.setQ("100%_complete");

        when(entityManager.createNativeQuery(anyString())).thenReturn(countQuery, dataQuery);
        when(countQuery.getSingleResult()).thenReturn(0L);
        when(dataQuery.getResultList()).thenReturn(List.of());

        service.search(request);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(entityManager, times(2)).createNativeQuery(sqlCaptor.capture());
        String countSql = sqlCaptor.getAllValues().get(0);
        assertTrue(countSql.contains("ESCAPE"));
    }

    @Test
    void search_infersLanguageFromFileExtension() {
        SearchRequest request = new SearchRequest();
        request.setQ("/app");

        when(entityManager.createNativeQuery(anyString())).thenReturn(countQuery, dataQuery);
        when(countQuery.getSingleResult()).thenReturn(1L);
        when(dataQuery.getResultList()).thenReturn(snippetRows("app/Service.java", "proj1", "java", "2026-01-01T00:00:00Z"));

        SearchResponse<SnippetDocument> response = service.search(request);

        assertEquals("java", response.getItems().get(0).getLanguage());
    }

    @Test
    void search_unknownExtension_returnsUnknownLanguage() {
        SearchRequest request = new SearchRequest();
        request.setQ("/app");

        when(entityManager.createNativeQuery(anyString())).thenReturn(countQuery, dataQuery);
        when(countQuery.getSingleResult()).thenReturn(1L);
        when(dataQuery.getResultList()).thenReturn(snippetRows("app/file.xyz", "proj1", "unknown", "2026-01-01T00:00:00Z"));

        SearchResponse<SnippetDocument> response = service.search(request);

        assertEquals("unknown", response.getItems().get(0).getLanguage());
    }

    @Test
    void search_withPageSizeOver100_clampsTo100() {
        SearchRequest request = new SearchRequest();
        request.setQ("src");
        request.setPageSize(500);

        when(entityManager.createNativeQuery(anyString())).thenReturn(countQuery, dataQuery);
        when(countQuery.getSingleResult()).thenReturn(0L);
        when(dataQuery.getResultList()).thenReturn(List.of());

        SearchResponse<SnippetDocument> response = service.search(request);

        assertEquals(100, response.getPageSize());
        assertNotNull(response.getWarnings());
    }

    @Test
    void search_withEmptyResult_returnsEmptyItems() {
        SearchRequest request = new SearchRequest();
        request.setQ("nonexistent");

        when(entityManager.createNativeQuery(anyString())).thenReturn(countQuery, dataQuery);
        when(countQuery.getSingleResult()).thenReturn(0L);
        when(dataQuery.getResultList()).thenReturn(List.of());

        SearchResponse<SnippetDocument> response = service.search(request);

        assertEquals(0, response.getTotal());
        assertTrue(response.getItems().isEmpty());
    }
}
