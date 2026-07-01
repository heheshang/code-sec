package com.codesec.engine.performance;

import com.codesec.engine.Engine;
import com.codesec.engine.model.Finding;
import com.codesec.engine.rule.RuleRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Accuracy Benchmark")
class AccuracyBenchmarkTest {

    /**
     * Ground truth for sample-code/judge/ files.
     * Key: file name suffix to match in finding filePath.
     * Value: expected exploitability string.
     * UntouchedLibrary can be NOT_EXPLOITABLE (proven unreachable when
     * scanned alongside controllers) or POTENTIALLY_EXPLOITABLE
     * (scanned in isolation); either is acceptable.
     */
    private static final Map<String, String> EXPECTED = Map.of(
        "ExploitableController", "exploitable",
        "DeadCodeUtil", "not_exploitable",
        "ProtectedController", "not_exploitable",
        "IndirectDao", "exploitable",
        "UntouchedLibrary", "potentially_exploitable_or_not"
    );

    /** Files that should be EXPLOITABLE (for precision/recall calculation). */
    private static final List<String> EXPLOITABLE_FILES = List.of(
        "ExploitableController", "IndirectDao"
    );

    private static Engine engine;

    @BeforeAll
    static void setUp() throws Exception {
        RuleRegistry registry = new RuleRegistry();
        registry.loadFromClasspath("rules/java", "rules/go", "rules/python");
        engine = Engine.create(registry);
    }

    @Nested
    @DisplayName("Per-file accuracy")
    class PerFileAccuracy {

        @Test
        @DisplayName("ExploitableController -> EXPLOITABLE")
        void exploitableController_isExploitable() throws Exception {
            var findings = scanJudgeSamples();
            var fileFindings = filterByFile(findings, "ExploitableController");
            assertFalse(fileFindings.isEmpty(), "Should find at least 1 finding in ExploitableController");
            for (Finding f : fileFindings) {
                assertEquals("exploitable", f.exploitability(),
                    "ExploitableController should be EXPLOITABLE, got: " + f.exploitability()
                    + " reason: " + f.exploitReason());
            }
        }

        @Test
        @DisplayName("DeadCodeUtil -> NOT_EXPLOITABLE")
        void deadCodeUtil_isNotExploitable() throws Exception {
            var findings = scanJudgeSamples();
            var fileFindings = filterByFile(findings, "DeadCodeUtil");
            for (Finding f : fileFindings) {
                assertEquals("not_exploitable", f.exploitability(),
                    "DeadCodeUtil should be NOT_EXPLOITABLE, got: " + f.exploitability()
                    + " reason: " + f.exploitReason());
            }
        }

        @Test
        @DisplayName("ProtectedController -> NOT_EXPLOITABLE")
        void protectedController_isNotExploitable() throws Exception {
            var findings = scanJudgeSamples();
            var fileFindings = filterByFile(findings, "ProtectedController");
            assertFalse(fileFindings.isEmpty(), "Should find at least 1 finding in ProtectedController");
            for (Finding f : fileFindings) {
                assertEquals("not_exploitable", f.exploitability(),
                    "ProtectedController should be NOT_EXPLOITABLE, got: " + f.exploitability()
                    + " reason: " + f.exploitReason());
            }
        }

        @Test
        @DisplayName("IndirectDao -> EXPLOITABLE")
        void indirectDao_isExploitable() throws Exception {
            var findings = scanJudgeSamples();
            var fileFindings = filterByFile(findings, "IndirectDao");
            assertFalse(fileFindings.isEmpty(), "Should find at least 1 finding in IndirectDao");
            for (Finding f : fileFindings) {
                assertEquals("exploitable", f.exploitability(),
                    "IndirectDao should be EXPLOITABLE, got: " + f.exploitability()
                    + " reason: " + f.exploitReason());
            }
        }

        @Test
        @DisplayName("UntouchedLibrary -> NOT_EXPLOITABLE or POTENTIALLY_EXPLOITABLE")
        void untouchedLibrary_isNotOrPotentiallyExploitable() throws Exception {
            var findings = scanJudgeSamples();
            var fileFindings = filterByFile(findings, "UntouchedLibrary");
            for (Finding f : fileFindings) {
                String exploit = f.exploitability();
                assertTrue(
                    "not_exploitable".equals(exploit) || "potentially_exploitable".equals(exploit),
                    "UntouchedLibrary should be NOT_EXPLOITABLE or POTENTIALLY_EXPLOITABLE, got: "
                    + exploit + " reason: " + f.exploitReason());
            }
        }
    }

