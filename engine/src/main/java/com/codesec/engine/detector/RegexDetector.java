package com.codesec.engine.detector;

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

public class RegexDetector implements Detector {
    private static final Logger log = LoggerFactory.getLogger(RegexDetector.class);

    @Override
    public List<Finding> detect(ParsedFile file, Rule rule) {
        String patternStr = rule.detectionPattern();
        if (patternStr == null || patternStr.isBlank()) {
            log.warn("No regex pattern defined for rule {}", rule.id());
            return List.of();
        }

        Pattern pattern = Pattern.compile(patternStr, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(file.sourceCode());
        List<Finding> findings = new ArrayList<>();

        while (matcher.find()) {
            int lineNumber = lineNumberOf(file.sourceCode(), matcher.start());
            String snippet = extractSnippet(file.sourceCode(), matcher.start(), matcher.end());
            Finding finding = buildFinding(file, rule, lineNumber, snippet, matcher.group());
            if (isValidMatch(finding, file, rule, matcher)) {
                findings.add(finding);
            }
        }

        return findings;
    }

    protected Finding buildFinding(ParsedFile file, Rule rule, int lineNumber, String snippet, String matchText) {
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
            .description(rule.name() + " detected at line " + lineNumber)
            .fixSuggestion(rule.fixDescription())
            .cwe(rule.cwe())
            .exploitability("potentially_exploitable")
            .exploitReason("Pattern match: " + rule.detectionPattern())
            .engineRaw(Map.of("detection_type", "regex", "pattern", rule.detectionPattern()))
            .discoveredAt(Instant.now())
            .build();
    }

    protected boolean isValidMatch(Finding finding, ParsedFile file, Rule rule, Matcher matcher) {
        return true;
    }

    protected static int lineNumberOf(String source, int position) {
        int line = 1;
        for (int i = 0; i < position && i < source.length(); i++) {
            if (source.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    protected static String extractSnippet(String source, int start, int end) {
        int lineStart = start;
        while (lineStart > 0 && source.charAt(lineStart - 1) != '\n') {
            lineStart--;
        }
        int lineEnd = end;
        while (lineEnd < source.length() && source.charAt(lineEnd) != '\n') {
            lineEnd++;
        }
        return source.substring(lineStart, lineEnd).trim();
    }
}
