package com.codesec.codex.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "codex")
public class CodexProperties {
    private boolean enabled = true;
    private int maxConcurrency = 20;
    private int perRequestTimeoutSeconds = 30;
    private ApiConfig api = new ApiConfig();
    private ApiStrategyConfig apiStrategy = new ApiStrategyConfig();
    private CapabilitiesConfig capabilities = new CapabilitiesConfig();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getMaxConcurrency() { return maxConcurrency; }
    public void setMaxConcurrency(int maxConcurrency) { this.maxConcurrency = maxConcurrency; }
    public int getPerRequestTimeoutSeconds() { return perRequestTimeoutSeconds; }
    public void setPerRequestTimeoutSeconds(int perRequestTimeoutSeconds) { this.perRequestTimeoutSeconds = perRequestTimeoutSeconds; }
    public ApiConfig getApi() { return api; }
    public void setApi(ApiConfig api) { this.api = api; }
    public ApiStrategyConfig getApiStrategy() { return apiStrategy; }
    public void setApiStrategy(ApiStrategyConfig apiStrategy) { this.apiStrategy = apiStrategy; }
    public CapabilitiesConfig getCapabilities() { return capabilities; }
    public void setCapabilities(CapabilitiesConfig capabilities) { this.capabilities = capabilities; }

    public static class ApiConfig {
        private ApiModelConfig codeModel = new ApiModelConfig();
        private ApiModelConfig llmModel = new ApiModelConfig();

        public ApiModelConfig getCodeModel() { return codeModel; }
        public void setCodeModel(ApiModelConfig codeModel) { this.codeModel = codeModel; }
        public ApiModelConfig getLlmModel() { return llmModel; }
        public void setLlmModel(ApiModelConfig llmModel) { this.llmModel = llmModel; }
    }

    public static class ApiModelConfig {
        private String endpoint = "https://api.openai.com/v1/chat/completions";
        private String apiKey;
        private String model = "gpt-4o";
        private int maxTokens = 4096;
        private double temperature = 0.1;
        private int timeoutSeconds = 60;

        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    }

    public static class ApiStrategyConfig {
        private RetryConfig retry = new RetryConfig();
        private CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig();

        public RetryConfig getRetry() { return retry; }
        public void setRetry(RetryConfig retry) { this.retry = retry; }
        public CircuitBreakerConfig getCircuitBreaker() { return circuitBreaker; }
        public void setCircuitBreaker(CircuitBreakerConfig circuitBreaker) { this.circuitBreaker = circuitBreaker; }
    }

    public static class RetryConfig {
        private int maxAttempts = 3;
        private String backoff = "exponential";
        private long initialDelayMs = 1000;

        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
        public String getBackoff() { return backoff; }
        public void setBackoff(String backoff) { this.backoff = backoff; }
        public long getInitialDelayMs() { return initialDelayMs; }
        public void setInitialDelayMs(long initialDelayMs) { this.initialDelayMs = initialDelayMs; }
    }

    public static class CircuitBreakerConfig {
        private int failureThreshold = 5;
        private long resetTimeoutMs = 60000;

        public int getFailureThreshold() { return failureThreshold; }
        public void setFailureThreshold(int failureThreshold) { this.failureThreshold = failureThreshold; }
        public long getResetTimeoutMs() { return resetTimeoutMs; }
        public void setResetTimeoutMs(long resetTimeoutMs) { this.resetTimeoutMs = resetTimeoutMs; }
    }

    public static class CapabilitiesConfig {
        private boolean vulnAnalysis = true;
        private boolean falsePositiveFilter = true;
        private boolean pocGeneration = false;
        private boolean patchGeneration = false;
        private boolean logicVulnMining = false;

        public boolean isVulnAnalysis() { return vulnAnalysis; }
        public void setVulnAnalysis(boolean vulnAnalysis) { this.vulnAnalysis = vulnAnalysis; }
        public boolean isFalsePositiveFilter() { return falsePositiveFilter; }
        public void setFalsePositiveFilter(boolean falsePositiveFilter) { this.falsePositiveFilter = falsePositiveFilter; }
        public boolean isPocGeneration() { return pocGeneration; }
        public void setPocGeneration(boolean pocGeneration) { this.pocGeneration = pocGeneration; }
        public boolean isPatchGeneration() { return patchGeneration; }
        public void setPatchGeneration(boolean patchGeneration) { this.patchGeneration = patchGeneration; }
        public boolean isLogicVulnMining() { return logicVulnMining; }
        public void setLogicVulnMining(boolean logicVulnMining) { this.logicVulnMining = logicVulnMining; }
    }
}
