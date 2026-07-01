package com.codesec.engine.detector.impl;

import com.codesec.engine.model.Finding;
import com.codesec.engine.parser.ParsedFile;
import com.codesec.engine.parser.languages.GoLanguage;
import com.codesec.engine.rule.Rule;
import com.codesec.engine.rule.RuleLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Go Detector Tests")
class GoDetectorTest {

    private static final Path SAMPLE_DIR = Paths.get("examples/sample-code-go");
    private static GoLanguage goParser;

    @BeforeAll
    static void setUp() {
        goParser = new GoLanguage();
    }

    @Test
    @DisplayName("go/hardcoded-password-001 detects hardcoded secrets in Go files")
    void hardcodedPassword() throws Exception {
        Rule rule = RuleLoader.loadFromClasspath("rules/go/hardcoded-password-001.yml");
        HardcodedPasswordDetector detector = new HardcodedPasswordDetector();

        ParsedFile file = goParser.parse(SAMPLE_DIR.resolve("hardcoded_password.go"));
        List<Finding> findings = detector.detect(file, rule);

        assertEquals(2, findings.size(),
            "Expected 2 findings for password and apiKey");
        findings.forEach(f -> assertEquals("go/hardcoded-password-001", f.ruleId()));
    }

    @Test
    @DisplayName("go/hardcoded-password-001 does not fire on safe Go code")
    void noFalsePositiveOnSafeGo() throws Exception {
        Rule rule = RuleLoader.loadFromClasspath("rules/go/hardcoded-password-001.yml");
        HardcodedPasswordDetector detector = new HardcodedPasswordDetector();

        ParsedFile file = goParser.parse(SAMPLE_DIR.resolve("safe_code.go"));
        List<Finding> findings = detector.detect(file, rule);

        assertEquals(0, findings.size(), "No hardcoded secrets expected in safe_code.go");
    }

    @Test
    @DisplayName("go/command-injection-001 detects exec.Command with variable args")
    void commandInjection() throws Exception {
        Rule rule = RuleLoader.loadFromClasspath("rules/go/command-injection-001.yml");
        GoCommandInjectionDetector detector = new GoCommandInjectionDetector();

        ParsedFile file = goParser.parse(SAMPLE_DIR.resolve("command_injection.go"));
        List<Finding> findings = detector.detect(file, rule);

        assertEquals(1, findings.size(),
            "Expected 1 finding for exec.Command with variable argument");
        assertEquals("go/command-injection-001", findings.get(0).ruleId());
        assertTrue(findings.get(0).codeSnippet().contains("exec.Command"),
            "Snippet should contain exec.Command call");
    }

    @Test
    @DisplayName("go/command-injection-001 does not fire on safe Go code")
    void noFalsePositiveOnSafeCommand() throws Exception {
        Rule rule = RuleLoader.loadFromClasspath("rules/go/command-injection-001.yml");
        GoCommandInjectionDetector detector = new GoCommandInjectionDetector();

        ParsedFile file = goParser.parse(SAMPLE_DIR.resolve("safe_code.go"));
        List<Finding> findings = detector.detect(file, rule);

        assertEquals(0, findings.size(), "No command injection expected in safe_code.go");
    }
}
