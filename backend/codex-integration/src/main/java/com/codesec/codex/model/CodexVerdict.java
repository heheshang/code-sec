package com.codesec.codex.model;

public class CodexVerdict {
    private String vulnId;
    private String verdict;
    private double confidence;
    private String reason;

    public CodexVerdict() {}

    public CodexVerdict(String vulnId, String verdict, double confidence, String reason) {
        this.vulnId = vulnId;
        this.verdict = verdict;
        this.confidence = confidence;
        this.reason = reason;
    }

    public String getVulnId() { return vulnId; }
    public void setVulnId(String vulnId) { this.vulnId = vulnId; }
    public String getVerdict() { return verdict; }
    public void setVerdict(String verdict) { this.verdict = verdict; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
