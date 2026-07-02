package com.codesec.engineadapter;

import com.codesec.engine.rule.RuleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EngineAdapterConfig {
    private static final Logger log = LoggerFactory.getLogger(EngineAdapterConfig.class);

    @Bean
    public RuleRegistry ruleRegistry() {
        RuleRegistry registry = new RuleRegistry();
        try {
            registry.loadFromClasspath("rules/java", "rules/go", "rules/python");
            log.info("Loaded {} rules from classpath", registry.size());
        } catch (Exception e) {
            log.warn("Failed to load rules from classpath: {}", e.getMessage());
        }
        return registry;
    }
}
