package com.codesec.engine.detector;

import com.codesec.engine.model.Finding;
import com.codesec.engine.parser.ParsedFile;
import com.codesec.engine.rule.Rule;
import com.github.javaparser.ast.CompilationUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class AstDetector implements Detector {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    /** Number of surrounding lines to include in code snippet for context. */
    protected static final int CONTEXT_LINES = 5;

    public record AstMatch(
        int lineStart,
        int lineEnd,
        String codeSnippet,
        String description,
        String exploitReason
    ) {}

    @Override
    public List<Finding> detect(ParsedFile file, Rule rule) {
        if (!(file.ast() instanceof CompilationUnit cu)) {
            log.warn("AST is not a CompilationUnit for rule {}", rule.id());
            return List.of();
        }
        List<AstMatch> matches = findMatches(cu, file, rule);
        return matches.stream()
            .map(match -> buildFinding(match, file, rule))
            .toList();
    }

    protected abstract List<AstMatch> findMatches(CompilationUnit cu, ParsedFile file, Rule rule);

    protected Finding buildFinding(AstMatch match, ParsedFile file, Rule rule) {
        String snippet = extractContextSnippet(file.sourceCode(), match.lineStart(), match.lineEnd());
        return Finding.builder()
            .vulnId(UUID.randomUUID().toString())
            .engine("self_sast")
            .ruleId(rule.id())
            .title(rule.name())
            .severity(rule.severity())
            .filePath(file.relativePath())
            .lineStart(match.lineStart())
            .lineEnd(match.lineEnd())
            .codeSnippet(snippet)
            .description(match.description())
            .fixSuggestion(rule.fixDescription())
            .cwe(rule.cwe())
            .exploitability("potentially_exploitable")
            .exploitReason(match.exploitReason())
            .engineRaw(Map.of("detection_type", "ast", "pattern", rule.detectionPattern()))
            .discoveredAt(Instant.now())
            .build();
    }

    /**
     * Extract a code snippet centered on the given line range with surrounding context lines.
     * Falls back to the original match snippet when full source is unavailable.
     */
    protected static String extractContextSnippet(String source, int lineStart, int lineEnd) {
        if (source == null || source.isEmpty()) return "";
        String[] lines = source.split("\n", -1);
        int startIdx = Math.max(0, lineStart - 1 - CONTEXT_LINES);
        int endIdx = Math.min(lines.length, lineEnd + CONTEXT_LINES);
        return String.join("\n", Arrays.copyOfRange(lines, startIdx, endIdx));
    }
}
