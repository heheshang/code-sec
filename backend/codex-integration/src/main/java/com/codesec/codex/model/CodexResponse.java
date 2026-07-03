package com.codesec.codex.model;

import java.time.Duration;

public class CodexResponse<T> {
    private boolean success;
    private T data;
    private String errorMessage;
    private Duration duration;
    private String modelVersion;
    private FallbackLevel fallbackLevel;

    public CodexResponse() {}

    public static <T> CodexResponse<T> success(T data, Duration duration, String modelVersion) {
        CodexResponse<T> resp = new CodexResponse<>();
        resp.success = true;
        resp.data = data;
        resp.duration = duration;
        resp.modelVersion = modelVersion;
        resp.fallbackLevel = FallbackLevel.NONE;
        return resp;
    }

    public static <T> CodexResponse<T> failure(String errorMessage, FallbackLevel fallbackLevel) {
        CodexResponse<T> resp = new CodexResponse<>();
        resp.success = false;
        resp.errorMessage = errorMessage;
        resp.fallbackLevel = fallbackLevel;
        return resp;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Duration getDuration() { return duration; }
    public void setDuration(Duration duration) { this.duration = duration; }
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    public FallbackLevel getFallbackLevel() { return fallbackLevel; }
    public void setFallbackLevel(FallbackLevel fallbackLevel) { this.fallbackLevel = fallbackLevel; }
}
