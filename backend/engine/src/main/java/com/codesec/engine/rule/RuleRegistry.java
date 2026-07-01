package com.codesec.engine.rule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RuleRegistry {
    private static final Logger log = LoggerFactory.getLogger(RuleRegistry.class);
    private final Map<String, Rule> rulesById = new LinkedHashMap<>();
    private final List<Rule> allRules = new ArrayList<>();

    public void loadFromDirectory(Path ruleDir) throws IOException {
        List<Rule> loaded = RuleLoader.loadFromDirectory(ruleDir);
        addAll(loaded);
    }

    public void loadFromClasspath(String classpathPrefix) throws IOException {
        addAll(RuleLoader.loadFromClasspathDirectory(classpathPrefix));
    }

    /**
     * Load rules from one or more classpath language directories (e.g. "rules/java", "rules/go").
     */
    public void loadFromClasspath(String... classpathPrefixes) throws IOException {
        List<Rule> loaded = RuleLoader.loadFromClasspathDirectories(classpathPrefixes);
        addAll(loaded);
    }

    private void addAll(List<Rule> loaded) {
        for (Rule rule : loaded) {
            rulesById.put(rule.id(), rule);
            allRules.add(rule);
        }
    }

    public void addRule(Rule rule) {
        rulesById.put(rule.id(), rule);
        allRules.add(rule);
    }

    public Optional<Rule> findById(String ruleId) {
        return Optional.ofNullable(rulesById.get(ruleId));
    }

    public List<Rule> findAll() {
        return List.copyOf(allRules);
    }

    public List<Rule> findByLanguage(String language) {
        return allRules.stream()
            .filter(Rule::isEnabled)
            .filter(rule -> rule.supportsLanguage(language))
            .toList();
    }

    public List<Rule> findEnabled() {
        return allRules.stream()
            .filter(Rule::isEnabled)
            .toList();
    }

    public int size() {
        return rulesById.size();
    }

    public void clear() {
        rulesById.clear();
        allRules.clear();
    }
}
