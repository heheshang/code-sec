package com.codesec.codex.benchmark;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai")
@ConditionalOnProperty(name = "codex.benchmarks.enabled", havingValue = "true", matchIfMissing = false)
public class BenchmarkController {
    private static final Logger log = LoggerFactory.getLogger(BenchmarkController.class);

    private final BenchmarkService benchmarkService;

    public BenchmarkController(BenchmarkService benchmarkService) {
        this.benchmarkService = benchmarkService;
    }

    @GetMapping("/benchmarks")
    public ResponseEntity<List<BenchmarkResult>> getBenchmarks() {
        BenchmarkSummary summary = benchmarkService.getLastResults();
        if (summary == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(summary.getResults());
    }

    @PostMapping("/benchmarks/run")
    public ResponseEntity<List<BenchmarkResult>> runBenchmarks() {
        log.info("Benchmark run requested");
        BenchmarkSummary summary = benchmarkService.runBenchmarks();
        return ResponseEntity.ok(summary.getResults());
    }
}
