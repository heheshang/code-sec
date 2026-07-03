package com.codesec.codex.client;

import com.codesec.codex.model.ClientType;
import com.codesec.codex.model.CodexContext;
import com.codesec.codex.model.CodexHealth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

public class CodexApiClient implements CodexClient {
    private static final Logger log = LoggerFactory.getLogger(CodexApiClient.class);

    private final RestTemplate restTemplate;
    private final RateLimiter rateLimiter;
    private final CircuitBreaker circuitBreaker;
    private final RetryHandler retryHandler;
    private final String modelLabel;

    public CodexApiClient(RestTemplate restTemplate, RateLimiter rateLimiter,
                          CircuitBreaker circuitBreaker, RetryHandler retryHandler,
                          String modelLabel) {
        this.restTemplate = restTemplate;
        this.rateLimiter = rateLimiter;
        this.circuitBreaker = circuitBreaker;
        this.retryHandler = retryHandler;
        this.modelLabel = modelLabel;
    }

    @Override
    public String execute(CodexContext context, String systemPrompt, String userPrompt) {
        if (!circuitBreaker.isAvailable()) {
            throw new IllegalStateException("Circuit breaker is OPEN for " + modelLabel);
        }
        if (!rateLimiter.tryAcquire()) {
            throw new IllegalStateException("Rate limit exceeded for " + modelLabel);
        }

        Map<String, Object> requestBody = Map.of(
            "model", context.getModel(),
            "messages", new Object[] {
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
            },
            "max_tokens", 4096,
            "temperature", 0.1
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(context.getApiKey());

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        RetryHandler.RetryResult<String> result = retryHandler.execute(() -> {
            try {
                ResponseEntity<Map> response = restTemplate.postForEntity(
                    context.getEndpoint(), request, Map.class);
                Map body = response.getBody();
                if (body == null || !body.containsKey("choices")) {
                    throw new RuntimeException("unexpected API response: " + body);
                }
                Map choice = (Map) ((java.util.List) body.get("choices")).get(0);
                Map message = (Map) choice.get("message");
                String content = (String) message.get("content");
                if (content == null) {
                    throw new RuntimeException("empty response content");
                }
                circuitBreaker.onSuccess();
                return content;
            } catch (Exception e) {
                circuitBreaker.onFailure();
                throw e;
            }
        });

        if (!result.isSuccess()) {
            throw new RuntimeException("execute failed after " + result.getAttempts()
                + " attempts: " + result.getException().getMessage(), result.getException());
        }

        return result.getResult();
    }

    @Override
    public boolean isAvailable() {
        return circuitBreaker.isAvailable();
    }

    @Override
    public CodexHealth health() {
        long start = System.currentTimeMillis();
        boolean ok = isAvailable();
        long latency = System.currentTimeMillis() - start;
        return new CodexHealth(ok, modelLabel, "unknown", latency);
    }

    @Override
    public ClientType type() {
        return ClientType.API;
    }
}
