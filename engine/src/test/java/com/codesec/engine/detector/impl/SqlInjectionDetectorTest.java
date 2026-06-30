package com.codesec.engine.detector.impl;

import com.codesec.engine.model.Finding;
import com.codesec.engine.parser.ParsedFile;
import com.codesec.engine.parser.languages.JavaLanguage;
import com.codesec.engine.rule.Rule;
import com.codesec.engine.rule.RuleLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SqlInjectionDetectorTest {

    private static final SqlInjectionDetector detector = new SqlInjectionDetector();

    @Test
    void detect_shouldFindStringConcatInExecuteQuery() throws Exception {
        String code = """
            package test;
            import java.sql.*;
            public class Test {
                public void findUser(Connection conn, String userId) throws Exception {
                    String sql = "SELECT * FROM users WHERE id = " + userId;
                    Statement stmt = conn.createStatement();
                    stmt.executeQuery(sql);
                }
            }
            """;

        Rule rule = RuleLoader.loadFromClasspath("rules/java/sql-injection-001.yml");
        ParsedFile file = parseCode("Test.java", code);
        List<Finding> findings = detector.detect(file, rule);

        assertFalse(findings.isEmpty(), "Should detect SQL injection");
        Finding f = findings.get(0);
        assertEquals("java/sql-injection-001", f.ruleId());
        assertEquals("high", f.severity());
        assertEquals("CWE-89", f.cwe());
    }

    @Test
    void detect_shouldFindStringConcatInExecuteUpdate() throws Exception {
        String code = """
            package test;
            import java.sql.*;
            public class Test {
                public void updateUser(Connection conn, String name, int id) throws Exception {
                    String sql = "UPDATE users SET name = '" + name + "' WHERE id = " + id;
                    Statement stmt = conn.createStatement();
                    stmt.executeUpdate(sql);
                }
            }
            """;

        Rule rule = RuleLoader.loadFromClasspath("rules/java/sql-injection-001.yml");
        ParsedFile file = parseCode("Test.java", code);
        List<Finding> findings = detector.detect(file, rule);

        assertFalse(findings.isEmpty(), "Should detect SQL injection in executeUpdate");
    }

    @Test
    void detect_shouldNotFlagPreparedStatement() throws Exception {
        String code = """
            package test;
            import java.sql.*;
            public class Test {
                public void findUserSafe(Connection conn, int userId) throws Exception {
                    String sql = "SELECT * FROM users WHERE id = ?";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setInt(1, userId);
                    ps.executeQuery();
                }
            }
            """;

        Rule rule = RuleLoader.loadFromClasspath("rules/java/sql-injection-001.yml");
        ParsedFile file = parseCode("Test.java", code);
        List<Finding> findings = detector.detect(file, rule);

        assertTrue(findings.isEmpty(), "Should not flag parameterized queries");
    }

    @Test
    void detect_shouldNotFlagSafeLiteralQuery() throws Exception {
        String code = """
            package test;
            import java.sql.*;
            public class Test {
                public void listUsers(Connection conn) throws Exception {
                    Statement stmt = conn.createStatement();
                    stmt.executeQuery("SELECT * FROM users");
                }
            }
            """;

        Rule rule = RuleLoader.loadFromClasspath("rules/java/sql-injection-001.yml");
        ParsedFile file = parseCode("Test.java", code);
        List<Finding> findings = detector.detect(file, rule);

        assertTrue(findings.isEmpty(), "Should not flag literal SQL without concatenation");
    }

    private static ParsedFile parseCode(String fileName, String code) {
        var cu = com.github.javaparser.StaticJavaParser.parse(code);
        return new ParsedFile(Path.of(fileName), "java", code, cu);
    }
}
