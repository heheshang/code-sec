package com.codesec.codex.benchmark;

import com.codesec.codex.client.CodexClient;
import com.codesec.codex.config.CodexProperties;
import com.codesec.codex.prompt.PromptLoader;
import com.codesec.codex.prompt.PromptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FalsePositiveBenchmarkTest {

    private CodexClient mockClient;
    private PromptRepository repo;
    private CodexProperties props;
    private List<FalsePositiveSample> samples;

    @BeforeEach
    void setUp() {
        mockClient = mock(CodexClient.class);
        when(mockClient.execute(any(), anyString(), anyString())).thenReturn("{\"is_true_positive\": true, \"confidence\": 0.9}");
        PromptLoader loader = new PromptLoader();
        repo = new PromptRepository(loader);
        props = new CodexProperties();

        samples = List.of(
            createSample("tp-001", true),
            createSample("fp-001", false)
        );
    }

    @Test
    void runReturnsBenchmarkResult() {
        FalsePositiveBenchmark b = new FalsePositiveBenchmark(mockClient, repo, props, samples);
        BenchmarkResult result = b.run();
        assertNotNull(result);
        assertEquals("FalsePositiveFilter", result.getName());
        assertTrue(result.getSampleCount() > 0);
    }

    @Test
    void runProducesValidConfusionMatrix() {
        FalsePositiveBenchmark b = new FalsePositiveBenchmark(mockClient, repo, props, samples);
        BenchmarkResult result = b.run();
        int[][] cm = result.getConfusionMatrix();
        assertEquals(2, cm.length);
        assertEquals(2, cm[0].length);
    }

    @Test
    void runHandlesCodexClientFailure() {
        when(mockClient.execute(any(), anyString(), anyString())).thenThrow(new RuntimeException("API down"));
        FalsePositiveBenchmark b = new FalsePositiveBenchmark(mockClient, repo, props, samples);
        BenchmarkResult result = b.run();
        assertNotNull(result);
        assertTrue(result.getSampleCount() > 0);
    }

    @Test
    void runHandlesEmptySamples() {
        FalsePositiveBenchmark b = new FalsePositiveBenchmark(mockClient, repo, props, List.of());
        BenchmarkResult result = b.run();
        assertEquals(0, result.getSampleCount());
    }

    private FalsePositiveSample createSample(String id, boolean truePositive) {
        FalsePositiveSample s = new FalsePositiveSample();
        s.setId(id);
        s.setRuleId("test-rule");
        s.setTitle("Test Finding");
        s.setSeverity("HIGH");
        s.setLanguage("java");
        s.setCode("test code");
        s.setCallChain("test()");
        s.setDataSource("input");
        s.setReachable(true);
        s.setFrameworkProtection("none");
        s.setExpectedVerdict(truePositive ? "true_positive" : "false_positive");
        s.setConfidence(truePositive ? 0.9 : 0.8);
        return s;
    }
}
