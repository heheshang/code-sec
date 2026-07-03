package com.codesec.codex.benchmark;

public class FalsePositiveSample {
    private String id;
    private String ruleId;
    private String title;
    private String severity;
    private String language;
    private String code;
    private String callChain;
    private String dataSource;
    private boolean reachable;
    private String frameworkProtection;
    private String expectedVerdict;
    private double confidence;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getCallChain() { return callChain; }
    public void setCallChain(String callChain) { this.callChain = callChain; }
    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }
    public boolean isReachable() { return reachable; }
    public void setReachable(boolean reachable) { this.reachable = reachable; }
    public String getFrameworkProtection() { return frameworkProtection; }
    public void setFrameworkProtection(String frameworkProtection) { this.frameworkProtection = frameworkProtection; }
    public String getExpectedVerdict() { return expectedVerdict; }
    public void setExpectedVerdict(String expectedVerdict) { this.expectedVerdict = expectedVerdict; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public boolean isTruePositive() { return "true_positive".equals(expectedVerdict); }
}
