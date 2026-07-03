package com.codesec.codex.model;

import java.util.List;

public class PatchResult {
    private String patchCode;
    private String language;
    private String compilationStatus;
    private List<String> errors;
    private String filePath;
    private int lineStart;
    private int lineEnd;

    public PatchResult() {}

    public PatchResult(String patchCode, String language) {
        this.patchCode = patchCode;
        this.language = language;
        this.compilationStatus = "PENDING";
    }

    public String getPatchCode() { return patchCode; }
    public void setPatchCode(String patchCode) { this.patchCode = patchCode; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getCompilationStatus() { return compilationStatus; }
    public void setCompilationStatus(String compilationStatus) { this.compilationStatus = compilationStatus; }
    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public int getLineStart() { return lineStart; }
    public void setLineStart(int lineStart) { this.lineStart = lineStart; }
    public int getLineEnd() { return lineEnd; }
    public void setLineEnd(int lineEnd) { this.lineEnd = lineEnd; }
}
