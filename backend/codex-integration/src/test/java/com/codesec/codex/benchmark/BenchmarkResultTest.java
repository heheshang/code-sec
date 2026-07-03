package com.codesec.codex.benchmark;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BenchmarkResultTest {

    @Test
    void aggregateComputesCorrectMetrics() {
        BenchmarkResult r1 = new BenchmarkResult("A", 0.8, 0.75, 0.77,
            new int[][]{{5, 1}, {2, 4}}, 12, 100);
        BenchmarkResult r2 = new BenchmarkResult("B", 0.9, 0.85, 0.87,
            new int[][]{{8, 1}, {1, 6}}, 16, 150);

        BenchmarkResult agg = BenchmarkResult.aggregate(List.of(r1, r2));
        assertEquals("Overall", agg.getName());
        assertEquals(28, agg.getSampleCount());
        assertEquals(250, agg.getDurationMs());
        assertTrue(agg.getPrecision() > 0.8);
        assertTrue(agg.getPrecision() < 0.9);
        assertEquals(2, agg.getConfusionMatrix().length);
        assertEquals(13, agg.getConfusionMatrix()[0][0]);
    }

    @Test
    void aggregateHandlesEmptyList() {
        BenchmarkResult agg = BenchmarkResult.aggregate(List.of());
        assertEquals(0, agg.getSampleCount());
        assertEquals(0, agg.getConfusionMatrix().length);
    }
}
