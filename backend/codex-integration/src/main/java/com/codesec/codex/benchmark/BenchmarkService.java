package com.codesec.codex.benchmark;

import com.codesec.codex.client.CodexClient;
import com.codesec.codex.config.CodexProperties;
import com.codesec.codex.prompt.PromptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BenchmarkService {
    private static final Logger log = LoggerFactory.getLogger(BenchmarkService.class);

    private final BenchmarkRunner runner;
    private BenchmarkSummary lastSummary;

    public BenchmarkService(CodexClient codeModelClient, CodexClient llmModelClient,
                            PromptRepository promptRepo, CodexProperties props) {
        this.runner = new BenchmarkRunner(codeModelClient, llmModelClient, promptRepo, props);
        this.lastSummary = null;
    }

    public synchronized BenchmarkSummary getLastResults() {
        return lastSummary;
    }

    public synchronized BenchmarkSummary runBenchmarks() {
        log.info("Running all benchmarks...");
        lastSummary = runner.runAll();
        return lastSummary;
    }
}
