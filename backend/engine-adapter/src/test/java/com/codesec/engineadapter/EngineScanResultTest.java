package com.codesec.engineadapter;

import com.codesec.engine.model.Finding;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link EngineScanResult}.
 */
class EngineScanResultTest {

    @Test
    void constructorAndAccessors() {
        List<Finding> findings = List.of(
            Finding.builder().ruleId("test-rule").title("Test finding").build()
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
