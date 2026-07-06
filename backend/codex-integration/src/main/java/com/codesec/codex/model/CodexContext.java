package com.codesec.codex.model;

public class CodexContext {
    private String model;
    private int timeoutSeconds;

    public CodexContext() {}

    public CodexContext(String model, int timeoutSeconds) {
        this.model = model;
        this.timeoutSeconds = timeoutSeconds;
    }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
}
