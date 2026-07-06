package com.codesec.codex.client;

import com.codesec.codex.config.CodexProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CodexClientFactory {

    @Bean
    @ConditionalOnProperty(name = "codex.enabled", havingValue = "true", matchIfMissing = true)
    public CodexClient codeModelClient(CodexProperties props) {
        CodexProperties.ApiModelConfig cfg = props.getCodeModel();
        return new CodexCliClient("code-model", null, cfg.getMaxTokens(), cfg.getTemperature());
    }

    @Bean
    @ConditionalOnProperty(name = "codex.enabled", havingValue = "true", matchIfMissing = true)
    public CodexClient llmModelClient(CodexProperties props) {
        CodexProperties.ApiModelConfig cfg = props.getLlmModel();
        return new CodexCliClient("llm-model", null, cfg.getMaxTokens(), cfg.getTemperature());
    }
}
