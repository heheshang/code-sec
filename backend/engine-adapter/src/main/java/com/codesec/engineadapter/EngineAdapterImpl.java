package com.codesec.engineadapter;

import com.codesec.engine.Engine;
import com.codesec.engine.model.Finding;
import com.codesec.engine.rule.RuleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.nio.file.Path;
import java.util.*;

@Component
public class EngineAdapterImpl implements EngineAdapter {
    private static final Logger log = LoggerFactory.getLogger(EngineAdapterImpl.class);
    private final RuleRegistry ruleRegistry;
    private long scanCount = 0;

    public EngineAdapterImpl(RuleRegistry ruleRegistry) {
        this.ruleRegistry = ruleRegistry;
    }

    @Override
    public EngineScanResult scan(ScanRequest request) {
        long start = System.currentTimeMillis();
        try {
            Engine engine = Engine.create(ruleRegistry);
            List<Finding> findings = engine.scan(request.sourceRoot());
            long duration = System.currentTimeMillis() - start;
            scanCount++;
            log.info("Full scan complete: {} findings in {}ms", findings.size(), duration);
            return new EngineScanResult(UUID.randomUUID().toString(), findings, duration);
        } catch (Exception e) {
            log.error("Full scan failed for repo {}", request.repoId(), e);
            long duration = System.currentTimeMillis() - start;
            return new EngineScanResult(UUID.randomUUID().toString(), List.of(), duration);
        }
    }

    @Override
    public EngineScanResult scanFiles(Path sourceRoot, List<String> relativeFiles) {
        long start = System.currentTimeMillis();
        if (relativeFiles == null || relativeFiles.isEmpty()) {
            return new EngineScanResult(UUID.randomUUID().toString(), List.of(), 0);
        }
        try {
            Engine engine = Engine.create(ruleRegistry);
            List<Finding> allFindings = engine.scan(sourceRoot);
            // Filter findings to only those in relativeFiles
            List<Finding> filtered = allFindings.stream()
                .filter(f -> relativeFiles.stream().anyMatch(rf -> f.filePath().contains(rf)))
                .toList();
            long duration = System.currentTimeMillis() - start;
            scanCount++;
            log.info("Incremental scan complete: {} findings (filtered from {}) in {}ms",
                filtered.size(), allFindings.size(), duration);
            return new EngineScanResult(UUID.randomUUID().toString(), filtered, duration);
        } catch (Exception e) {
            log.error("Incremental scan failed for {}", sourceRoot, e);
            long duration = System.currentTimeMillis() - start;
            return new EngineScanResult(UUID.randomUUID().toString(), List.of(), duration);
        }
    }

    @Override
    public EngineHealth health() {
        EngineHealth h = new EngineHealth();
        h.setOk(true);
        h.setEngineVersion("1.0.0-SNAPSHOT");
        h.setScanCount(scanCount);
        return h;
    }
}
