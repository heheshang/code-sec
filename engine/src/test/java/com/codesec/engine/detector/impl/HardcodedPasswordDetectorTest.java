package com.codesec.engine.detector.impl;

import com.codesec.engine.model.Finding;
import com.codesec.engine.parser.ParsedFile;
import com.codesec.engine.rule.Rule;
import com.codesec.engine.rule.RuleLoader;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HardcodedPasswordDetectorTest {

    private static final HardcodedPasswordDetector detector = new HardcodedPasswordDetector();

    @Test
    void detect_shouldFindHardcodedPassword() throws Exception {
        String code = """
            package test;
            public class Test {
                public static final String dbPassword = "admin123!";
            }
            """;

        Rule rule = RuleLoader.loadFromClasspath("rules/java/hardcoded-password-001.yml");
        ParsedFile file = new ParsedFile(Path.of("Test.java"), "java", code, null);
        List<Finding> findings = detector.detect(file, rule);

        assertFalse(findings.isEmpty(), "Should detect hardcoded password");
        Finding f = findings.get(0);
        assertEquals("java/hardcoded-password-001", f.ruleId());
        assertEquals("high", f.severity());
        assertEquals("CWE-798", f.cwe());
    }

    @Test
    void detect_shouldFindHardcodedApiKey() throws Exception {
        String code = """
            package test;
            public class Test {
                private String apiKey = "sk-1234567890abcdef";
            }
            """;

        Rule rule = RuleLoader.loadFromClasspath("rules/java/hardcoded-password-001.yml");
        ParsedFile file = new ParsedFile(Path.of("Test.java"), "java", code, null);
        List<Finding> findings = detector.detect(file, rule);

        assertFalse(findings.isEmpty(), "Should detect hardcoded API key");
    }

    @Test
    void detect_shouldFindHardcodedToken() throws Exception {
        String code = """
            package test;
            public class Test {
                String authToken = "eyJhbGciOiJIUzI1NiJ9...";
            }
            """;

        Rule rule = RuleLoader.loadFromClasspath("rules/java/hardcoded-password-001.yml");
        ParsedFile file = new ParsedFile(Path.of("Test.java"), "java", code, null);
        List<Finding> findings = detector.detect(file, rule);

        assertFalse(findings.isEmpty(), "Should detect hardcoded token");
    }

    @Test
    void detect_shouldNotFlagNonSecretStrings() throws Exception {
        String code = """
            package test;
            public class Test {
                private String url = "https://api.example.com";
                private int port = 8080;
            }
            """;

        Rule rule = RuleLoader.loadFromClasspath("rules/java/hardcoded-password-001.yml");
        ParsedFile file = new ParsedFile(Path.of("Test.java"), "java", code, null);
        List<Finding> findings = detector.detect(file, rule);

        assertTrue(findings.isEmpty(), "Should not flag non-secret string assignments");
    }

    @Test
    void detect_shouldNotFlagEnvVariables() throws Exception {
        String code = """
            package test;
            public class Test {
                String password = System.getenv("DB_PASSWORD");
            }
            """;

        Rule rule = RuleLoader.loadFromClasspath("rules/java/hardcoded-password-001.yml");
        ParsedFile file = new ParsedFile(Path.of("Test.java"), "java", code, null);
        List<Finding> findings = detector.detect(file, rule);

        assertTrue(findings.isEmpty(), "Should not flag environment variable reads");
    }

    @Test
    void detect_shouldDetectBothFindingsInConfig() throws Exception {
        String code = """
            package test;
            public class Test {
                public static final String PASSWORD = "secret123";
                public static final String API_KEY = "key-abc";
                public static final String DB_HOST = "localhost";
            }
            """;

        Rule rule = RuleLoader.loadFromClasspath("rules/java/hardcoded-password-001.yml");
        ParsedFile file = new ParsedFile(Path.of("Test.java"), "java", code, null);
        List<Finding> findings = detector.detect(file, rule);

        assertEquals(2, findings.size(), "Should detect both password and API key");
    }
}
