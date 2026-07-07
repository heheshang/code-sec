package com.codesec.engineadapter;

import java.nio.file.Path;
import java.util.List;

public record ScanRequest(
    Long repoId,
    Path sourceRoot,
    String commitSha,
    List<String> engines
) {
    public static ScanRequest of(Long repoId, Path sourceRoot, String commitSha) {
        return new ScanRequest(repoId, sourceRoot, commitSha, List.of("self_sast"));
    }
}
