package com.codesec.engine.detector.impl;

import com.codesec.engine.detector.Detector;
import com.codesec.engine.model.Finding;
import com.codesec.engine.parser.ParsedFile;
import com.codesec.engine.rule.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects unsafe {@code eval()} calls in Python.
 * <p>
 * Flags calls to {@code eval(...)} where the argument is <em>not</em> a
 * hardcoded string literal — i.e. where user-controlled input may be evaluated.
 * Calls to {@code ast.literal_eval} are explicitly excluded.
 * </p>
 */
public final class PythonUnsafeEvalDetector implements Detector {
    private static final Logger log = LoggerFactory.getLogger(PythonUnsafeEvalDetector.class);

    // Match eval(...) but not ast.literal_eval(...)
    private static final Pattern EVAL_CALL = Pattern.compile(
        "(?<!literal_)eval\\(\\s*([^)]+)\\s*\\)",
        Pattern.MULTILINE
    );

    // A hardcoded string literal is enclosed in single or double quotes
    private static final Pattern LITERAL_ARG = Pattern.compile(
        "^\""   // starts with double quote
    );
    private static final Pattern LITERAL_ARG_SINGLE = Pattern.compile(
        "^'"   // starts with single quote
    );

    @Override
    public List<Finding> detect(ParsedFile file, Rule rule) {
        List<Finding> findings = new ArrayList<>();
        String source = file.sourceCode();
        Matcher matcher = EVAL_CALL.matcher(source);

        while (matcher.find()) {
            String arg = matcher.group(1);
            if (arg == null || arg.isBlank()) {
                continue;
            }
            String trimmed = arg.trim();

            // If the argument is a hardcoded string literal, it's not unsafe
            boolean isLiteral = (trimmed.startsWith("\"") && trimmed.length() > 1)
                || (trimmed.startsWith("'") && trimmed.endsWith("'"));

            if (!isLiteral) {
                int lineNumber = lineNumberOf(source, matcher.start());
                String snippet = extractLine(source, matcher.start());
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
            .description("Unsafe eval() with potentially user-controlled input at line " + lineNumber)
            .fixSuggestion(rule.fixDescription())
            .cwe(rule.cwe())
            .exploitability("potentially_exploitable")
            .exploitReason("eval() called with non-literal argument — possible code injection")
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
