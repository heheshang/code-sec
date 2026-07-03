package com.codesec.codex.benchmark;

import java.util.List;
import java.util.Map;

public class VulnAnalysisSample {
    private String id;
    private String language;
    private String code;
    private String expectedVuln;
    private String cwe;
    private String difficulty;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getExpectedVuln() { return expectedVuln; }
    public void setExpectedVuln(String expectedVuln) { this.expectedVuln = expectedVuln; }
    public String getCwe() { return cwe; }
    public void setCwe(String cwe) { this.cwe = cwe; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public boolean isSafe() { return "null".equals(expectedVuln) || expectedVuln == null; }
}
