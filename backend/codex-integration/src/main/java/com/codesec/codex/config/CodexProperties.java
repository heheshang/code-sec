package com.codesec.codex.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "codex")
public class CodexProperties {
   private boolean enabled = true;
   private boolean benchmarksEnabled = false;
   private int maxConcurrency = 20;
   private int perRequestTimeoutSeconds = 30;
   private ApiModelConfig codeModel = new ApiModelConfig();
   private ApiModelConfig llmModel = new ApiModelConfig();
   private CapabilitiesConfig capabilities = new CapabilitiesConfig();

   public boolean isEnabled() { return enabled; }
   public void setEnabled(boolean enabled) { this.enabled = enabled; }
   public boolean isBenchmarksEnabled() { return benchmarksEnabled; }
   public void setBenchmarksEnabled(boolean benchmarksEnabled) { this.benchmarksEnabled = benchmarksEnabled; }
   public int getMaxConcurrency() { return maxConcurrency; }
    public void setMaxConcurrency(int maxConcurrency) { this.maxConcurrency = maxConcurrency; }
    public int getPerRequestTimeoutSeconds() { return perRequestTimeoutSeconds; }
    public void setPerRequestTimeoutSeconds(int perRequestTimeoutSeconds) { this.perRequestTimeoutSeconds = perRequestTimeoutSeconds; }
    public ApiModelConfig getCodeModel() { return codeModel; }
    public void setCodeModel(ApiModelConfig codeModel) { this.codeModel = codeModel; }
    public ApiModelConfig getLlmModel() { return llmModel; }
    public void setLlmModel(ApiModelConfig llmModel) { this.llmModel = llmModel; }
    public CapabilitiesConfig getCapabilities() { return capabilities; }
    public void setCapabilities(CapabilitiesConfig capabilities) { this.capabilities = capabilities; }

    public static class ApiModelConfig {
        private String model = "qwen3-coder-plus";
        private int maxTokens = 4096;
        private double temperature = 0.1;
        private int timeoutSeconds = 120;

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
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
