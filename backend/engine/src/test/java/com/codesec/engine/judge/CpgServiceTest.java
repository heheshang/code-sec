package com.codesec.engine.judge;

import com.codesec.engine.config.CpgConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class CpgServiceTest {

    private CpgConfiguration memoryConfig;

    @BeforeEach
    void setUp() {
        memoryConfig = new CpgConfiguration("bolt://localhost:7687", "neo4j", "neo4j", false);
    }

    @Test
    void shouldReturnNotAvailableWhenMemoryOnly() {
        CpgService service = new CpgService(memoryConfig);
        assertFalse(service.isAvailable());
    }

    @Test
    void importGraphShouldNotThrowWhenMemoryOnly() {
        CpgService service = new CpgService(memoryConfig);
        CallGraphBuilder builder = new CallGraphBuilder();
        ProjectCallGraph graph = builder.build(new ArrayList<>());

        assertDoesNotThrow(() -> {
            service.importGraph(graph, "test-scan-1");
        });
    }

    @Test
    void findLatestByProjectIdShouldReturnEmptyWhenMemoryOnly() {
        CpgService service = new CpgService(memoryConfig);
        assertTrue(service.findLatestByProjectId("test-scan-1").isEmpty());
    }

    @Test
    void findReachablePathsShouldReturnEmptyWhenMemoryOnly() {
        CpgService service = new CpgService(memoryConfig);
        assertTrue(service.findReachablePaths("test-scan-1").isEmpty());
    }

    @Test
    void clearScanShouldNotThrowWhenMemoryOnly() {
        CpgService service = new CpgService(memoryConfig);
        assertDoesNotThrow(() -> service.clearScan("test-scan-1"));
    }

    @Test
    void closeShouldNotThrow() {
        CpgService service = new CpgService(memoryConfig);
        assertDoesNotThrow(() -> service.close());
    }
}
