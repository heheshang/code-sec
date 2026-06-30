package com.codesec.api.module.repo;

import com.codesec.api.domain.entity.*;
import com.codesec.api.domain.repository.RepoRepository;
import com.codesec.api.module.repo.dto.*;
import com.codesec.api.interfaces.dto.PaginatedResult;
import com.codesec.common.crypto.CryptoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RepoService {
    private final RepoRepository repoRepo;
    private final CryptoService cryptoService;

    public RepoResponse create(RepoCreateRequest req) {
        RepoEntity entity = RepoEntity.builder()
            .name(req.getName())
            .platform(req.getPlatform())
            .url(req.getUrl())
            .defaultBranch(req.getDefaultBranch())
            .businessLine(req.getBusinessLine())
            .gitlabProjectId(req.getGitlabProjectId())
            .accessTokenEncrypted(req.getAccessToken() != null ? cryptoService.encrypt(req.getAccessToken()) : "")
            .webhookSecret(req.getWebhookSecret() != null ? cryptoService.encrypt(req.getWebhookSecret()) : "")
            .status("active")
            .build();
        return toResponse(repoRepo.save(entity));
    }

    public RepoResponse getById(Long id) {
        return toResponse(findEntityOrThrow(id));
    }

    public PaginatedResult<RepoListItem> list(int page, int size) {
        var p = repoRepo.findAll(PageRequest.of(page - 1, size));
        var items = p.getContent().stream().map(this::toListItem).toList();
        return PaginatedResult.of(items, p.getTotalElements(), page, size);
    }

    public RepoResponse update(Long id, RepoUpdateRequest req) {
        RepoEntity entity = findEntityOrThrow(id);
        if (req.getName() != null) entity.setName(req.getName());
        if (req.getUrl() != null) entity.setUrl(req.getUrl());
        if (req.getAccessToken() != null) entity.setAccessTokenEncrypted(cryptoService.encrypt(req.getAccessToken()));
        if (req.getDefaultBranch() != null) entity.setDefaultBranch(req.getDefaultBranch());
        if (req.getBusinessLine() != null) entity.setBusinessLine(req.getBusinessLine());
        if (req.getStatus() != null) entity.setStatus(req.getStatus());
        return toResponse(repoRepo.save(entity));
    }

    public void delete(Long id) {
        repoRepo.deleteById(id);
    }

    public TestConnectionResponse testConnection(Long id) {
        RepoEntity repo = findEntityOrThrow(id);
        // v1: mock GitLab connection
        return TestConnectionResponse.builder()
            .ok(true)
            .branches(List.of("main", "develop", "feature/test"))
            .build();
    }

    /**
     * E-S2-002 cross-Epic interface: find repo by GitLab project_id for webhook entry.
     */
    @Transactional(readOnly = true)
    public Optional<RepoEntity> findByGitlabProjectId(Long gitlabProjectId) {
        return repoRepo.findByGitlabProjectId(gitlabProjectId);
    }

    /**
     * E-S2-002 cross-Epic interface: shallow clone + checkout MR head SHA.
     * v1: simplified mock - returns temp dir path.
     */
    @Transactional(readOnly = false)
    public Path checkoutMrHead(Long repoId, String commitSha) {
        RepoEntity repo = findEntityOrThrow(repoId);
        try {
            Path workDir = Path.of(System.getProperty("java.io.tmpdir"), "codesec-checkout-" + repoId + "-" + commitSha);
            java.nio.file.Files.createDirectories(workDir);
            // In sprint 2, we use existing workspace or temp dir
            return workDir;
        } catch (Exception e) {
            throw new RuntimeException("Checkout failed for repo " + repoId, e);
        }
    }

    private RepoResponse toResponse(RepoEntity e) {
        return RepoResponse.builder()
            .id(e.getId()).name(e.getName()).platform(e.getPlatform())
            .url(e.getUrl()).defaultBranch(e.getDefaultBranch())
            .businessLine(e.getBusinessLine()).status(e.getStatus())
            .gitlabProjectId(e.getGitlabProjectId())
            .hasToken(e.getAccessTokenEncrypted() != null && !e.getAccessTokenEncrypted().isEmpty())
            .createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt())
            .build();
    }

    private RepoEntity findEntityOrThrow(Long id) {
        return repoRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Repo not found: " + id));
    }

    private RepoListItem toListItem(RepoEntity e) {
        return RepoListItem.builder()
            .id(e.getId()).name(e.getName()).platform(e.getPlatform())
            .url(e.getUrl()).businessLine(e.getBusinessLine()).status(e.getStatus())
            .createdAt(e.getCreatedAt()).build();
    }
}
