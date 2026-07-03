package com.codesec.codex.prompt;

import com.codesec.codex.model.CodexCapability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class PromptRepository {
    private static final Logger log = LoggerFactory.getLogger(PromptRepository.class);

    private final PromptLoader loader;
    private final Map<String, PromptTemplate> cache = new ConcurrentHashMap<>();

    public PromptRepository(PromptLoader loader) {
        this.loader = loader;
    }

    public PromptTemplate findByCapability(CodexCapability capability) {
        return cache.computeIfAbsent(capability.name().toLowerCase().replace('_', '-'), name -> {
            log.info("Loading prompt template for capability: {}", name);
            return loader.load(name);
        });
    }

    public PromptTemplate findByCapability(CodexCapability capability, String language) {
        String key = capability.name().toLowerCase().replace('_', '-') + "_" + language;
        return cache.computeIfAbsent(key, name -> {
            log.info("Loading prompt template for capability: {} language: {}", capability, language);
            PromptTemplate base = findByCapability(capability);
            PromptTemplate lang = new PromptTemplate();
            lang.setCapability(base.getCapability());
            lang.setVersion(base.getVersion());
            lang.setModel(base.getModel());
            lang.setSystemPrompt(base.getSystemPrompt());
            String langSpecific = base.getUserPromptTemplate()
                .replace("{language}", language);
            lang.setUserPromptTemplate(langSpecific);
            lang.setTimeoutSeconds(base.getTimeoutSeconds());
            lang.setMaxRetries(base.getMaxRetries());
            return lang;
        });
    }

    public void refresh() {
        cache.clear();
        loader.listAvailable().forEach(name -> {
            cache.put(name, loader.load(name));
        });
    }
}
