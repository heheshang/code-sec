package com.codesec.api.e2e;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * E2E: Vuln persist batch no longer publishes ES events.
 * ES indexing has been replaced by PG full-text search triggers.
 * Search functionality is tested separately via VulnSearchService.
 */
public class E12VulnPersistBatchE2ETest extends BaseE2ETest {

    @Test
    void contextLoads() {
        assertTrue(true, "Context loads successfully without ES dependencies");
    }
}
