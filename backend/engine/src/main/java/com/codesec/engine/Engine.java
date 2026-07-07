package com.codesec.engine;

import com.codesec.engine.config.JudgeConfig;
import com.codesec.engine.model.Finding;
import com.codesec.engine.parser.AstParser;
import com.codesec.engine.parser.languages.GoLanguage;
import com.codesec.engine.parser.languages.JavaLanguage;
import com.codesec.engine.parser.languages.PythonLanguage;
import com.codesec.engine.rule.RuleRegistry;
import com.codesec.engine.detector.Detector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class Engine {
    private static final Logger log = LoggerFactory.getLogger(Engine.class);

    private final ScanOrchestrator orchestrator;

    private Engine(EngineConfig config) {
        this.orchestrator = new ScanOrchestrator(config);
    }

    public static Engine create(RuleRegistry ruleRegistry) {
        Map<String, AstParser> parsers = Map.of(
            "java", new JavaLanguage(),
            "go", new GoLanguage(),
            "python", new PythonLanguage()
        );
        List<String> excludes = List.of(
            "**/test/**",
            "**/*Test.java",
            "**/target/**",
            "**/node_modules/**",
            "**/.git/**"
        );
        EngineConfig config = new EngineConfig(ruleRegistry, parsers, excludes, JudgeConfig.defaults());
        return new Engine(config);
    }

    public void registerDetector(String ruleId, Detector detector) {
        orchestrator.registerDetector(ruleId, detector);
    }

    public List<Finding> scan(Path root) throws IOException {
        return orchestrator.scan(root);
    }

    public void shutdown() {
        log.debug("Engine shutdown complete");
    }
}
