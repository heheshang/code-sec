package com.codesec.domain.service.ai;

public class AiAnalysisResult {
    private String vulnId;
    private String aiVerdict;
    private double aiConfidence;
    private String aiExplanation;
    private String aiGeneratedPatch;
    private String analyzedAt;
    private String modelVersion;
    private long durationMs;
    private String fallbackLevel;

    public String getVulnId() { return vulnId; }
    public void setVulnId(String vulnId) { this.vulnId = vulnId; }
    public String getAiVerdict() { return aiVerdict; }
    public void setAiVerdict(String aiVerdict) { this.aiVerdict = aiVerdict; }
    public double getAiConfidence() { return aiConfidence; }
    public void setAiConfidence(double aiConfidence) { this.aiConfidence = aiConfidence; }
    public String getAiExplanation() { return aiExplanation; }
    public void setAiExplanation(String aiExplanation) { this.aiExplanation = aiExplanation; }
    public String getAiGeneratedPatch() { return aiGeneratedPatch; }
    public void setAiGeneratedPatch(String aiGeneratedPatch) { this.aiGeneratedPatch = aiGeneratedPatch; }
    public String getAnalyzedAt() { return analyzedAt; }
    public void setAnalyzedAt(String analyzedAt) { this.analyzedAt = analyzedAt; }
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
    public String getFallbackLevel() { return fallbackLevel; }
    public void setFallbackLevel(String fallbackLevel) { this.fallbackLevel = fallbackLevel; }
}
