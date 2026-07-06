package com.codesec.worker;

import com.codesec.engine.model.Finding;
import com.codesec.engineadapter.EngineAdapter;
import com.codesec.engineadapter.ScanRequest;
import com.codesec.domain.entity.RepoEntity;
import com.codesec.domain.entity.ScanTaskEntity;
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

@Component
public class ScanQueueConsumer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(ScanQueueConsumer.class);
    private static final long POLL_INTERVAL_MS = 2000;

    private final EngineAdapter engineAdapter;
    private final VulnService vulnService;
    private final ScanTaskRepository scanTaskRepo;
    private final RepoRepository repoRepo;
    private final TransactionTemplate transactionTemplate;

    public ScanQueueConsumer(EngineAdapter engineAdapter,
                              VulnService vulnService, ScanTaskRepository scanTaskRepo,
                              RepoRepository repoRepo,
                              TransactionTemplate transactionTemplate) {
        this.engineAdapter = engineAdapter;
        this.vulnService = vulnService;
        this.scanTaskRepo = scanTaskRepo;
        this.repoRepo = repoRepo;
        this.transactionTemplate = transactionTemplate;
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
            log.info("Cloning repo: {}", repo.getUrl());

            sourceRoot = Path.of(System.getProperty("java.io.tmpdir"), "codesec-scan-" + task.getId());
            cloneRepo(repo.getUrl(), task.getBranch() != null ? task.getBranch() : "main", sourceRoot);

            var request = ScanRequest.of(
                task.getRepoId(), sourceRoot, task.getCommitSha());

            var result = engineAdapter.scan(request);

            if (!result.findings().isEmpty()) {
                // Override scanId to the actual task ID before persisting
                java.util.List<Finding> findings = result.findings().stream()
                    .map(f -> Finding.builder()
                        .vulnId(f.vulnId())
                        .projectId(f.projectId())
                        .scanId(String.valueOf(task.getId()))
                        .engine(f.engine())
                        .ruleId(f.ruleId())
                        .title(f.title())
                        .severity(f.severity())
                        .filePath(f.filePath())
                        .lineStart(f.lineStart())
                        .lineEnd(f.lineEnd())
                        .codeSnippet(f.codeSnippet())
                        .description(f.description())
                        .fixSuggestion(f.fixSuggestion())
                        .cwe(f.cwe())
                        .cve(f.cve())
                        .exploitability(f.exploitability())
                        .exploitReason(f.exploitReason())
                        .engineRaw(f.engineRaw())
                        .discoveredAt(f.discoveredAt())
                        .build())
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

    void cloneRepo(String url, String branch, Path target) throws Exception {
        if (target.toFile().exists()) {
            deleteRecursively(target);
        }
        Files.createDirectories(target.getParent());

        ProcessBuilder pb = new ProcessBuilder(
            "git", "clone", "--depth=1", "--branch=" + branch, url, target.toString());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        int exitCode = p.waitFor();
        if (exitCode != 0) {
            String output = new String(p.getInputStream().readAllBytes());
            throw new RuntimeException("Git clone failed (exit=" + exitCode + "): " + output);
        }
        log.info("Repo cloned to {}", target);
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
