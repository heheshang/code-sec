package com.codesec.api.module.scan;

import com.codesec.domain.dto.*;
import com.codesec.domain.dto.PaginatedResult;
import com.codesec.domain.service.scan.ScanService;
import com.codesec.api.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/scans")
@RequiredArgsConstructor
public class ScanController {
    private final ScanService scanService;

    @PostMapping
    @PreAuthorize("@perm.check('scan:create')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ScanResponse create(@RequestBody ScanCreateRequest req, @AuthenticationPrincipal UserPrincipal user) {
        return scanService.create(req, user.getUserId());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.check('scan:read')")
    public ScanTaskResponse get(@PathVariable Long id) {
        return scanService.getById(id);
    }

    @GetMapping
    @PreAuthorize("@perm.check('scan:read')")
    public PaginatedResult<ScanListItem> list(@RequestParam(required = false) Long repoId,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return scanService.list(repoId, page, size);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.check('scan:cancel')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long id) {
        scanService.cancel(id);
    }
}
