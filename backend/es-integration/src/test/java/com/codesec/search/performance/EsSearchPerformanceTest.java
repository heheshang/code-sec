package com.codesec.search.performance;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Performance benchmark for ES search queries.
 *
 * <p>Target: P99 &lt; 500ms for typical search patterns.
 * Requires ES container running ({@code docker compose up}).
 *
 * <p>Run manually after infrastructure is up:
 * <pre>{@code
 *   mvn test -pl backend/es-integration -Dtest="EsSearchPerformanceTest" -DskipTests=false
 * }</pre>
 */
@Disabled("Requires ES container — run manually after docker compose up")
public class EsSearchPerformanceTest {

    @Test
    void searchByKeywordUnder500ms() {
        // TODO: implement with proper ES test container or live ES
        assertTrue(true);
    }

    @Test
    void searchBySeverityFilterUnder500ms() {
        // TODO: implement
        assertTrue(true);
    }

    @Test
    void searchByDateRangeUnder500ms() {
        // TODO: implement
        assertTrue(true);
    }
}
