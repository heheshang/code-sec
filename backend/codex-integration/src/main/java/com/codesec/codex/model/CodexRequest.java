package com.codesec.codex.model;

import java.time.Duration;
import java.util.Map;

public class CodexRequest {
    private String scanId;
    private String vulnId;
    private String language;
    private String codeSnippet;
    private String filePath;
    private int lineStart;
    private int lineEnd;
    private String callChain;
    private String dataSource;
    private boolean reachable;
    private Duration timeout;
    private Map<String, String> extra;

    public CodexRequest() {}

    public CodexRequest(String scanId, String vulnId, String language, String codeSnippet,
                        String filePath, int lineStart, int lineEnd) {
        this.scanId = scanId;
        this.vulnId = vulnId;
        this.language = language;
        this.codeSnippet = codeSnippet;
        this.filePath = filePath;
        this.lineStart = lineStart;
        this.lineEnd = lineEnd;
    }

    public String getScanId() { return scanId; }
    public void setScanId(String scanId) { this.scanId = scanId; }
    public String getVulnId() { return vulnId; }
    public void setVulnId(String vulnId) { this.vulnId = vulnId; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getCodeSnippet() { return codeSnippet; }
    public void setCodeSnippet(String codeSnippet) { this.codeSnippet = codeSnippet; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public int getLineStart() { return lineStart; }
    public void setLineStart(int lineStart) { this.lineStart = lineStart; }
    public int getLineEnd() { return lineEnd; }
    public void setLineEnd(int lineEnd) { this.lineEnd = lineEnd; }
    public String getCallChain() { return callChain; }
    public void setCallChain(String callChain) { this.callChain = callChain; }
    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }
    public boolean isReachable() { return reachable; }
    public void setReachable(boolean reachable) { this.reachable = reachable; }
    public Duration getTimeout() { return timeout; }
    public void setTimeout(Duration timeout) { this.timeout = timeout; }
    public Map<String, String> getExtra() { return extra; }
    public void setExtra(Map<String, String> extra) { this.extra = extra; }
}
