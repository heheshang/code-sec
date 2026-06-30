package com.codesec.search.controller;

import com.codesec.search.dto.SearchRequest;
import com.codesec.search.dto.SearchResponse;
import com.codesec.search.repository.SnippetDocument;
import com.codesec.search.repository.VulnDocument;
import com.codesec.search.service.SnippetSearchService;
import com.codesec.search.service.VulnSearchService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for ES full-text search.
 * GET /api/v1/search/vulns — vuln search with highlight
 * GET /api/v1/search/snippets — file_snippet prefix search (v1)
 *
 * RBAC: @PreAuthorize("hasAuthority('vuln:read')") — wired in E-S2-CRITICAL Task 5.
 */
@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private static final Logger log = LoggerFactory.getLogger(SearchController.class);

    private final VulnSearchService vulnSearchService;
    private final SnippetSearchService snippetSearchService;

    public SearchController(VulnSearchService vulnSearchService,
                            SnippetSearchService snippetSearchService) {
        this.vulnSearchService = vulnSearchService;
        this.snippetSearchService = snippetSearchService;
    }

    /**
     * Search vulnerabilities — multi-field full-text query + filters + highlight.
     * GET /api/v1/search/vulns?q=sql+injection&severity=critical&page=1&page_size=20
     */
    @GetMapping("/vulns")
    public ResponseEntity<SearchResponse<VulnDocument>> searchVulns(@Valid SearchRequest request) {
        log.info("REST vuln search: q='{}', severity={}, page={}/{}",
                request.getQ(), request.getSeverity(), request.getPage(), request.getPageSize());
        SearchResponse<VulnDocument> result = vulnSearchService.search(request);
        return ResponseEntity.ok(result);
    }

    /**
     * Search code snippets — v1: file_path prefix match only.
     * GET /api/v1/search/snippets?q=src/main&language=java
     */
    @GetMapping("/snippets")
    public ResponseEntity<SearchResponse<SnippetDocument>> searchSnippets(@Valid SearchRequest request) {
        log.info("REST snippet search: q='{}', project_id={}", request.getQ(), request.getProjectId());
        SearchResponse<SnippetDocument> result = snippetSearchService.search(request);
        return ResponseEntity.ok(result);
    }
}
