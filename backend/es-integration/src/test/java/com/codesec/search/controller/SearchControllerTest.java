package com.codesec.search.controller;

import com.codesec.search.dto.SearchRequest;
import com.codesec.search.dto.SearchResponse;
import com.codesec.search.repository.SnippetDocument;
import com.codesec.search.repository.VulnDocument;
import com.codesec.search.service.SnippetSearchService;
import com.codesec.search.service.VulnSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller test for SearchController.
 * Uses standalone MockMvc setup (no Spring context) to avoid
 * Mockito/JDK 25 incompatibility with inline mock maker.
 */
class SearchControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        VulnSearchService vulnStub = new VulnSearchService(null, null, null) {
            @Override
            public SearchResponse<VulnDocument> search(SearchRequest request) {
                if ("zzz_nonexistent".equals(request.getQ())) {
                    return SearchResponse.<VulnDocument>builder()
                            .total(0).page(1).pageSize(20).tookMs(2)
                            .items(List.of()).build();
                }
                return SearchResponse.<VulnDocument>builder()
                        .total(1).page(1).pageSize(20).tookMs(5)
                        .items(List.of(VulnDocument.builder()
                                .id("1").projectId("proj-a").severity("critical")
                                .title("SQL Injection").build()))
                        .build();
            }
        };

        SnippetSearchService snippetStub = new SnippetSearchService(null, null, null) {
            @Override
            public SearchResponse<SnippetDocument> search(SearchRequest request) {
                return SearchResponse.<SnippetDocument>builder()
                        .total(1).page(1).pageSize(20).tookMs(3)
                        .items(List.of(SnippetDocument.builder()
                                .filePath("src/main/java/Login.java")
                                .projectId("proj-b").language("java").build()))
                        .build();
            }
        };

        SearchController controller = new SearchController(vulnStub, snippetStub);
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/search/vulns — search by keyword")
    void searchVulnsByKeyword() throws Exception {
        mockMvc.perform(get("/api/v1/search/vulns")
                        .param("q", "SQL Injection")
                        .param("page", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value("1"));
    }

    @Test
    @DisplayName("GET /api/v1/search/vulns — filter by severity")
    void searchVulnsBySeverity() throws Exception {
        mockMvc.perform(get("/api/v1/search/vulns")
                        .param("severity", "critical")
                        .param("page", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].severity").value("critical"));
    }

    @Test
    @DisplayName("GET /api/v1/search/vulns — return empty results")
    void searchVulnsEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/search/vulns")
                        .param("q", "zzz_nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/search/snippets — search by file_path prefix")
    void searchSnippetsByFilePath() throws Exception {
        mockMvc.perform(get("/api/v1/search/snippets")
                        .param("q", "src/main")
                        .param("page", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].file_path").value("src/main/java/Login.java"));
    }

    @Test
    @DisplayName("GET /api/v1/search/snippets — filter by project_id")
    void searchSnippetsByProjectId() throws Exception {
        mockMvc.perform(get("/api/v1/search/snippets")
                        .param("projectId", "proj-b")
                        .param("page", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].project_id").value("proj-b"));
    }
}
