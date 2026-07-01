package com.codesec.engine;

import com.codesec.engine.model.Finding;
import com.codesec.engine.rule.RuleRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EngineIntegrationTest {

    private Engine engine;

    @BeforeEach
    void setUp() throws Exception {
        RuleRegistry registry = new RuleRegistry();
        registry.loadFromClasspath("rules/java", "rules/go", "rules/python");
        engine = Engine.create(registry);
    }

    @Test
    void scan_shouldFindAllExpectedVulnerabilities() throws Exception {
        Path samplesDir = Paths.get("examples/sample-code");
        List<Finding> findings = engine.scan(samplesDir);

        // Filter to original sample files only (exclude judge/ subdirectory)
        var originalFindings = findings.stream()
            .filter(f -> !f.filePath().contains("judge/"))
            .toList();

        List<Finding> sqlInjections = findingsByRule(originalFindings, "java/sql-injection-001");
        List<Finding> hardcodedPw = findingsByRule(originalFindings, "java/hardcoded-password-001");
        List<Finding> xssFindings = findingsByRule(originalFindings, "java/xss-001");
        List<Finding> weakCrypto = findingsByRule(originalFindings, "java/weak-crypto-001");

        assertFalse(sqlInjections.isEmpty(), "Should find SQL injection in UserDao.java");
        assertFalse(hardcodedPw.isEmpty(), "Should find hardcoded passwords in Config.java");
        assertFalse(xssFindings.isEmpty(), "Should find XSS in WebController.java");
        assertFalse(weakCrypto.isEmpty(), "Should find weak crypto in CipherUtil.java");

        assertEquals(1, sqlInjections.size(), "Should have 1 SQL injection finding");
        assertTrue(hardcodedPw.size() >= 2, "Config.java should have at least 2 hardcoded secrets");

        Finding sqlFinding = sqlInjections.get(0);
        assertTrue(sqlFinding.filePath().contains("UserDao.java"), "SQL finding should be in UserDao.java");
        assertEquals("high", sqlFinding.severity());
        assertEquals("CWE-89", sqlFinding.cwe());

        Finding xssFinding = xssFindings.get(0);
        assertTrue(xssFinding.filePath().contains("WebController.java"), "XSS finding should be in WebController.java");
        assertEquals("CWE-79", xssFinding.cwe());

        Finding cryptoFinding = weakCrypto.get(0);
        assertTrue(cryptoFinding.filePath().contains("CipherUtil.java"), "Weak crypto finding should be in CipherUtil.java");
        assertEquals("CWE-327", cryptoFinding.cwe());
    }

    @Test
    void scan_shouldProduceNoFindingsForSafeCode() throws Exception {
        Path safeCodeDir = Paths.get("examples/sample-code");
        List<Finding> findings = engine.scan(safeCodeDir);

        List<Finding> safeFileFindings = findings.stream()
            .filter(f -> f.filePath().contains("SafeCode.java"))
            .toList();

        assertTrue(safeFileFindings.isEmpty(),
            "SafeCode.java should produce no findings, but got: " + safeFileFindings.size());
    }

    @Test
    void scan_shouldHaveRequiredFields() throws Exception {
        Path samplesDir = Paths.get("examples/sample-code");
        List<Finding> findings = engine.scan(samplesDir);

        assertFalse(findings.isEmpty(), "Should find at least one vulnerability");

        for (Finding f : findings) {
            assertNotNull(f.vulnId(), "vulnId must not be null");
            assertNotNull(f.engine(), "engine must not be null");
            assertEquals("self_sast", f.engine());
            assertNotNull(f.ruleId(), "ruleId must not be null");
            assertNotNull(f.severity(), "severity must not be null");
            assertNotNull(f.filePath(), "filePath must not be null");
            assertTrue(f.lineStart() > 0, "lineStart must be > 0");
            assertNotNull(f.codeSnippet(), "codeSnippet must not be null");
            assertNotNull(f.cwe(), "cwe must not be null");
            assertNotNull(f.exploitability(), "exploitability must not be null");
            assertNotNull(f.discoveredAt(), "discoveredAt must not be null");
        }
    }

    @Test
    void scan_shouldRunWithoutExceptions() throws Exception {
        Path samplesDir = Paths.get("examples/sample-code");
        List<Finding> findings = engine.scan(samplesDir);
        assertNotNull(findings);
    }

    @Test
    void scan_withJudgerEnabled_producesEnrichedFindings() throws Exception {
        Path samplesDir = Paths.get("examples/sample-code");
        List<Finding> findings = engine.scan(samplesDir);

        assertFalse(findings.isEmpty(), "Should find at least one vulnerability");

        for (Finding f : findings) {
            assertNotNull(f.exploitability(), "exploitability must be set by Judger");
            assertNotNull(f.exploitReason(), "exploitReason must be set by Judger");
            assertFalse(f.exploitReason().isBlank(), "exploitReason must not be blank");
        }
    }

    // ---- New tests for judge/ samples ----

    @Test
    @DisplayName("End-to-end: scan judge/ samples produces correct exploitability")
    void scanJudgeSamples_producesCorrectExploitability() throws Exception {
        Path judgeDir = Paths.get("examples/sample-code/judge");
        List<Finding> findings = engine.scan(judgeDir);

        assertFalse(findings.isEmpty(), "Should find vulnerabilities in judge/ samples");

        // Filter findings by file name
        List<Finding> exploitableFindings = findings.stream()
            .filter(f -> f.filePath().contains("ExploitableController.java"))
            .toList();
        List<Finding> deadCodeFindings = findings.stream()
            .filter(f -> f.filePath().contains("DeadCodeUtil.java"))
            .toList();
        List<Finding> protectedFindings = findings.stream()
            .filter(f -> f.filePath().contains("ProtectedController.java"))
            .toList();
        List<Finding> indirectDaoFindings = findings.stream()
            .filter(f -> f.filePath().contains("IndirectDao.java"))
            .toList();
        List<Finding> libraryFindings = findings.stream()
            .filter(f -> f.filePath().contains("UntouchedLibrary.java"))
            .toList();

        // ExploitableController: @RequestParam → SQL sink → EXPLOITABLE
        assertFalse(exploitableFindings.isEmpty(),
            "Should have findings for ExploitableController");
        for (Finding f : exploitableFindings) {
            assertEquals("exploitable", f.exploitability(),
                "ExploitableController finding should be EXPLOITABLE, file: " + f.filePath()
                + " reason: " + f.exploitReason());
        }

        // DeadCodeUtil: not reachable from any entry point → NOT_EXPLOITABLE
        for (Finding f : deadCodeFindings) {
            assertEquals("not_exploitable", f.exploitability(),
                "DeadCodeUtil finding should be NOT_EXPLOITABLE, reason: " + f.exploitReason());
        }

        // ProtectedController: class-level @PreAuthorize → NOT_EXPLOITABLE
        assertFalse(protectedFindings.isEmpty(),
            "Should have findings for ProtectedController");
        for (Finding f : protectedFindings) {
            assertEquals("not_exploitable", f.exploitability(),
                "ProtectedController finding should be NOT_EXPLOITABLE, reason: " + f.exploitReason());
        }

        // IndirectDao: reachable through controller chain → EXPLOITABLE
        assertFalse(indirectDaoFindings.isEmpty(),
            "Should have findings for IndirectDao");
        for (Finding f : indirectDaoFindings) {
            assertEquals("exploitable", f.exploitability(),
                "IndirectDao finding should be EXPLOITABLE, reason: " + f.exploitReason());
        }

        // UntouchedLibrary: unreachable from any entry point → NOT_EXPLOITABLE
        // (when scanned alongside controllers that provide entry points)
        for (Finding f : libraryFindings) {
            String exploit = f.exploitability();
            assertTrue("not_exploitable".equals(exploit) || "potentially_exploitable".equals(exploit),
                "UntouchedLibrary should be NOT_EXPLOITABLE or POTENTIALLY_EXPLOITABLE, got: "
                + exploit + " reason: " + f.exploitReason());
        }
    }

    @Test
    @DisplayName("End-to-end: all findings have non-null exploitability")
    void allFindings_haveNonNullExploitability() throws Exception {
        Path judgeDir = Paths.get("examples/sample-code/judge");
        List<Finding> findings = engine.scan(judgeDir);

        assertFalse(findings.isEmpty(), "Should find at least one finding");

        for (Finding f : findings) {
            assertNotNull(f.exploitability(),
                "exploitability must be non-null for " + f.filePath());
            assertNotNull(f.exploitReason(),
                "exploitReason must be non-null for " + f.filePath());
            assertFalse(f.exploitReason().isBlank(),
                "exploitReason must be non-blank for " + f.filePath() + ": " + f.exploitReason());
        }
    }

    @Test
    @DisplayName("End-to-end: findings with EXPLOITABLE have non-empty exploitReason")
    void exploitableFindings_haveNonEmptyReason() throws Exception {
        Path judgeDir = Paths.get("examples/sample-code/judge");
        List<Finding> findings = engine.scan(judgeDir);

        List<Finding> exploitableFindings = findings.stream()
            .filter(f -> "exploitable".equals(f.exploitability()))
            .toList();

        assertFalse(exploitableFindings.isEmpty(),
            "Should have at least one EXPLOITABLE finding");

        for (Finding f : exploitableFindings) {
            assertTrue(f.exploitReason() != null && !f.exploitReason().isBlank(),
                "EXPLOITABLE finding must have non-empty exploitReason, file: "
                + f.filePath() + " reason: " + f.exploitReason());
        }
    }

    // ---- Task 1.1b: Precision, Recall, and Field Completeness assertions ----

    /** Ground truth: files expected to be EXPLOITABLE. */
    private static final List<String> EXPLOITABLE_FILES = List.of(
        "ExploitableController", "IndirectDao"
    );

    @Test
    @DisplayName("Precision@EXPLOITABLE >= 80% (实测 100%)")
    void verifyPrecisionMeetsThreshold() throws Exception {
        Path judgeDir = Paths.get("examples/sample-code/judge");
        List<Finding> findings = engine.scan(judgeDir);
        assertFalse(findings.isEmpty(), "Should have findings");

        int truePositive = 0;
        int falsePositive = 0;

        for (Finding f : findings) {
            boolean shouldBeExploitable = EXPLOITABLE_FILES.stream()
                .anyMatch(expected -> f.filePath().contains(expected));
            boolean reportedExploitable = "exploitable".equals(f.exploitability());

            if (shouldBeExploitable && reportedExploitable) {
                truePositive++;
            } else if (!shouldBeExploitable && reportedExploitable) {
                falsePositive++;
            }
        }

        double precision = (truePositive + falsePositive) == 0
            ? 1.0 : (double) truePositive / (truePositive + falsePositive);

        assertTrue(precision >= 0.80,
            String.format("Precision %.1f%% is below 80%% threshold (TP=%d, FP=%d)",
                precision * 100, truePositive, falsePositive));
    }

    @Test
    @DisplayName("Recall@EXPLOITABLE >= 90% (实测 100%)")
    void verifyRecallMeetsThreshold() throws Exception {
        Path judgeDir = Paths.get("examples/sample-code/judge");
        List<Finding> findings = engine.scan(judgeDir);
        assertFalse(findings.isEmpty(), "Should have findings");

        int truePositive = 0;
        int falseNegative = 0;

        for (Finding f : findings) {
            boolean shouldBeExploitable = EXPLOITABLE_FILES.stream()
                .anyMatch(expected -> f.filePath().contains(expected));
            boolean reportedExploitable = "exploitable".equals(f.exploitability());

            if (shouldBeExploitable && reportedExploitable) {
                truePositive++;
            } else if (shouldBeExploitable && !reportedExploitable) {
                falseNegative++;
            }
        }

        double recall = (truePositive + falseNegative) == 0
            ? 1.0 : (double) truePositive / (truePositive + falseNegative);

        assertTrue(recall >= 0.90,
            String.format("Recall %.1f%% is below 90%% threshold (TP=%d, FN=%d)",
                recall * 100, truePositive, falseNegative));
    }

    @Test
    @DisplayName("Field completeness: all judge/ findings have exploitability + exploitReason")
    void verifyFieldCompletenessForJudgeSamples() throws Exception {
        Path judgeDir = Paths.get("examples/sample-code/judge");
        List<Finding> findings = engine.scan(judgeDir);
        assertFalse(findings.isEmpty(), "Should have at least one finding");

        int completenessFailures = 0;
        for (Finding f : findings) {
            if (f.exploitability() == null) {
                completenessFailures++;
                System.err.println("FAIL: exploitability is null for " + f.filePath());
            }
            if (f.exploitReason() == null || f.exploitReason().isBlank()) {
                completenessFailures++;
                System.err.println("FAIL: exploitReason is null/blank for " + f.filePath());
            }
        }

        assertEquals(0, completenessFailures,
            String.format("Field completeness: %d failures out of %d findings",
                completenessFailures, findings.size()));
    }

    private static List<Finding> findingsByRule(List<Finding> all, String ruleId) {
        return all.stream()
            .filter(f -> f.ruleId().equals(ruleId))
            .toList();
    }
}
