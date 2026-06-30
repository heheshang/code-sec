package com.codesec.engine.util;

import java.time.Duration;

public record BenchmarkResult(
    int totalFiles,
    long totalLinesOfCode,
    Duration totalScanTime,
    Duration p50PerFile,
    Duration p99PerFile,
    long memoryPeakBytes,
    int findingsExploitable,
    int findingsPotentiallyExploitable,
    int findingsNotExploitable
) {
    public String toSummary() {
        long memMb = memoryPeakBytes / (1024 * 1024);
        return String.format(
            "%d files, %d LOC, scan=%dms, p50=%dms, p99=%dms, mem=%dMB, "
                + "E=%d, P=%d, N=%d",
            totalFiles, totalLinesOfCode,
            totalScanTime.toMillis(),
            p50PerFile.toMillis(),
            p99PerFile.toMillis(),
            memMb,
            findingsExploitable,
            findingsPotentiallyExploitable,
            findingsNotExploitable
        );
    }
}
