package com.codesec.engine.rule;

import java.util.List;

public record Rule(
    String id,
    String name,
    String severity,
    String cwe,
    List<String> languages,
    String engine,
    Detection detection,
    Fix fix,
    List<FalsePositiveScenario> falsePositiveScenarios,
    String author,
    boolean enabled
) {
    public boolean isEnabled() {
        return enabled;
    }

    public boolean supportsLanguage(String language) {
        return languages.contains(language);
    }

    public String detectionType() {
        return detection.type();
    }

    public String detectionPattern() {
        return detection.pattern();
    }

    public String fixDescription() {
        return fix != null ? fix.description() : null;
    }

    public String fixExample() {
        return fix != null ? fix.example() : null;
    }
}
