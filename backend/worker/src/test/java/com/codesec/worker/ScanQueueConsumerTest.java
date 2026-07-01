package com.codesec.worker;

import com.codesec.api.domain.entity.ScanTaskEntity;
import com.codesec.api.domain.repository.ScanTaskRepository;
import com.codesec.api.infrastructure.queue.InMemoryScanQueue;
import com.codesec.api.module.vuln.VulnService;
import com.codesec.engine.model.Finding;
import com.codesec.engineadapter.EngineAdapter;
import com.codesec.engineadapter.EngineScanResult;
import com.codesec.engineadapter.ScanRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ScanQueueConsumerTest {

    private InMemoryScanQueue queue;
    private EngineAdapter engineAdapter;
    private VulnService vulnService;
    private ScanTaskRepository scanTaskRepo;
    private ScanQueueConsumer consumer;

    @BeforeEach
    void setUp() {
        queue = new InMemoryScanQueue();
        engineAdapter = mock(EngineAdapter.class);
        vulnService = mock(VulnService.class);
        scanTaskRepo = mock(ScanTaskRepository.class);
        consumer = new ScanQueueConsumer(queue, engineAdapter, vulnService, scanTaskRepo);
    }

    @Test
    void processTask_setsRunningThenCompleted() throws Exception {
        ScanTaskEntity task = new ScanTaskEntity();
        task.setId(1L);
        task.setRepoId(42L);
        task.setBranch("main");
        task.setCommitSha("abc123");
        task.setStatus("pending");
        queue.enqueue(task);

        when(engineAdapter.scan(any(ScanRequest.class)))
            .thenReturn(new EngineScanResult("scan-1", List.of(), 50L));

        Thread consumerThread = new Thread(() -> consumer.run());
        consumerThread.setDaemon(true);
        consumerThread.start();
        Thread.sleep(300);
        consumerThread.interrupt();
        verify(scanTaskRepo, atLeastOnce()).save(task);
        verify(engineAdapter).scan(any(ScanRequest.class));
        verify(vulnService, never()).persistBatch(any());
    }

    @Test
    void processTask_persistsFindings_whenScanReturnsResults() throws Exception {
        ScanTaskEntity task = new ScanTaskEntity();
        task.setId(2L);
        task.setRepoId(43L);
        task.setBranch("dev");
        task.setCommitSha("def456");
        task.setStatus("pending");
        queue.enqueue(task);

        Finding finding = Finding.builder()
            .ruleId("java/sql-injection-001")
            .title("SQL Injection")
            .severity("high")
            .filePath("VulnApp.java")
            .build();
        EngineScanResult result = new EngineScanResult("scan-2", List.of(finding), 100L);

        when(engineAdapter.scan(any(ScanRequest.class))).thenReturn(result);

        Thread consumerThread = new Thread(() -> consumer.run());
        consumerThread.setDaemon(true);
        consumerThread.start();
        Thread.sleep(300);
        consumerThread.interrupt();
        verify(vulnService).persistBatch(List.of(finding));
        verify(scanTaskRepo, atLeastOnce()).save(task);
    }

    @Test
    void processTask_setsFailed_whenScanThrows() throws Exception {
        ScanTaskEntity task = new ScanTaskEntity();
        task.setId(3L);
        task.setRepoId(44L);
        task.setBranch("feature/x");
        task.setCommitSha("ghi789");
        task.setStatus("pending");
        queue.enqueue(task);

        when(engineAdapter.scan(any(ScanRequest.class)))
            .thenThrow(new RuntimeException("Engine crashed"));

        Thread consumerThread = new Thread(() -> consumer.run());
        consumerThread.setDaemon(true);
        consumerThread.start();
        Thread.sleep(300);
        consumerThread.interrupt();
        verify(scanTaskRepo, atLeastOnce()).save(task);
        verify(vulnService, never()).persistBatch(any());
    }
}
