package com.codesec.codex.prompt;

import com.codesec.codex.model.CodexCapability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class PromptLoader {
    private static final Logger log = LoggerFactory.getLogger(PromptLoader.class);

    private static final String PROMPT_DIR = "prompts/";

    public PromptTemplate load(String capabilityName) {
        String path = PROMPT_DIR + capabilityName + ".yaml";
        return loadFromPath(path);
    }

    public PromptTemplate loadFromPath(String classpath) {
        Yaml yaml = new Yaml();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(classpath)) {
            if (is == null) {
                throw new IllegalArgumentException("Prompt file not found: " + classpath);
            }
            Map<String, Object> raw = yaml.load(is);
            return parse(raw);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load prompt: " + classpath, e);
        }
    }

    @SuppressWarnings("unchecked")
    private PromptTemplate parse(Map<String, Object> raw) {
        PromptTemplate template = new PromptTemplate();
        template.setCapability(CodexCapability.valueOf(
            ((String) raw.get("capability")).toUpperCase()));
        Object version = raw.get("version");
        template.setVersion(version != null ? version.toString() : "1.0");
        template.setModel((String) raw.get("model"));
        template.setSystemPrompt((String) raw.get("system_prompt"));
        template.setUserPromptTemplate((String) raw.get("user_prompt_template"));
        template.setTimeoutSeconds(asInt(raw.get("timeout_seconds"), 30));
        template.setMaxRetries(asInt(raw.get("max_retries"), 2));
        return template;
    }

    private int asInt(Object val, int defaultVal) {
        if (val instanceof Integer) return (Integer) val;
        if (val instanceof String) return Integer.parseInt((String) val);
        return defaultVal;
    }

    public List<String> listAvailable() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:" + PROMPT_DIR + "*.yaml");
            return Arrays.stream(resources)
                .map(Resource::getFilename)
                .filter(f -> f != null && f.endsWith(".yaml"))
                .map(f -> f.replace(".yaml", ""))
                .toList();
        } catch (Exception e) {
            log.warn("Failed to list prompts", e);
            return List.of();
        }
    }
}
