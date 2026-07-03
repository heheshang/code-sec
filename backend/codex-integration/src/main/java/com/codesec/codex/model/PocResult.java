package com.codesec.codex.model;

import java.util.List;

public class PocResult {
    private String pocType;
    private String httpMethod;
    private String endpoint;
    private String payload;
    private String expectedResult;
    private String codeSnippet;
    private String sandboxStatus;

    public PocResult() {}

    public PocResult(String pocType, String httpMethod, String endpoint,
                     String payload, String expectedResult, String codeSnippet) {
        this.pocType = pocType;
        this.httpMethod = httpMethod;
        this.endpoint = endpoint;
        this.payload = payload;
        this.expectedResult = expectedResult;
        this.codeSnippet = codeSnippet;
        this.sandboxStatus = "PENDING";
    }

    public String getPocType() { return pocType; }
    public void setPocType(String pocType) { this.pocType = pocType; }
    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getExpectedResult() { return expectedResult; }
    public void setExpectedResult(String expectedResult) { this.expectedResult = expectedResult; }
    public String getCodeSnippet() { return codeSnippet; }
    public void setCodeSnippet(String codeSnippet) { this.codeSnippet = codeSnippet; }
    public String getSandboxStatus() { return sandboxStatus; }
    public void setSandboxStatus(String sandboxStatus) { this.sandboxStatus = sandboxStatus; }
}
