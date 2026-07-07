package com.codesec.engine;

import com.codesec.engine.config.JudgeConfig;
import com.codesec.engine.parser.AstParser;
import com.codesec.engine.rule.RuleRegistry;

import java.util.List;
import java.util.Map;

public record EngineConfig(
    RuleRegistry ruleRegistry,
    Map<String, AstParser> parsersByLanguage,
    List<String> excludePatterns,
    JudgeConfig judgeConfig
) {}
