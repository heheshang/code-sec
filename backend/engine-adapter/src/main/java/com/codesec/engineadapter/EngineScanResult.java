package com.codesec.engineadapter;

import com.codesec.engine.model.Finding;
import java.util.List;

public record EngineScanResult(
    String scanId,
    List<Finding> findings,
    long durationMs
) {}
