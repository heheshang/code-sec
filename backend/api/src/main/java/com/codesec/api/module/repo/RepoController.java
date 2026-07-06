package com.codesec.api.module.repo;

import com.codesec.domain.dto.*;
import com.codesec.domain.dto.PaginatedResult;
import com.codesec.domain.service.repo.RepoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/repos")
@RequiredArgsConstructor
public class RepoController {
    private final RepoService repoService;

    @PostMapping
    @PreAuthorize("@perm.check('repo:create')")
    @ResponseStatus(HttpStatus.CREATED)
    public RepoResponse create(@RequestBody RepoCreateRequest req) {
        return repoService.create(req);
    }

    @GetMapping
    @PreAuthorize("@perm.check('repo:read')")
    public PaginatedResult<RepoListItem> list(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return repoService.list(page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.check('repo:read')")
    public RepoResponse get(@PathVariable Long id) {
        return repoService.getById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@perm.check('repo:update')")
    public RepoResponse update(@PathVariable Long id, @RequestBody RepoUpdateRequest req) {
        return repoService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@perm.check('repo:delete')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        repoService.delete(id);
    }

    @PostMapping("/{id}/test-connection")
    @PreAuthorize("@perm.check('repo:read')")
    public TestConnectionResponse testConnection(@PathVariable Long id) {
        return repoService.testConnection(id);
    }
}
