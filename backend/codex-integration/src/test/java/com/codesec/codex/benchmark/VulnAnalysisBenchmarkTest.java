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

class VulnAnalysisBenchmarkTest {

    private CodexClient mockClient;
    private PromptRepository repo;
    private CodexProperties props;
    private List<VulnAnalysisSample> samples;

    @BeforeEach
    void setUp() {
        mockClient = mock(CodexClient.class);
        when(mockClient.execute(any(), anyString(), anyString())).thenReturn("SQL Injection detected (CWE-89)");
        PromptLoader loader = new PromptLoader();
        repo = new PromptRepository(loader);
        props = new CodexProperties();

        samples = List.of(
            createSample("vuln-001", "java", "SELECT * FROM users WHERE id = '" + request() + "'", "SQL Injection", "CWE-89", "basic"),
            createSample("vuln-002", "java", "<div>" + param() + "</div>", "XSS", "CWE-79", "basic")
        );
    }

    @Test
    void runReturnsBenchmarkResult() {
        VulnAnalysisBenchmark b = new VulnAnalysisBenchmark(mockClient, repo, props, samples);
        BenchmarkResult result = b.run();
        assertNotNull(result);
        assertEquals("VulnAnalysis", result.getName());
        assertTrue(result.getSampleCount() > 0);
    }

    @Test
    void runProducesNonNegativeMetrics() {
        VulnAnalysisBenchmark b = new VulnAnalysisBenchmark(mockClient, repo, props, samples);
        BenchmarkResult result = b.run();
        assertTrue(result.getPrecision() >= 0);
        assertTrue(result.getRecall() >= 0);
        assertTrue(result.getF1() >= 0);
    }

    @Test
    void runHandlesCodexClientFailure() {
        when(mockClient.execute(any(), anyString(), anyString())).thenThrow(new RuntimeException("API down"));
        VulnAnalysisBenchmark b = new VulnAnalysisBenchmark(mockClient, repo, props, samples);
        BenchmarkResult result = b.run();
        assertNotNull(result);
        assertTrue(result.getSampleCount() > 0);
    }

    @Test
    void runHandlesEmptySamples() {
        VulnAnalysisBenchmark b = new VulnAnalysisBenchmark(mockClient, repo, props, List.of());
        BenchmarkResult result = b.run();
        assertEquals(0, result.getSampleCount());
    }

    private String request() { return "request.getParameter(\"id\")"; }
    private String param() { return "request.getParameter(\"name\")"; }

    private VulnAnalysisSample createSample(String id, String lang, String code, String vuln, String cwe, String diff) {
        VulnAnalysisSample s = new VulnAnalysisSample();
        s.setId(id);
        s.setLanguage(lang);
        s.setCode(code);
        s.setExpectedVuln(vuln);
        s.setCwe(cwe);
        s.setDifficulty(diff);
        return s;
    }
}
