package com.codesec.engine.detector.impl;

import com.codesec.engine.detector.Detector;
import com.codesec.engine.model.Finding;
import com.codesec.engine.parser.ParsedFile;
import com.codesec.engine.rule.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects Go command injection: {@code exec.Command} / {@code exec.CommandContext}
 * calls where at least one argument is a variable, concatenation, or function return
 * value rather than a hardcoded string literal.
 * <p>
 * Pattern: {@code exec\\.Command(Context)?\(...}) where any argument to the call
 * is not a pure string literal.
 * </p>
 */
public final class GoCommandInjectionDetector implements Detector {
    private static final Logger log = LoggerFactory.getLogger(GoCommandInjectionDetector.class);

    // Match exec.Command / exec.CommandContext calls
    private static final Pattern COMMAND_CALL_PATTERN = Pattern.compile(
        "exec\\.Command(?:Context)?\\(([^)]*)\\)",
        Pattern.MULTILINE
    );

    // A "literal" argument is a double-quoted Go string (no variable interpolation inside)
    private static final Pattern LITERAL_ARG = Pattern.compile(
        "^\""   // starts with a double-quote
    );

    @Override
    public List<Finding> detect(ParsedFile file, Rule rule) {
        List<Finding> findings = new ArrayList<>();
        String source = file.sourceCode();
        Matcher matcher = COMMAND_CALL_PATTERN.matcher(source);

        while (matcher.find()) {
            String argsSection = matcher.group(1);
            if (argsSection == null || argsSection.isBlank()) {
                continue;
            }

            // Split by commas, respecting that Go strings can contain commas
            List<String> args = splitArgsRespectingStrings(argsSection);

            // Check each argument — if any is not a plain string literal, flag it
            boolean hasNonLiteral = false;
            for (String arg : args) {
                String trimmed = arg.trim();
                if (trimmed.isEmpty()) continue;
                // Skip the command name (first arg is often "bash", "sh", etc.)
                // But fully check all args beyond the first for non-literal usage
                boolean isLiteral = trimmed.startsWith("\"") && trimmed.endsWith("\"");
                // Also consider backtick raw strings as literal
                boolean isRawLiteral = trimmed.startsWith("`") && trimmed.endsWith("`");
                if (!isLiteral && !isRawLiteral) {
                    hasNonLiteral = true;
                    break;
                }
            }

            if (hasNonLiteral) {
                int lineNumber = lineNumberOf(source, matcher.start());
                String snippet = extractLine(source, matcher.start());
                findings.add(buildFinding(file, rule, lineNumber, snippet));
            }
        }

        return findings;
    }

    /**
     * Splits the argument section of a Go function call, respecting string
     * boundaries so commas inside strings are not treated as separators.
     */
    private static List<String> splitArgsRespectingStrings(String argsSection) {
        List<String> args = new ArrayList<>();
        int depth = 0;
        boolean inDoubleQuote = false;
        boolean inBacktick = false;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < argsSection.length(); i++) {
            char c = argsSection.charAt(i);

            if (inBacktick) {
                if (c == '`') inBacktick = false;
                current.append(c);
                continue;
            }
            if (inDoubleQuote) {
                if (c == '"' && (i == 0 || argsSection.charAt(i - 1) != '\\')) {
                    inDoubleQuote = false;
                }
                current.append(c);
                continue;
            }

            switch (c) {
                case '`' -> { inBacktick = true; current.append(c); }
                case '"' -> { inDoubleQuote = true; current.append(c); }
                case '(' -> { depth++; current.append(c); }
                case ')' -> { depth--; current.append(c); }
                case ',' -> {
                    if (depth == 0) {
                        args.add(current.toString());
                        current = new StringBuilder();
                    } else {
                        current.append(c);
                    }
                }
                default -> current.append(c);
            }
        }
        if (!current.isEmpty()) {
            args.add(current.toString());
        }
        return args;
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
            .description("Go command injection: exec.Command called with non-literal arguments at line " + lineNumber)
            .fixSuggestion(rule.fixDescription())
            .cwe(rule.cwe())
            .exploitability("potentially_exploitable")
            .exploitReason("exec.Command arguments contain variable or expression rather than hardcoded string")
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

    private static final int CONTEXT_LINES = 5;

    private static String extractSnippetWithContext(String source, int lineNum) {
        String[] lines = source.split("\n", -1);
        int startIdx = Math.max(0, lineNum - 1 - CONTEXT_LINES);
        int endIdx = Math.min(lines.length, lineNum + CONTEXT_LINES);
        return String.join("\n", Arrays.copyOfRange(lines, startIdx, endIdx));
    }

    private static String extractLine(String source, int position) {
        int lineNum = lineNumberOf(source, position);
        return extractSnippetWithContext(source, lineNum);
    }
}
