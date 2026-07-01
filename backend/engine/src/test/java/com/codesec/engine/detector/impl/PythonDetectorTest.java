package com.codesec.engine.detector.impl;

import com.codesec.engine.model.Finding;
import com.codesec.engine.parser.ParsedFile;
import com.codesec.engine.parser.languages.PythonLanguage;
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

@DisplayName("Python Detector Tests")
class PythonDetectorTest {

    private static final Path SAMPLE_DIR = Paths.get("examples/sample-code-python");
    private static PythonLanguage pyParser;

    @BeforeAll
    static void setUp() {
        pyParser = new PythonLanguage();
    }

    @Test
    @DisplayName("python/sql-injection-001 detects string concatenation in execute() calls")
    void sqlInjection() throws Exception {
        Rule rule = RuleLoader.loadFromClasspath("rules/python/sql-injection-001.yml");
        PythonSqlInjectionDetector detector = new PythonSqlInjectionDetector();

        ParsedFile file = pyParser.parse(SAMPLE_DIR.resolve("sql_injection.py"));
        List<Finding> findings = detector.detect(file, rule);

        assertEquals(2, findings.size(),
            "Expected 2 findings: concat SQL and f-string SQL");
        findings.forEach(f -> assertEquals("python/sql-injection-001", f.ruleId()));
    }

    @Test
    @DisplayName("python/sql-injection-001 does not fire on parameterized queries")
    void noFalsePositiveOnParameterized() throws Exception {
        Rule rule = RuleLoader.loadFromClasspath("rules/python/sql-injection-001.yml");
        PythonSqlInjectionDetector detector = new PythonSqlInjectionDetector();

        ParsedFile file = pyParser.parse(SAMPLE_DIR.resolve("safe_code.py"));
        List<Finding> findings = detector.detect(file, rule);

        assertEquals(0, findings.size(), "No SQL injection expected in safe_code.py");
    }

    @Test
    @DisplayName("python/unsafe-eval-001 detects eval() with variable input")
    void unsafeEval() throws Exception {
        Rule rule = RuleLoader.loadFromClasspath("rules/python/unsafe-eval-001.yml");
        PythonUnsafeEvalDetector detector = new PythonUnsafeEvalDetector();

        ParsedFile file = pyParser.parse(SAMPLE_DIR.resolve("unsafe_eval.py"));
        List<Finding> findings = detector.detect(file, rule);

        assertEquals(2, findings.size(),
            "Expected 2 findings: eval(user_input) and eval(variable)");
        findings.forEach(f -> assertEquals("python/unsafe-eval-001", f.ruleId()));
        assertTrue(findings.get(0).codeSnippet().contains("eval("),
            "Snippet should contain eval() call");
    }

    @Test
    @DisplayName("python/unsafe-eval-001 does not fire on ast.literal_eval or safe code")
    void noFalsePositiveOnSafeEval() throws Exception {
        Rule rule = RuleLoader.loadFromClasspath("rules/python/unsafe-eval-001.yml");
        PythonUnsafeEvalDetector detector = new PythonUnsafeEvalDetector();

        ParsedFile file = pyParser.parse(SAMPLE_DIR.resolve("safe_code.py"));
        List<Finding> findings = detector.detect(file, rule);

        assertEquals(0, findings.size(), "No unsafe eval expected in safe_code.py");
    }
}
