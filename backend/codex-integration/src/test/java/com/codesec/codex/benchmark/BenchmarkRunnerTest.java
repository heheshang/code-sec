package com.codesec.codex.benchmark;

import com.codesec.codex.client.CodexClient;
import com.codesec.codex.config.CodexProperties;
import com.codesec.codex.prompt.PromptLoader;
import com.codesec.codex.prompt.PromptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BenchmarkRunnerTest {

    private CodexClient mockCodeClient;
    private CodexClient mockLlmClient;
    private PromptRepository repo;
    private CodexProperties props;

    @BeforeEach
    void setUp() {
        mockCodeClient = mock(CodexClient.class);
        mockLlmClient = mock(CodexClient.class);
        when(mockCodeClient.execute(any(), anyString(), anyString())).thenReturn("SQL Injection detected");
        when(mockLlmClient.execute(any(), anyString(), anyString())).thenReturn("{\"is_true_positive\": true}");
        PromptLoader loader = new PromptLoader();
        repo = new PromptRepository(loader);
        props = new CodexProperties();
    }

    @Test
    void runAllReturnsSummaryWithTwoResults() {
        BenchmarkRunner runner = new BenchmarkRunner(mockCodeClient, mockLlmClient, repo, props);
        BenchmarkSummary summary = runner.runAll();
        assertNotNull(summary);
        assertEquals(2, summary.getResults().size());
    }

    @Test
    void runAllResultsHaveValidMetrics() {
        BenchmarkRunner runner = new BenchmarkRunner(mockCodeClient, mockLlmClient, repo, props);
        BenchmarkSummary summary = runner.runAll();
        for (BenchmarkResult r : summary.getResults()) {
            assertNotNull(r.getName());
            assertTrue(r.getSampleCount() > 0);
            assertTrue(r.getDurationMs() >= 0);
            assertNotNull(r.getConfusionMatrix());
        }
    }
}
