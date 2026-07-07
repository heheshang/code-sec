package com.codesec.engineadapter;

import java.util.List;

public record EngineScanResult(
    String scanId,
    List<FindingDto> findings,
    long durationMs
) {}
