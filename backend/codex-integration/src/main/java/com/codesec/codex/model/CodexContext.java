package com.codesec.codex.model;

public class CodexContext {
    private String apiKey;
    private String endpoint;
    private String model;
    private int timeoutSeconds;

    public CodexContext() {}

    public CodexContext(String apiKey, String endpoint, String model, int timeoutSeconds) {
        this.apiKey = apiKey;
        this.endpoint = endpoint;
        this.model = model;
        this.timeoutSeconds = timeoutSeconds;
    }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
}
