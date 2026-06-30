package com.codesec.engine.detector.impl;

import com.codesec.engine.model.Finding;
import com.codesec.engine.parser.ParsedFile;
import com.codesec.engine.rule.Rule;
import com.codesec.engine.rule.RuleLoader;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class XssDetectorTest {

    private static final XssDetector detector = new XssDetector();

    @Test
    void detect_shouldFindXssInResponseWriter() throws Exception {
        String code = """
            package test;
            import javax.servlet.http.*;
            import java.io.*;
            public class Test {
                public void search(HttpServletRequest req, HttpServletResponse res) throws IOException {
                    String q = req.getParameter("q");
                    res.getWriter().println("<h1>Results: " + q + "</h1>");
                }
            }
            """;

        Rule rule = RuleLoader.loadFromClasspath("rules/java/xss-001.yml");
        ParsedFile file = parseCode("Test.java", code);
        List<Finding> findings = detector.detect(file, rule);

        assertFalse(findings.isEmpty(), "Should detect XSS in response.getWriter().println()");
        Finding f = findings.get(0);
        assertEquals("java/xss-001", f.ruleId());
        assertEquals("high", f.severity());
        assertEquals("CWE-79", f.cwe());
    }

    @Test
    void detect_shouldFindXssInPrint() throws Exception {
        String code = """
            package test;
            import javax.servlet.http.*;
            import java.io.*;
            public class Test {
                public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
                    String name = req.getParameter("name");
                    res.getWriter().print(name);
                }
            }
            """;

        Rule rule = RuleLoader.loadFromClasspath("rules/java/xss-001.yml");
        ParsedFile file = parseCode("Test.java", code);
        List<Finding> findings = detector.detect(file, rule);

        assertFalse(findings.isEmpty(), "Should detect XSS in response.getWriter().print()");
    }

    @Test
    void detect_shouldNotFlagSafeStaticOutput() throws Exception {
        String code = """
            package test;
            import javax.servlet.http.*;
            import java.io.*;
            public class Test {
                public void status(HttpServletResponse res) throws IOException {
                    res.getWriter().println("Server is running");
                }
            }
            """;

        Rule rule = RuleLoader.loadFromClasspath("rules/java/xss-001.yml");
        ParsedFile file = parseCode("Test.java", code);
        List<Finding> findings = detector.detect(file, rule);

        assertTrue(findings.isEmpty(), "Should not flag static output");
    }

    @Test
    void detect_shouldNotFlagResponseWithoutUntrustedInput() throws Exception {
        String code = """
            package test;
            import javax.servlet.http.*;
            import java.io.*;
            public class Test {
                public void showError(HttpServletResponse res, String errorCode) throws IOException {
                    res.getWriter().println("Error: " + errorCode);
                }
            }
            """;

        Rule rule = RuleLoader.loadFromClasspath("rules/java/xss-001.yml");
        ParsedFile file = parseCode("Test.java", code);
        List<Finding> findings = detector.detect(file, rule);

        assertTrue(findings.isEmpty(), "Should not flag response without request.getParameter()");
    }

    private static ParsedFile parseCode(String fileName, String code) {
        var cu = com.github.javaparser.StaticJavaParser.parse(code);
        return new ParsedFile(Path.of(fileName), "java", code, cu);
    }
}
