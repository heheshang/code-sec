package com.codesec.engine.judge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads protection rules from YAML files on the classpath.
 * Consolidates duplicate rule-loading logic from
 * {@link ExploitabilityJudger} and {@link FrameworkProtectionDetector}.
 */
public final class ProtectionRuleLoader {

    private static final Logger log = LoggerFactory.getLogger(ProtectionRuleLoader.class);

    static final String[] RULE_FILES = {
        "rules/protection/spring-security.yml",
        "rules/protection/mybatis.yml",
        "rules/protection/hibernate.yml",
        "rules/protection/esapi.yml"
    };

    private ProtectionRuleLoader() {
        /* utility class */
    }

    /**
     * Loads all default protection rules from the classpath YAML files.
     *
     * @return unmodifiable list of default protection rules
     * @throws IllegalStateException if a rule file is missing or unparseable
     */
    public static List<ProtectionRule> loadDefaultRules() {
        List<ProtectionRule> allRules = new ArrayList<>();

        for (String file : RULE_FILES) {
            InputStream is = ProtectionRuleLoader.class.getClassLoader().getResourceAsStream(file);
            if (is == null) {
                throw new IllegalStateException(
                    "Protection rule file not found on classpath: " + file);
            }
            try (is) {
                Yaml yaml = new Yaml();
                Object loaded = yaml.load(is);

                if (!(loaded instanceof List<?> rawList)) {
                    throw new IllegalStateException(
                        "Expected YAML sequence in " + file + ", got " + loaded.getClass().getSimpleName());
                }

                for (Object item : rawList) {
                    if (!(item instanceof Map<?, ?> rawMap)) {
                        throw new IllegalStateException(
                            "Expected YAML mapping in " + file + ", got " + item.getClass().getSimpleName());
                    }
                    @SuppressWarnings("unchecked")
                    Map<String, Object> raw = (Map<String, Object>) rawMap;
                    allRules.add(parseRule(raw, file));
                }
            } catch (IOException e) {
                throw new IllegalStateException(
                    "Failed to read protection rule file: " + file, e);
            }
        }

        log.info("Loaded {} protection rules from {} YAML files", allRules.size(), RULE_FILES.length);
        return List.copyOf(allRules);
    }

    /**
     * Parses a raw YAML map into a {@link ProtectionRule}.
     *
     * @param raw  the deserialized YAML mapping
     * @param file the source file name (for error messages)
     * @return a parsed ProtectionRule
     * @throws IllegalArgumentException if required fields are missing or have invalid values
     */
    @SuppressWarnings("unchecked")
    public static ProtectionRule parseRule(Map<String, ?> raw, String file) {
        String name = (String) raw.get("name");
        String typeStr = (String) raw.get("type");
        String reason = (String) raw.get("reason");
        Object matchRaw = raw.get("match");

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Rule in " + file + " missing required field: name");
        }
        if (typeStr == null || typeStr.isBlank()) {
            throw new IllegalArgumentException("Rule '" + name + "' in " + file + " missing required field: type");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Rule '" + name + "' in " + file + " missing required field: reason");
        }
        if (!(matchRaw instanceof Map<?, ?> matchMap)) {
            throw new IllegalArgumentException(
                "Rule '" + name + "' in " + file + " missing required field: match");
        }

        ProtectionRule.Type type = switch (typeStr) {
            case "annotation" -> ProtectionRule.Type.ANNOTATION;
            case "method-call" -> ProtectionRule.Type.METHOD_CALL;
            case "class-annotation" -> ProtectionRule.Type.CLASS_ANNOTATION;
            default -> throw new IllegalArgumentException(
                "Rule '" + name + "' in " + file + " has unknown type: " + typeStr);
        };

        Map<String, Object> match = (Map<String, Object>) matchMap;
        String annotation = (String) match.get("annotation");
        String scope = (String) match.get("scope");
        String methodName = (String) match.get("methodName");
        String className = (String) match.get("className");

        ProtectionRule.MatchSpec matchSpec = new ProtectionRule.MatchSpec(
            annotation, scope, methodName, className);

        return new ProtectionRule(name, type, matchSpec, reason);
    }
}
