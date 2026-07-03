package com.codesec.codex.model;

public class CodexHealth {
    private boolean ok;
    private String modelType;
    private String modelVersion;
    private long latencyMs;

    public CodexHealth() {}

    public CodexHealth(boolean ok, String modelType, String modelVersion, long latencyMs) {
        this.ok = ok;
        this.modelType = modelType;
        this.modelVersion = modelVersion;
        this.latencyMs = latencyMs;
    }

    public boolean isOk() { return ok; }
    public void setOk(boolean ok) { this.ok = ok; }
    public String getModelType() { return modelType; }
    public void setModelType(String modelType) { this.modelType = modelType; }
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    public long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }
}
