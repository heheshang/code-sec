package com.codesec.codex.client;

import com.codesec.codex.config.CodexProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class CodexClientFactory {

    @Bean
    @ConditionalOnProperty(name = "codex.enabled", havingValue = "true", matchIfMissing = true)
    public CodexClient codeModelClient(CodexProperties props) {
        CodexProperties.ApiModelConfig cfg = props.getApi().getCodeModel();
        return new CodexApiClient(
            new RestTemplate(),
            new RateLimiter(props.getMaxConcurrency(), props.getMaxConcurrency()),
            new CircuitBreaker(
                props.getApiStrategy().getCircuitBreaker().getFailureThreshold(),
                props.getApiStrategy().getCircuitBreaker().getResetTimeoutMs()),
            new RetryHandler(
                props.getApiStrategy().getRetry().getMaxAttempts(),
                props.getApiStrategy().getRetry().getInitialDelayMs(),
                2.0),
            "code-model"
        );
    }

    @Bean
    @ConditionalOnProperty(name = "codex.enabled", havingValue = "true", matchIfMissing = true)
    public CodexClient llmModelClient(CodexProperties props) {
        CodexProperties.ApiModelConfig cfg = props.getApi().getLlmModel();
        return new CodexApiClient(
            new RestTemplate(),
            new RateLimiter(props.getMaxConcurrency(), props.getMaxConcurrency()),
            new CircuitBreaker(
                props.getApiStrategy().getCircuitBreaker().getFailureThreshold(),
                props.getApiStrategy().getCircuitBreaker().getResetTimeoutMs()),
            new RetryHandler(
                props.getApiStrategy().getRetry().getMaxAttempts(),
                props.getApiStrategy().getRetry().getInitialDelayMs(),
                2.0),
            "llm-model"
        );
    }
}
