package com.codesec.codex.prompt;

import com.codesec.codex.model.CodexCapability;

public class PromptTemplate {
    private CodexCapability capability;
    private String version;
    private String model;
    private String systemPrompt;
    private String userPromptTemplate;
    private int timeoutSeconds;
    private int maxRetries;

    public PromptTemplate() {}

    public PromptTemplate(CodexCapability capability, String version, String model,
                          String systemPrompt, String userPromptTemplate,
                          int timeoutSeconds, int maxRetries) {
        this.capability = capability;
        this.version = version;
        this.model = model;
        this.systemPrompt = systemPrompt;
        this.userPromptTemplate = userPromptTemplate;
        this.timeoutSeconds = timeoutSeconds;
        this.maxRetries = maxRetries;
    }

    public CodexCapability getCapability() { return capability; }
    public void setCapability(CodexCapability capability) { this.capability = capability; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
    public String getUserPromptTemplate() { return userPromptTemplate; }
    public void setUserPromptTemplate(String userPromptTemplate) { this.userPromptTemplate = userPromptTemplate; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
}
