package com.codesec.worker;

import com.codesec.domain.entity.RepoEntity;
import com.codesec.domain.entity.ScanTaskEntity;
import com.codesec.domain.repository.RepoRepository;
import com.codesec.domain.repository.ScanTaskRepository;
import com.codesec.domain.service.VulnService;
import com.codesec.engine.model.Finding;
import com.codesec.engineadapter.EngineAdapter;
import com.codesec.engineadapter.EngineScanResult;
import com.codesec.engineadapter.ScanRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class ScanQueueConsumerTest {

    private EngineAdapter engineAdapter;
    private VulnService vulnService;
    private ScanTaskRepository scanTaskRepo;
    private TransactionTemplate transactionTemplate;
    private ScanQueueConsumer consumer;

    private static final String TEST_REPO_URL =
        "https://gitee.com/heheshang/ssk_spring_boot_template.git";

    @BeforeEach
    void setUp() {
        engineAdapter = mock(EngineAdapter.class);
        vulnService = mock(VulnService.class);
        scanTaskRepo = mock(ScanTaskRepository.class);
        RepoRepository repoRepo = mock(RepoRepository.class);
        PlatformTransactionManager ptm = mock(PlatformTransactionManager.class);
        when(ptm.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        transactionTemplate = new TransactionTemplate(ptm);

        when(repoRepo.findById(anyLong())).thenReturn(Optional.of(
            RepoEntity.builder().id(1L).url(TEST_REPO_URL).build()
        ));

        consumer = new ScanQueueConsumer(engineAdapter, vulnService, scanTaskRepo,
            repoRepo, transactionTemplate);
    }

    @Test
    void processTask_clonesRepoAndScans_completesSuccessfully() {
        ScanTaskEntity task = new ScanTaskEntity();
        task.setId(1L);
        task.setRepoId(1L);
        task.setBranch("main");
        task.setStatus("queued");

        when(scanTaskRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(engineAdapter.scan(any(ScanRequest.class)))
            .thenReturn(new EngineScanResult("scan-1", List.of(), 50L));

        consumer.processTask(task);

        verify(engineAdapter).scan(any(ScanRequest.class));
        verify(vulnService, never()).persistBatch(any());
        verify(scanTaskRepo, atLeastOnce()).save(task);
    }

    @Test
    void processTask_persistsFindings_whenScanReturnsResults() {
        ScanTaskEntity task = new ScanTaskEntity();
        task.setId(2L);
        task.setRepoId(1L);
        task.setBranch("main");
        task.setStatus("queued");

        when(scanTaskRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        Finding finding = Finding.builder()
            .ruleId("java/sql-injection-001")
            .title("SQL Injection")
            .severity("high")
            .filePath("VulnApp.java")
            .build();
        EngineScanResult result = new EngineScanResult("scan-2", List.of(finding), 100L);

        when(engineAdapter.scan(any(ScanRequest.class))).thenReturn(result);

        consumer.processTask(task);

        // processTask overrides scanId to match the task ID
        verify(vulnService).persistBatch(argThat(list ->
            list.size() == 1 && "2".equals(list.get(0).scanId())
                && "java/sql-injection-001".equals(list.get(0).ruleId())
        ));
        verify(scanTaskRepo, atLeastOnce()).save(task);
    }

    @Test
    void processTask_setsFailed_whenScanThrows() {
        ScanTaskEntity task = new ScanTaskEntity();
        task.setId(3L);
        task.setRepoId(1L);
        task.setBranch("main");
        task.setStatus("queued");

        when(scanTaskRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        when(engineAdapter.scan(any(ScanRequest.class)))
            .thenThrow(new RuntimeException("Engine crashed"));

        consumer.processTask(task);

        verify(scanTaskRepo, atLeastOnce()).save(task);
        verify(vulnService, never()).persistBatch(any());
    }
}
