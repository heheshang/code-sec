package com.codesec.engineadapter;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ScanRequest}.
 */
class ScanRequestTest {

    @Test
    void of_createsRequest_withDefaultEngines() {
        ScanRequest req = ScanRequest.of(42L, Path.of("/tmp/repo"), "abc123");
        assertEquals(42L, req.repoId());
        assertEquals(Path.of("/tmp/repo"), req.sourceRoot());
        assertEquals("abc123", req.commitSha());
        assertEquals(List.of("self_sast"), req.engines());
    }

    @Test
    void constructor_acceptsExplicitEngines() {
        ScanRequest req = new ScanRequest(1L, Path.of("/tmp"), "sha", List.of("self_sast", "codeql"));
        assertEquals(List.of("self_sast", "codeql"), req.engines());
    }

    @Test
    void equalsAndHashCode() {
        ScanRequest a = ScanRequest.of(1L, Path.of("/a"), "s1");
        ScanRequest b = ScanRequest.of(1L, Path.of("/a"), "s1");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
