package com.codesec.codex.model;

import java.util.List;

public class LogicVulnResult {
    private String vulnType;
    private List<String> evidenceChain;
    private String exploitCondition;
    private String riskLevel;
    private String recommendedFix;
    private String codeSnippet;
    private int lineStart;
    private int lineEnd;

    public LogicVulnResult() {}

    public LogicVulnResult(String vulnType, List<String> evidenceChain,
                           String exploitCondition, String riskLevel, String recommendedFix) {
        this.vulnType = vulnType;
        this.evidenceChain = evidenceChain;
        this.exploitCondition = exploitCondition;
        this.riskLevel = riskLevel;
        this.recommendedFix = recommendedFix;
    }

    public String getVulnType() { return vulnType; }
    public void setVulnType(String vulnType) { this.vulnType = vulnType; }
    public List<String> getEvidenceChain() { return evidenceChain; }
    public void setEvidenceChain(List<String> evidenceChain) { this.evidenceChain = evidenceChain; }
    public String getExploitCondition() { return exploitCondition; }
    public void setExploitCondition(String exploitCondition) { this.exploitCondition = exploitCondition; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getRecommendedFix() { return recommendedFix; }
    public void setRecommendedFix(String recommendedFix) { this.recommendedFix = recommendedFix; }
    public String getCodeSnippet() { return codeSnippet; }
    public void setCodeSnippet(String codeSnippet) { this.codeSnippet = codeSnippet; }
    public int getLineStart() { return lineStart; }
    public void setLineStart(int lineStart) { this.lineStart = lineStart; }
    public int getLineEnd() { return lineEnd; }
    public void setLineEnd(int lineEnd) { this.lineEnd = lineEnd; }
}
