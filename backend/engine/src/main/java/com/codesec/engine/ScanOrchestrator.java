package com.codesec.engine;

import com.codesec.engine.detector.Detector;
import com.codesec.engine.detector.impl.HardcodedPasswordDetector;
import com.codesec.engine.detector.impl.GoCommandInjectionDetector;
import com.codesec.engine.detector.impl.PythonSqlInjectionDetector;
import com.codesec.engine.detector.impl.PythonUnsafeEvalDetector;
import com.codesec.engine.detector.impl.SqlInjectionDetector;
import com.codesec.engine.detector.impl.WeakCryptoDetector;
import com.codesec.engine.detector.impl.XssDetector;
import com.codesec.engine.judge.CallGraphBuilder;
import com.codesec.engine.judge.ExploitabilityJudger;
import com.codesec.engine.judge.ProjectCallGraph;
import com.codesec.engine.judge.ProtectionRule;
import com.codesec.engine.model.Finding;
import com.codesec.engine.parser.AstParser;
import com.codesec.engine.parser.ParsedFile;
import com.codesec.engine.rule.Rule;
import com.codesec.engine.util.PathMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class ScanOrchestrator {
    private static final Logger log = LoggerFactory.getLogger(ScanOrchestrator.class);

    private final EngineConfig config;
    private final Map<String, Detector> detectorsByRuleId = new LinkedHashMap<>();

    public ScanOrchestrator(EngineConfig config) {
        this.config = config;
        registerDetectors();
    }

    private void registerDetectors() {
        detectorsByRuleId.put("java/sql-injection-001", new SqlInjectionDetector());
        detectorsByRuleId.put("java/hardcoded-password-001", new HardcodedPasswordDetector());
        detectorsByRuleId.put("java/xss-001", new XssDetector());
        detectorsByRuleId.put("java/weak-crypto-001", new WeakCryptoDetector());
        detectorsByRuleId.put("go/hardcoded-password-001", new HardcodedPasswordDetector());
        detectorsByRuleId.put("go/command-injection-001", new GoCommandInjectionDetector());
        detectorsByRuleId.put("python/sql-injection-001", new PythonSqlInjectionDetector());
        detectorsByRuleId.put("python/unsafe-eval-001", new PythonUnsafeEvalDetector());
    }

    public void registerDetector(String ruleId, Detector detector) {
        detectorsByRuleId.put(ruleId, detector);
    }

    public List<Finding> scan(Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException("Scan root must be a directory: " + root);
        }

        List<Finding> allFindings = new ArrayList<>();
        List<ParsedFile> parsedFiles = new ArrayList<>();

        try (Stream<Path> fileStream = Files.walk(root)) {
            List<Path> sourceFiles = fileStream
                .filter(Files::isRegularFile)
                .filter(path -> !PathMatcher.isExcluded(path, config.excludePatterns()))
                .toList();

            log.info("Scanning {} source files in {}", sourceFiles.size(), root);

            for (Path filePath : sourceFiles) {
                Optional<AstParser> parser = findParser(filePath);
                if (parser.isEmpty()) {
                    log.debug("No parser for file: {}", filePath);
                    continue;
                }

                ParsedFile parsedFile;
                try {
                    parsedFile = parser.get().parse(filePath);
                } catch (Exception e) {
                    log.warn("Failed to parse {}: {}", filePath, e.getMessage());
                    continue;
                }

                parsedFiles.add(parsedFile);

                String language = parsedFile.language();
                List<Rule> rules = config.ruleRegistry().findByLanguage(language);

                for (Rule rule : rules) {
                    Detector detector = detectorsByRuleId.get(rule.id());
                    if (detector != null) {
                        try {
                            List<Finding> findings = detector.detect(parsedFile, rule);
                            allFindings.addAll(findings);
                        } catch (Exception e) {
                            log.error("Detector error for rule {} on {}: {}",
                                rule.id(), filePath, e.getMessage(), e);
                        }
                    } else {
                        log.debug("No detector registered for rule: {}", rule.id());
                    }
                }
            }
        }

        if (config.judgeConfig().enabled() && !allFindings.isEmpty()) {
            judgeFindings(allFindings, parsedFiles);
        }

        log.info("Scan complete. Found {} findings in {}", allFindings.size(), root);
        return allFindings;
    }

    ProjectCallGraph buildCallGraph(List<ParsedFile> files) {
        CallGraphBuilder builder = new CallGraphBuilder();
        return builder.build(files);
    }

    void judgeFindings(List<Finding> findings, List<ParsedFile> parsedFiles) {
        if (findings.isEmpty() || parsedFiles.isEmpty()) {
            return;
        }

        try {
            ProjectCallGraph graph = buildCallGraph(parsedFiles);
            Map<String, ParsedFile> parsedFileMap = buildParsedFileMap(parsedFiles);
            Map<Path, String> sourceMap = buildSourceMap(parsedFiles);
            List<ProtectionRule> rules = safeLoadProtectionRules();

            ExploitabilityJudger judger = new ExploitabilityJudger(
                graph, parsedFileMap, rules, config.judgeConfig().perFileTimeout());
            try {
                judger.judgeBatch(findings, sourceMap);
                log.info("Exploitability judgment complete for {} findings", findings.size());
            } finally {
                judger.shutdown();
            }
        } catch (Exception e) {
            int affectedCount = findings != null ? findings.size() : 0;
            log.error("Exploitability judgment failed; {} findings remain POTENTIALLY_EXPLOITABLE. Check judger logs above for details.", affectedCount, e);
        }
    }

    private Optional<AstParser> findParser(Path filePath) {
        for (AstParser parser : config.parsersByLanguage().values()) {
            if (parser.supports(filePath)) {
                return Optional.of(parser);
            }
        }
        return Optional.empty();
    }

    private static Map<String, ParsedFile> buildParsedFileMap(List<ParsedFile> files) {
        Map<String, ParsedFile> map = new HashMap<>();
        for (ParsedFile file : files) {
            map.put(file.relativePath(), file);
        }
        return Collections.unmodifiableMap(map);
    }

    private static Map<Path, String> buildSourceMap(List<ParsedFile> files) {
        Map<Path, String> map = new HashMap<>();
        for (ParsedFile file : files) {
            map.put(file.path(), file.sourceCode());
        }
        return Collections.unmodifiableMap(map);
    }

    private static List<ProtectionRule> safeLoadProtectionRules() {
        try {
            return ExploitabilityJudger.loadDefaultProtectionRules();
        } catch (Exception e) {
            log.warn("Failed to load protection rules; framework protection detection disabled", e);
            return List.of();
        }
    }
}
