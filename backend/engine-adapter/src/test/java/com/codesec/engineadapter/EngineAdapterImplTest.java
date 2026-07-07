package com.codesec.engineadapter;

import com.codesec.engine.rule.Detection;
import com.codesec.engine.rule.Rule;
import com.codesec.engine.rule.RuleRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link EngineAdapterImpl}.
 *
 * <p>Creates a {@link RuleRegistry} with sample rules and runs scan/scanFiles/health
 * against a real {@link com.codesec.engine.Engine} instance on temp source trees.</p>
 */
class EngineAdapterImplTest {

    private EngineAdapterImpl adapter;
    private RuleRegistry ruleRegistry;

    @BeforeEach
    void setUp() {
        ruleRegistry = new RuleRegistry();
        // Register a Java rule so the engine has something to scan for
        ruleRegistry.addRule(new Rule(
            "java/sql-injection-001",
            "SQL Injection",
            "high",
            "89",
            List.of("java"),
            "self_sast",
            new Detection("regex", "SELECT.*FROM.*\\+"),
            null,
            List.of(),
            "test",
            true
        ));
        ruleRegistry.addRule(new Rule(
            "java/hardcoded-password-001",
            "Hardcoded Password",
            "medium",
            "798",
            List.of("java"),
            "self_sast",
            new Detection("regex", "password\\s*=\\s*\"[^\"]+\""),
            null,
            List.of(),
            "test",
            true
        ));
        adapter = new EngineAdapterImpl(ruleRegistry, null);
    }

    // ---------------------------------------------------------------
    // scan(ScanRequest)
    // ---------------------------------------------------------------

    @Test
    void scan_returnsFindings_whenSourceContainsVulns(@TempDir Path tempDir) throws IOException {
        Path sourceFile = tempDir.resolve("VulnApp.java");
        Files.writeString(sourceFile, """
            import java.sql.Connection;
            import java.sql.ResultSet;
            import java.sql.Statement;

            public class VulnApp {
                public void unsafeQuery(Connection conn, String userInput) throws Exception {
                    Statement stmt = conn.createStatement();
                    // SQL injection
                    ResultSet rs = stmt.executeQuery("SELECT * FROM users WHERE id = " + userInput);
                }
            }
            """);

        ScanRequest request = ScanRequest.of(1L, tempDir, "abc123");
        EngineScanResult result = adapter.scan(request);

        assertNotNull(result);
        assertNotNull(result.scanId());
        assertFalse(result.scanId().isBlank());
        assertTrue(result.durationMs() >= 0);
        // At least one finding expected for SQL injection code
        assertFalse(result.findings().isEmpty(), "Expected findings for SQL injection source");
    }

    @Test
    void scan_returnsEmpty_whenSourceIsEmpty(@TempDir Path tempDir) throws IOException {
        Files.createFile(tempDir.resolve("Empty.java"));
        ScanRequest request = ScanRequest.of(2L, tempDir, "def456");
        EngineScanResult result = adapter.scan(request);

        assertNotNull(result);
        assertTrue(result.findings().isEmpty(), "Empty source should yield no findings");
    }

    @Test
    void scan_returnsEmpty_whenSourceIsNotJava(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("readme.txt"), "hello world");
        Files.writeString(tempDir.resolve("config.json"), "{ \"key\": \"value\" }");

        ScanRequest request = ScanRequest.of(3L, tempDir, "ghi789");
        EngineScanResult result = adapter.scan(request);

        assertNotNull(result);
        assertTrue(result.findings().isEmpty(), "Non-Java files should yield no findings");
    }

    @Test
    void scan_returnsEmpty_whenPathIsNotDirectory(@TempDir Path tempDir) throws IOException {
        Path notDir = tempDir.resolve("not-a-dir.txt");
        Files.writeString(notDir, "not-a-dir");

        ScanRequest request = ScanRequest.of(4L, notDir, "jkl012");
        EngineScanResult result = adapter.scan(request);

        assertNotNull(result);
        assertTrue(result.findings().isEmpty(), "Non-directory path should yield no findings (graceful error)");
    }

    // ---------------------------------------------------------------
    // scanFiles(Path, List<String>)
    // ---------------------------------------------------------------

    @Test
    void scanFiles_filtersFindings_byRelativeFile(@TempDir Path tempDir) throws IOException {
        // Two Java files: one with vuln, one clean
        Path vulnFile = tempDir.resolve("VulnApp.java");
        Files.writeString(vulnFile, """
            import java.sql.Connection;
            import java.sql.ResultSet;
            import java.sql.Statement;

            public class VulnApp {
                public void unsafeQuery(Connection conn, String userInput) throws Exception {
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT * FROM users WHERE id = " + userInput);
                }
            }
            """);

        Path cleanFile = tempDir.resolve("CleanService.java");
        Files.writeString(cleanFile, """
            public class CleanService {
                public String greet(String name) {
                    return "Hello, " + name;
                }
            }
            """);

        // Scan all files first to see total findings
        EngineScanResult fullResult = adapter.scan(ScanRequest.of(5L, tempDir, "mno345"));
        assertFalse(fullResult.findings().isEmpty(), "Full scan should find vuln in VulnApp.java");

        // Now scanFiles with only the clean file — should get 0 or filtered findings
        EngineScanResult filteredResult = adapter.scanFiles(tempDir, List.of("CleanService.java"));

        assertNotNull(filteredResult);
        assertNotNull(filteredResult.scanId());
        // The finding file paths should contain CleanService only
        boolean allMatchClean = filteredResult.findings().stream()
            .allMatch(f -> f.filePath().contains("CleanService"));
        assertTrue(allMatchClean, "All filtered findings should match the requested file(s)");
    }

    @Test
    void scanFiles_returnsEmpty_whenFileListIsEmpty(@TempDir Path tempDir) {
        EngineScanResult result = adapter.scanFiles(tempDir, List.of());
        assertNotNull(result);
        assertTrue(result.findings().isEmpty());
        assertEquals(0, result.durationMs());
    }

    @Test
    void scanFiles_returnsEmpty_whenFileListIsNull(@TempDir Path tempDir) {
        EngineScanResult result = adapter.scanFiles(tempDir, null);
        assertNotNull(result);
        assertTrue(result.findings().isEmpty());
        assertEquals(0, result.durationMs());
    }

    // ---------------------------------------------------------------
    // health()
    // ---------------------------------------------------------------

    @Test
    void health_returnsExpectedValues() {
        EngineHealth h = adapter.health();
        assertTrue(h.isOk());
        assertEquals("1.0.0-SNAPSHOT", h.getEngineVersion());
        // scanCount should be 0 if no scan ran yet
        assertEquals(0, h.getScanCount());
    }

    @Test
    void health_scanCountReflectsCompletedScans(@TempDir Path tempDir) throws IOException {
        Files.createFile(tempDir.resolve("Empty.java"));

        assertEquals(0, adapter.health().getScanCount());
        adapter.scan(ScanRequest.of(6L, tempDir, "pqr678"));
        assertEquals(1, adapter.health().getScanCount());
        adapter.scan(ScanRequest.of(7L, tempDir, "stu901"));
        assertEquals(2, adapter.health().getScanCount());
    }
}
