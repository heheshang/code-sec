package com.codesec.engineadapter;

import com.codesec.common.dto.FindingDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link EngineScanResult}.
 */
class EngineScanResultTest {

    @Test
    void constructorAndAccessors() {
        List<FindingDto> findings = List.of(
            new FindingDto("vuln-1", null, null, "self_sast", "test-rule",
                "Test finding", null, null, 0, 0, null, null, null,
                null, null, null, null, null, null, null, null, null, null)
        );
        EngineScanResult result = new EngineScanResult("scan-1", findings, 150L);

        assertEquals("scan-1", result.scanId());
        assertEquals(1, result.findings().size());
        assertEquals(150L, result.durationMs());
    }

    @Test
    void emptyFindings() {
        EngineScanResult result = new EngineScanResult("scan-2", List.of(), 0L);
        assertTrue(result.findings().isEmpty());
        assertEquals(0L, result.durationMs());
    }
}
