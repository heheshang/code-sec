package com.codesec.codex.benchmark;

import java.util.List;

public class BenchmarkSummary {
    private List<BenchmarkResult> results;
    private BenchmarkResult aggregate;
    private long totalDurationMs;
    private String lastRunAt;

    public BenchmarkSummary() {}

    public BenchmarkSummary(List<BenchmarkResult> results, long totalDurationMs, String lastRunAt) {
        this.results = results;
        this.aggregate = BenchmarkResult.aggregate(results);
        this.totalDurationMs = totalDurationMs;
        this.lastRunAt = lastRunAt;
    }

    public List<BenchmarkResult> getResults() { return results; }
    public void setResults(List<BenchmarkResult> results) { this.results = results; }
    public BenchmarkResult getAggregate() { return aggregate; }
    public void setAggregate(BenchmarkResult aggregate) { this.aggregate = aggregate; }
    public long getTotalDurationMs() { return totalDurationMs; }
    public void setTotalDurationMs(long totalDurationMs) { this.totalDurationMs = totalDurationMs; }
    public String getLastRunAt() { return lastRunAt; }
    public void setLastRunAt(String lastRunAt) { this.lastRunAt = lastRunAt; }

    public int getTotalSampleCount() {
        return results.stream().mapToInt(BenchmarkResult::getSampleCount).sum();
    }
}
