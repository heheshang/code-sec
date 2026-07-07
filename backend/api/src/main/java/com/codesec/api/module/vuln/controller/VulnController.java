package com.codesec.api.module.vuln.controller;

import com.codesec.common.dto.*;
import com.codesec.common.dto.PaginatedResult;
import com.codesec.domain.service.VulnService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vulns")
@RequiredArgsConstructor
public class VulnController {
    private final VulnService vulnService;

    @GetMapping
    @PreAuthorize("@perm.check('vuln:read')")
    public PaginatedResult<VulnFindingResponse> list(
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String exploitability,
            @RequestParam(required = false) Long projectId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        var query = VulnQuery.builder()
            .severity(severity).exploitability(exploitability)
            .projectId(projectId).page(page).size(size).build();
        return vulnService.list(query);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.check('vuln:read')")
    public VulnFindingResponse get(@PathVariable Long id) {
        return vulnService.getById(id);
    }
}
