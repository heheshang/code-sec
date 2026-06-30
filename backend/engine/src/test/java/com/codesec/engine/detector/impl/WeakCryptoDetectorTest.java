package com.codesec.engine.detector.impl;

import com.codesec.engine.model.Finding;
import com.codesec.engine.parser.ParsedFile;
import com.codesec.engine.rule.Rule;
import com.codesec.engine.rule.RuleLoader;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WeakCryptoDetectorTest {

    private static final WeakCryptoDetector detector = new WeakCryptoDetector();

    @Test
    void detect_shouldFindMd5() throws Exception {
        String code = """
            package test;
            import java.security.MessageDigest;
            public class Test {
                public byte[] hash(String input) throws Exception {
                    MessageDigest md = MessageDigest.getInstance("MD5");
                    return md.digest(input.getBytes());
                }
            }
            """;

        Rule rule = RuleLoader.loadFromClasspath("rules/java/weak-crypto-001.yml");
        ParsedFile file = new ParsedFile(Path.of("Test.java"), "java", code, null);
        List<Finding> findings = detector.detect(file, rule);

        assertFalse(findings.isEmpty(), "Should detect MD5 usage");
        Finding f = findings.get(0);
        assertEquals("java/weak-crypto-001", f.ruleId());
        assertEquals("medium", f.severity());
        assertEquals("CWE-327", f.cwe());
    }

    @Test
    void detect_shouldFindSha1() throws Exception {
        String code = """
            package test;
            import java.security.MessageDigest;
            public class Test {
                public byte[] hash(String input) throws Exception {
                    MessageDigest md = MessageDigest.getInstance("SHA-1");
                    return md.digest(input.getBytes());
                }
            }
            """;

        Rule rule = RuleLoader.loadFromClasspath("rules/java/weak-crypto-001.yml");
        ParsedFile file = new ParsedFile(Path.of("Test.java"), "java", code, null);
        List<Finding> findings = detector.detect(file, rule);

        assertFalse(findings.isEmpty(), "Should detect SHA-1 usage");
    }

    @Test
    void detect_shouldFindDesCipher() throws Exception {
        String code = """
            package test;
            import javax.crypto.Cipher;
            public class Test {
                public void encrypt() throws Exception {
                    Cipher cipher = Cipher.getInstance("DES");
                }
            }
            """;

        Rule rule = RuleLoader.loadFromClasspath("rules/java/weak-crypto-001.yml");
        ParsedFile file = new ParsedFile(Path.of("Test.java"), "java", code, null);
        List<Finding> findings = detector.detect(file, rule);

        assertFalse(findings.isEmpty(), "Should detect DES cipher");
    }

    @Test
    void detect_shouldNotFlagStrongAlgorithms() throws Exception {
        String code = """
            package test;
            import java.security.MessageDigest;
            import javax.crypto.Cipher;
            public class Test {
                public byte[] hash(String input) throws Exception {
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    return md.digest(input.getBytes());
                }
                public void encrypt() throws Exception {
                    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                }
            }
            """;

        Rule rule = RuleLoader.loadFromClasspath("rules/java/weak-crypto-001.yml");
        ParsedFile file = new ParsedFile(Path.of("Test.java"), "java", code, null);
        List<Finding> findings = detector.detect(file, rule);

        assertTrue(findings.isEmpty(), "Should not flag SHA-256 or AES/GCM");
    }
}
