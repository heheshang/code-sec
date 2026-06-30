package com.codesec.engine.rule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class RuleLoader {
    private static final Logger log = LoggerFactory.getLogger(RuleLoader.class);
    private static final Yaml YAML = new Yaml();

    private RuleLoader() {}

    public static List<Rule> loadFromDirectory(Path ruleDir) throws IOException {
        if (!Files.isDirectory(ruleDir)) {
            throw new IllegalArgumentException("Rule directory does not exist: " + ruleDir);
        }

        List<Rule> rules = new ArrayList<>();
        try (Stream<Path> files = Files.walk(ruleDir)) {
            files.filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".yml") || path.toString().endsWith(".yaml"))
                .forEach(path -> {
                    try {
                        rules.add(loadFromFile(path));
                    } catch (IOException e) {
                        log.error("Failed to load rule from {}", path, e);
                    }
                });
        }
        log.info("Loaded {} rules from {}", rules.size(), ruleDir);
        return rules;
    }

    public static Rule loadFromFile(Path ruleFile) throws IOException {
        Map<String, Object> data;
        try (InputStream in = Files.newInputStream(ruleFile)) {
            data = YAML.load(in);
        }
        return parseRule(data);
    }

    @SuppressWarnings("unchecked")
    public static List<Rule> loadFromClasspathDirectory(String classpathPrefix) throws IOException {
        List<Rule> rules = new ArrayList<>();
        ClassLoader cl = RuleLoader.class.getClassLoader();

        String[] ruleNames = {
            "sql-injection-001.yml",
            "hardcoded-password-001.yml",
            "xss-001.yml",
            "weak-crypto-001.yml"
        };
        for (String name : ruleNames) {
            String resourcePath = classpathPrefix + "/" + name;
            try (InputStream in = cl.getResourceAsStream(resourcePath)) {
                if (in != null) {
                    Map<String, Object> data = (Map<String, Object>) YAML.load(in);
                    rules.add(parseRule(data));
                }
            }
        }
        log.info("Loaded {} rules from classpath:{}", rules.size(), classpathPrefix);
        return rules;
    }

    public static Rule loadFromClasspath(String resourcePath) throws IOException {
        Map<String, Object> data;
        try (InputStream in = RuleLoader.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Rule resource not found: " + resourcePath);
            }
            data = YAML.load(in);
        }
        return parseRule(data);
    }

    @SuppressWarnings("unchecked")
    private static Rule parseRule(Map<String, Object> data) {
        String id = getString(data, "id");
        String name = getString(data, "name");
        String severity = getString(data, "severity");
        String cwe = getString(data, "cwe");
        List<String> languages = getStringList(data, "languages");
        String engine = getString(data, "engine");
        boolean enabled = getBoolean(data, "enabled", true);
        String author = getString(data, "author");

        Map<String, Object> detectionMap = (Map<String, Object>) data.get("detection");
        String detectionType = getString(detectionMap, "type");
        String detectionPattern = getString(detectionMap, "pattern");
        Detection detection = new Detection(detectionType, detectionPattern);

        Map<String, Object> fixMap = (Map<String, Object>) data.get("fix");
        String fixDescription = fixMap != null ? getString(fixMap, "description") : null;
        String fixExample = fixMap != null ? getString(fixMap, "example") : null;
        Fix fix = fixMap != null ? new Fix(fixDescription, fixExample) : null;

        List<Map<String, Object>> fpScenarios = (List<Map<String, Object>>) data.get("false_positive_scenarios");
        List<FalsePositiveScenario> falsePositives = new ArrayList<>();
        if (fpScenarios != null) {
            for (Map<String, Object> fps : fpScenarios) {
                falsePositives.add(new FalsePositiveScenario(
                    getString(fps, "framework"),
                    getString(fps, "reason")
                ));
            }
        }

        return new Rule(id, name, severity, cwe, languages, engine, detection, fix, falsePositives, author, enabled);
    }

    private static String getString(Map<String, Object> map, String key) {
        if (map == null) {
            return null;
        }
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    @SuppressWarnings("unchecked")
    private static List<String> getStringList(Map<String, Object> map, String key) {
        if (map == null) {
            return List.of();
        }
        Object value = map.get(key);
        if (value instanceof List) {
            return ((List<Object>) value).stream()
                .map(Object::toString)
                .toList();
        }
        return List.of();
    }

    private static boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        if (map == null) {
            return defaultValue;
        }
        Object value = map.get(key);
        if (value instanceof Boolean b) {
            return b;
        }
        return defaultValue;
    }
}
