package com.codesec.engine.detector.impl;

import com.codesec.engine.detector.Detector;
import com.codesec.engine.model.Finding;
import com.codesec.engine.parser.ParsedFile;
import com.codesec.engine.rule.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects Python SQL injection patterns in {@code .execute()} / {@code .executemany()} calls.
 * <p>Three detection strategies:</p>
 * <ol>
 *   <li><b>Inline concatenation:</b> direct string concat in execute() args</li>
 *   <li><b>F-string:</b> f-string interpolation in execute() args</li>
 *   <li><b>Tainted variable:</b> a variable passed to execute() that was previously assigned via
 *       string concatenation or f-string (deterministic per-file taint marking)</li>
 * </ol>
 */
public final class PythonSqlInjectionDetector implements Detector {
    private static final Logger log = LoggerFactory.getLogger(PythonSqlInjectionDetector.class);

    // .execute() / .executemany() call
    private static final Pattern EXECUTE_CALL = Pattern.compile(
        "\\.(?:execute|executemany)\\(\\s*([^)]+)\\s*\\)",
        Pattern.MULTILINE
    );

    // Direct concatenation (+) or f-string in execute args
    private static final Pattern INLINE_CONCAT = Pattern.compile(
        "\"[^\"]*\"\\s*\\+|'[^']*'\\s*\\+|f\"[^\"]*\"",
        Pattern.MULTILINE
    );

    // Variable assignment with concat or f-string on the RHS
    private static final Pattern CONCAT_ASSIGNMENT = Pattern.compile(
        "^\s*(\\w+)\\s*=\\s*(?:\"[^\"]*\".*\\+.*|'[^']*'.*\\+.*|f\"[^\"]*\")",
        Pattern.MULTILINE
    );

    @Override
    public List<Finding> detect(ParsedFile file, Rule rule) {
        List<Finding> findings = new ArrayList<>();
        String source = file.sourceCode();

        // Phase 1: mark tainted variables (assigned via string concat or f-string)
        Set<String> taintedVars = new HashSet<>();
        Matcher assignMatcher = CONCAT_ASSIGNMENT.matcher(source);
        while (assignMatcher.find()) {
            String varName = assignMatcher.group(1);
            if (varName != null && !varName.isBlank()) {
                taintedVars.add(varName);
            }
        }

        // Phase 2: inspect execute() calls
        Matcher callMatcher = EXECUTE_CALL.matcher(source);
        while (callMatcher.find()) {
            String argsSection = callMatcher.group(1);
            if (argsSection == null || argsSection.isBlank()) continue;
            String trimmed = argsSection.trim();

            boolean vulnerable = INLINE_CONCAT.matcher(trimmed).find()
                || taintedVars.contains(trimmed);

            if (vulnerable) {
                int lineNumber = lineNumberOf(source, callMatcher.start());
                String snippet = extractLine(source, callMatcher.start());
                findings.add(buildFinding(file, rule, lineNumber, snippet));
            }
        }

        return findings;
    }

    private static Finding buildFinding(ParsedFile file, Rule rule, int lineNumber, String snippet) {
        return Finding.builder()
            .vulnId(UUID.randomUUID().toString())
            .engine("self_sast")
            .ruleId(rule.id())
            .title(rule.name())
            .severity(rule.severity())
            .filePath(file.relativePath())
            .lineStart(lineNumber)
            .lineEnd(lineNumber)
            .codeSnippet(snippet)
            .description("Python SQL injection: execute() with string concatenation at line " + lineNumber)
            .fixSuggestion(rule.fixDescription())
            .cwe(rule.cwe())
            .exploitability("potentially_exploitable")
            .exploitReason("SQL query constructed via string concatenation / f-string")
            .engineRaw(Map.of("detection_type", "source", "pattern", rule.id()))
            .discoveredAt(Instant.now())
            .build();
    }

    private static int lineNumberOf(String source, int position) {
        int line = 1;
        for (int i = 0; i < position && i < source.length(); i++) {
            if (source.charAt(i) == '\n') line++;
        }
        return line;
    }

    private static String extractLine(String source, int position) {
        int lineStart = position;
        while (lineStart > 0 && source.charAt(lineStart - 1) != '\n') lineStart--;
        int lineEnd = position;
        while (lineEnd < source.length() && source.charAt(lineEnd) != '\n') lineEnd++;
        return source.substring(lineStart, lineEnd).trim();
    }
}
