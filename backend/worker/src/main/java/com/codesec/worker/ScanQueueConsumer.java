package com.codesec.worker;

import com.codesec.common.dto.FindingDto;
import com.codesec.engineadapter.EngineAdapter;
import com.codesec.engineadapter.ScanRequest;
import com.codesec.domain.entity.RepoEntity;
import com.codesec.domain.entity.ScanTaskEntity;
import com.codesec.common.crypto.CryptoService;
import com.codesec.domain.repository.RepoRepository;
import com.codesec.domain.repository.ScanTaskRepository;
import com.codesec.domain.service.VulnService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

@Component
public class ScanQueueConsumer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(ScanQueueConsumer.class);
    private static final long POLL_INTERVAL_MS = 2000;

    private final EngineAdapter engineAdapter;
    private final VulnService vulnService;
    private final ScanTaskRepository scanTaskRepo;
    private final RepoRepository repoRepo;
    private final TransactionTemplate transactionTemplate;
    private final CryptoService cryptoService;

    public ScanQueueConsumer(EngineAdapter engineAdapter,
                              VulnService vulnService, ScanTaskRepository scanTaskRepo,
                              RepoRepository repoRepo,
                              TransactionTemplate transactionTemplate,
                              CryptoService cryptoService) {
        this.engineAdapter = engineAdapter;
        this.vulnService = vulnService;
        this.scanTaskRepo = scanTaskRepo;
        this.repoRepo = repoRepo;
        this.transactionTemplate = transactionTemplate;
        this.cryptoService = cryptoService;
    }

    @Override
    public void run(String... args) {
        Thread consumer = new Thread(() -> {
            log.info("ScanQueueConsumer started, polling DB for queued tasks...");
            while (true) {
                try {
                    Optional<ScanTaskEntity> task = claimNextTask();
                    if (task.isPresent()) {
                        processTask(task.get());
                    } else {
                        Thread.sleep(POLL_INTERVAL_MS);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("Unexpected error in consumer", e);
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }, "scan-queue-consumer");
        consumer.setDaemon(true);
        consumer.start();
    }

    Optional<ScanTaskEntity> claimNextTask() {
        return transactionTemplate.execute(status -> {
            var page = scanTaskRepo.findNextQueuedTask(PageRequest.of(0, 1));
            if (page.hasContent()) {
                ScanTaskEntity t = page.getContent().get(0);
                t.setStatus("running");
                t.setStartedAt(LocalDateTime.now());
                scanTaskRepo.save(t);
                log.info("Claimed scan task: id={}, repoId={}", t.getId(), t.getRepoId());
                return Optional.of(t);
            }
            return Optional.empty();
        });
    }

    void processTask(ScanTaskEntity task) {
        log.info("Processing scan task: id={}, repoId={}, branch={}", task.getId(), task.getRepoId(), task.getBranch());
        Path sourceRoot = null;

        try {
            RepoEntity repo = repoRepo.findById(task.getRepoId())
                .orElseThrow(() -> new RuntimeException("Repo not found: " + task.getRepoId()));
            log.info("Cloning repo: {} (branch={}, commitSha={})", repo.getUrl(), task.getBranch(), task.getCommitSha());

            sourceRoot = Path.of(System.getProperty("java.io.tmpdir"), "codesec-scan-" + task.getId());
            String token = decryptToken(repo);
            cloneRepo(repo.getUrl(), task.getBranch() != null ? task.getBranch() : "main", sourceRoot, token);

            // Checkout specific commit if provided and not HEAD
            String commitSha = task.getCommitSha();
            if (commitSha != null && !commitSha.isBlank()) {
                checkoutCommit(sourceRoot, commitSha, token);
            }

            var request = ScanRequest.of(
                task.getRepoId(), sourceRoot, task.getCommitSha());

            var result = engineAdapter.scan(request);

            if (!result.findings().isEmpty()) {
                // Stamp scanId with the task ID, preserving all other fields (including AI)
                List<FindingDto> findings = result.findings().stream()
                    .map(f -> f.withScanId(String.valueOf(task.getId())))
                    .toList();
                vulnService.persistBatch(findings);
            }

            task.setStatus("completed");
            task.setFinishedAt(LocalDateTime.now());
            scanTaskRepo.save(task);
            log.info("Scan completed: id={}, findings={}, duration={}ms",
                task.getId(), result.findings().size(), result.durationMs());
        } catch (Exception e) {
            log.error("Scan failed: id={}", task.getId(), e);
            task.setStatus("failed");
            task.setErrorMessage(e.getMessage());
            task.setFinishedAt(LocalDateTime.now());
            scanTaskRepo.save(task);
        } finally {
            if (sourceRoot != null) {
                try {
                    deleteRecursively(sourceRoot);
                } catch (Exception e) {
                    log.warn("Failed to clean up temp dir: {}", sourceRoot, e);
                }
            }
        }
    }

    void cloneRepo(String url, String branch, Path target, String token) throws Exception {
        if (target.toFile().exists()) {
            deleteRecursively(target);
        }
        Files.createDirectories(target.getParent());

        // Inject token via -c http.extraHeader for authenticated clone
        // This avoids leaving credentials in git config or URL refs
        ProcessBuilder pb = new ProcessBuilder(
            "git", "-c", "http.extraHeader=Authorization: Bearer " + (token != null ? token : "anonymous"),
            "clone", "--depth=1", "--branch=" + branch, url, target.toString());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try {
            int exitCode = p.waitFor();
            if (exitCode != 0) {
                String output = new String(p.getInputStream().readAllBytes());
                throw new RuntimeException("Git clone failed (exit=" + exitCode + "): " + output);
            }
            log.info("Repo cloned to {}", target);
        } finally {
            p.destroyForcibly();
        }
    }

    void checkoutCommit(Path repoDir, String commitSha, String token) throws Exception {
        Process fetchProc = null;
        Process checkoutProc = null;
        try {
            // Try shallow fetch of the specific SHA first
            ProcessBuilder fetchPb = new ProcessBuilder(
                "git", "-c", "http.extraHeader=Authorization: Bearer " + (token != null ? token : "anonymous"),
                "fetch", "--depth=1", "origin", commitSha);
            fetchPb.directory(repoDir.toFile());
            fetchPb.redirectErrorStream(true);
            fetchProc = fetchPb.start();
            int fetchExit = fetchProc.waitFor();
            if (fetchExit != 0) {
                // SHA not available for shallow fetch; log and skip (scan proceeds at branch HEAD)
                String output = new String(fetchProc.getInputStream().readAllBytes());
                log.warn("Shallow fetch of commit {} failed (exit={}): {}. Scanning branch HEAD instead.", commitSha, fetchExit, output.trim());
                return;
            }

            ProcessBuilder checkoutPb = new ProcessBuilder("git", "checkout", commitSha);
            checkoutPb.directory(repoDir.toFile());
            checkoutPb.redirectErrorStream(true);
            checkoutProc = checkoutPb.start();
            int checkoutExit = checkoutProc.waitFor();
            if (checkoutExit != 0) {
                String output = new String(checkoutProc.getInputStream().readAllBytes());
                log.warn("Checkout of commit {} failed (exit={}): {}. Scanning branch HEAD instead.", commitSha, checkoutExit, output.trim());
            } else {
                log.info("Checked out commit {}", commitSha);
            }
        } finally {
            if (fetchProc != null) fetchProc.destroyForcibly();
            if (checkoutProc != null) checkoutProc.destroyForcibly();
        }
    }

    private String decryptToken(RepoEntity repo) {
        String encrypted = repo.getAccessTokenEncrypted();
        if (encrypted == null || encrypted.isBlank()) {
            log.debug("No access token configured for repo {}, cloning without auth", repo.getId());
            return null;
        }
        try {
            return cryptoService.decrypt(encrypted);
        } catch (Exception e) {
            log.warn("Failed to decrypt access token for repo {}: {}", repo.getId(), e.getMessage());
            return null;
        }
    }

    private void deleteRecursively(Path path) throws Exception {
        if (path.toFile().exists()) {
            try (var files = java.nio.file.Files.walk(path)) {
                files.sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> p.toFile().delete());
            }
        }
    }
}
