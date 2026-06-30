package com.codesec.gitlab.dev;

import com.codesec.engine.Engine;
import com.codesec.engine.model.Finding;
import com.codesec.engine.rule.RuleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Development/stub implementation of EngineAdapter.
 *
 * <p><b>TEMPORARY:</b> Replace with real EngineAdapter implementation when
 * E-S2-CRITICAL delivers the production adapter. This class provides
 * functional correctness for development and testing.
 *
 * @deprecated Replace with real EngineAdapter per E-S2-CRITICAL.
 *     Scheduled removal: M2 engine integration sprint.
 */
@Deprecated(forRemoval = true)
@Component
public class DevEngineAdapter {

    private static final Logger log = LoggerFactory.getLogger(DevEngineAdapter.class);

    private final Engine engine;

    public DevEngineAdapter() {
        RuleRegistry ruleRegistry = new RuleRegistry();
        try {
            ruleRegistry.loadFromClasspath("rules/java");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load rules", e);
        }
        this.engine = Engine.create(ruleRegistry);
    }

    /**
     * Scans a set of relative files within a work directory.
     *
     * @param relativeFiles list of relative file paths to scan
     * @return findings from the scan
     */
    public List<Finding> scanFiles(List<String> relativeFiles) {
        Path workDir = createTempWorkDir();
        try {
            return scanFiles(workDir, relativeFiles);
        } finally {
            cleanupWorkDir(workDir);
        }
    }

    /**
     * Full signature matching E-S2-CRITICAL § 3.5.3.
     */
    public List<Finding> scanFiles(Path sourceRoot, List<String> relativeFiles) {
        if (relativeFiles == null || relativeFiles.isEmpty()) {
            return List.of();
        }

        log.info("DevEngineAdapter scanning {} files in {}", relativeFiles.size(), sourceRoot);

        try {
            List<Finding> allFindings = engine.scan(sourceRoot);
            return allFindings.stream()
                .filter(f -> relativeFiles.stream()
                    .anyMatch(rf -> f.filePath() != null && f.filePath().endsWith(rf)))
                .toList();
        } catch (IOException e) {
            log.error("DevEngineAdapter scan failed: {}", e.getMessage(), e);
            return List.of();
        }
    }

    private Path createTempWorkDir() {
        try {
            return Files.createTempDirectory("gitlab-scan-");
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temp work dir", e);
        }
    }

    private void cleanupWorkDir(Path workDir) {
        try {
            if (Files.exists(workDir)) {
                try (Stream<Path> walk = Files.walk(workDir)) {
                    walk.sorted(java.util.Comparator.reverseOrder())
                        .forEach(p -> {
                            try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                        });
                }
            }
        } catch (IOException e) {
            log.warn("Failed to cleanup work dir: {}", workDir, e);
        }
    }
}
