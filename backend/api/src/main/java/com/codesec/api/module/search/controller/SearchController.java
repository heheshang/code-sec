package com.codesec.api.module.search.controller;

import com.codesec.domain.dto.SearchRequest;
import com.codesec.domain.dto.SearchResponse;
import com.codesec.domain.model.SnippetDocument;
import com.codesec.domain.model.VulnDocument;
import com.codesec.domain.service.search.SnippetSearchService;
import com.codesec.domain.service.search.VulnSearchService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/vulns")
    @PreAuthorize("@perm.check('vuln:read')")
    public ResponseEntity<SearchResponse<VulnDocument>> searchVulns(@Valid SearchRequest request) {
        log.info("REST vuln search: q='{}', severity={}, page={}/{}",
                request.getQ(), request.getSeverity(), request.getPage(), request.getPageSize());
        SearchResponse<VulnDocument> result = vulnSearchService.search(request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/snippets")
    @PreAuthorize("@perm.check('vuln:read')")
    public ResponseEntity<SearchResponse<SnippetDocument>> searchSnippets(@Valid SearchRequest request) {
        log.info("REST snippet search: q='{}', project_id={}", request.getQ(), request.getProjectId());
        SearchResponse<SnippetDocument> result = snippetSearchService.search(request);
        return ResponseEntity.ok(result);
    }
}
