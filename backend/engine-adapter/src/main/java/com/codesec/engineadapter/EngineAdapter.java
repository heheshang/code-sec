package com.codesec.engineadapter;

import java.nio.file.Path;
import java.util.List;

/**
 * Engine adapter - sole entry point for backend→engine communication.
 * All engine calls go through this interface.
 * Sprint 3 upgrade path: replace with engine-as-a-service via REST/gRPC.
 */
public interface EngineAdapter {
    /** Full scan: scan all .java files under sourceRoot. */
    EngineScanResult scan(ScanRequest request);

    /**
     * Incremental scan (E-S2-002 cross-Epic interface).
     * Only scans files listed in relativeFiles; other files are skipped.
     * Does NOT modify Engine.scan(Path) internals (F-EXJ-001 120/120 tests unchanged).
     */
    EngineScanResult scanFiles(Path sourceRoot, List<String> relativeFiles);

    EngineHealth health();
}