    @Nested
    @DisplayName("Precision & Recall")
    class PrecisionRecall {

        @Test
        @DisplayName("precision@EXPLOITABLE >= 80% and recall@EXPLOITABLE >= 90%")
        void precisionAndRecall_meetThresholds() throws Exception {
            var findings = scanJudgeSamples();
            assertFalse(findings.isEmpty(), "Should have findings");

            int truePositive = 0;
            int falsePositive = 0;
            int falseNegative = 0;

            for (Finding f : findings) {
                boolean shouldBeExploitable = EXPLOITABLE_FILES.stream()
                    .anyMatch(expected -> f.filePath().contains(expected));
                boolean reportedExploitable = "exploitable".equals(f.exploitability());

                if (shouldBeExploitable && reportedExploitable) {
                    truePositive++;
                } else if (!shouldBeExploitable && reportedExploitable) {
                    falsePositive++;
                } else if (shouldBeExploitable && !reportedExploitable) {
                    falseNegative++;
                }
            }

            double precision = (truePositive + falsePositive) == 0
                ? 1.0 : (double) truePositive / (truePositive + falsePositive);
            double recall = (truePositive + falseNegative) == 0
                ? 1.0 : (double) truePositive / (truePositive + falseNegative);

            System.out.println();
            System.out.println("=== Accuracy Benchmark Results ===");
            System.out.printf("True Positives (correct EXPLOITABLE): %d%n", truePositive);
            System.out.printf("False Positives (incorrect EXPLOITABLE): %d%n", falsePositive);
            System.out.printf("False Negatives (missed EXPLOITABLE): %d%n", falseNegative);
            System.out.printf("Precision@EXPLOITABLE: %.1f%% (threshold: >= 80%%)%n",
                precision * 100);
            System.out.printf("Recall@EXPLOITABLE: %.1f%% (threshold: >= 90%%)%n",
                recall * 100);

            // Per-file results
            System.out.println();
            System.out.println("--- Per-file Results ---");
            for (var entry : EXPECTED.entrySet()) {
                var fileFindings = filterByFile(findings, entry.getKey());
                String expectedVal = entry.getValue();
                if (fileFindings.isEmpty()) {
                    System.out.printf("  %s: NO FINDINGS (expected: %s)%n",
                        entry.getKey(), expectedVal);
                } else {
                    for (Finding f : fileFindings) {
                        boolean match = "potentially_exploitable_or_not".equals(expectedVal)
                            ? ("not_exploitable".equals(f.exploitability())
                                || "potentially_exploitable".equals(f.exploitability()))
                            : expectedVal.equals(f.exploitability());
                        System.out.printf("  %s: %s %s (expected: %s) reason: %s%n",
                            entry.getKey(), f.exploitability(),
                            match ? "PASS" : "FAIL",
                            expectedVal, f.exploitReason());
                    }
                }
            }
            System.out.println("===================================");
            System.out.println();

            assertTrue(precision >= 0.80,
                String.format("Precision %.1f%% is below 80%% threshold", precision * 100));
            assertTrue(recall >= 0.90,
                String.format("Recall %.1f%% is below 90%% threshold", recall * 100));
        }
    }

    @Nested
    @DisplayName("Field completeness")
    class FieldCompleteness {

        @Test
        @DisplayName("all findings have non-null exploitability and exploitReason")
        void allFindings_haveAllFields() throws Exception {
            var findings = scanJudgeSamples();
            assertFalse(findings.isEmpty(), "Should have at least one finding");

            for (Finding f : findings) {
                assertNotNull(f.exploitability(),
                    "exploitability must not be null for " + f.filePath());
                assertNotNull(f.exploitReason(),
                    "exploitReason must not be null for " + f.filePath());
                assertFalse(f.exploitReason().isBlank(),
                    "exploitReason must not be blank for " + f.filePath());
            }
        }
    }

    /**
     * Runs the engine on judge/ samples and returns findings.
     */
    private List<Finding> scanJudgeSamples() throws Exception {
        Path judgeDir = Paths.get("examples/sample-code/judge");
        assertTrue(Files.isDirectory(judgeDir),
            "Judge sample directory must exist at: " + judgeDir.toAbsolutePath());
        return engine.scan(judgeDir);
    }

    /**
     * Returns findings whose filePath contains the given suffix.
     */
    private static List<Finding> filterByFile(List<Finding> findings, String fileSuffix) {
        return findings.stream()
            .filter(f -> f.filePath().contains(fileSuffix))
            .toList();
    }
}
