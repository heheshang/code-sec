package com.codesec.engine.detector;

import com.codesec.engine.model.Finding;
import com.codesec.engine.parser.ParsedFile;
import com.codesec.engine.rule.Rule;
import com.github.javaparser.ast.CompilationUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class AstDetector implements Detector {
    protected final Logger log = LoggerFactory.getLogger(getClass());

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
        return Finding.builder()
            .vulnId(UUID.randomUUID().toString())
            .engine("self_sast")
            .ruleId(rule.id())
            .title(rule.name())
            .severity(rule.severity())
            .filePath(file.relativePath())
            .lineStart(match.lineStart())
            .lineEnd(match.lineEnd())
            .codeSnippet(match.codeSnippet())
            .description(match.description())
            .fixSuggestion(rule.fixDescription())
            .cwe(rule.cwe())
            .exploitability("potentially_exploitable")
            .exploitReason(match.exploitReason())
            .engineRaw(Map.of("detection_type", "ast", "pattern", rule.detectionPattern()))
            .discoveredAt(Instant.now())
            .build();
    }
}
