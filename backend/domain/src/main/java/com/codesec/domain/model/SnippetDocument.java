package com.codesec.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SnippetDocument {

    @JsonProperty("file_path")
    private String filePath;

    @JsonProperty("project_id")
    private String projectId;

    @JsonProperty("language")
    private String language;

    @JsonProperty("code_snippet")
    private String codeSnippet;

    @JsonProperty("line_start")
    private Integer lineStart;

    @JsonProperty("indexed_at")
    private String indexedAt;

    public SnippetDocument() {}

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getCodeSnippet() { return codeSnippet; }
    public void setCodeSnippet(String codeSnippet) { this.codeSnippet = codeSnippet; }

    public Integer getLineStart() { return lineStart; }
    public void setLineStart(Integer lineStart) { this.lineStart = lineStart; }

    public String getIndexedAt() { return indexedAt; }
    public void setIndexedAt(String indexedAt) { this.indexedAt = indexedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final SnippetDocument doc = new SnippetDocument();
        public Builder filePath(String v) { doc.setFilePath(v); return this; }
        public Builder projectId(String v) { doc.setProjectId(v); return this; }
        public Builder language(String v) { doc.setLanguage(v); return this; }
        public Builder codeSnippet(String v) { doc.setCodeSnippet(v); return this; }
        public Builder lineStart(Integer v) { doc.setLineStart(v); return this; }
        public Builder indexedAt(String v) { doc.setIndexedAt(v); return this; }
        public SnippetDocument build() { return doc; }
    }
}
