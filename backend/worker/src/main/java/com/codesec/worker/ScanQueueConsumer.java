package com.codesec.worker;

import com.codesec.engineadapter.EngineAdapter;
import com.codesec.engineadapter.ScanRequest;
import com.codesec.api.infrastructure.queue.InMemoryScanQueue;
import com.codesec.api.module.vuln.VulnService;
import com.codesec.api.domain.entity.ScanTaskEntity;
import com.codesec.api.domain.repository.ScanTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.nio.file.Path;
import java.util.UUID;

@Component
public class ScanQueueConsumer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(ScanQueueConsumer.class);
    private final InMemoryScanQueue queue;
    private final EngineAdapter engineAdapter;
    private final VulnService vulnService;
    private final ScanTaskRepository scanTaskRepo;

    public ScanQueueConsumer(InMemoryScanQueue queue, EngineAdapter engineAdapter,
                              VulnService vulnService, ScanTaskRepository scanTaskRepo) {
        this.queue = queue;
        this.engineAdapter = engineAdapter;
        this.vulnService = vulnService;
        this.scanTaskRepo = scanTaskRepo;
    }

    @Override
    public void run(String... args) {
        Thread consumer = new Thread(() -> {
            log.info("ScanQueueConsumer started, waiting for tasks...");
            while (true) {
                try {
                    ScanTaskEntity task = queue.dequeue();
                    processTask(task);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("Unexpected error in consumer", e);
                }
            }
        }, "scan-queue-consumer");
        consumer.setDaemon(true);
        consumer.start();
    }

    private void processTask(ScanTaskEntity task) {
        log.info("Processing scan task: id={}, repoId={}, branch={}", task.getId(), task.getRepoId(), task.getBranch());
        task.setStatus("running");
        task.setStartedAt(LocalDateTime.now());
        scanTaskRepo.save(task);

        try {
            // In v1, we scan a temp directory or pre-cloned directory
            Path sourceRoot = Path.of(System.getProperty("java.io.tmpdir"), "codesec-scan-" + task.getId());
            java.nio.file.Files.createDirectories(sourceRoot);

            var request = com.codesec.engineadapter.ScanRequest.of(
                task.getRepoId(), sourceRoot, task.getCommitSha());

            var result = engineAdapter.scan(request);

            if (!result.findings().isEmpty()) {
                vulnService.persistBatch(result.findings());
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
        }
    }
}
