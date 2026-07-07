package com.codesec.engineadapter;

import com.codesec.common.dto.FindingDto;
import java.util.List;

public record EngineScanResult(
    String scanId,
    List<FindingDto> findings,
    long durationMs
) {}
