package com.codesec.search.repository;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * ES file_snippet index document — v1 alpha: 4 fields only.
 * file_path / project_id / language / indexed_at.
 * Content field is NOT indexed in v1 (Sprint 3).
 */
public class SnippetDocument {

    @JsonProperty("file_path")
    private String filePath;

    @JsonProperty("project_id")
    private String projectId;

    @JsonProperty("language")
    private String language;

    @JsonProperty("indexed_at")
    private String indexedAt;

    public SnippetDocument() {}

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getIndexedAt() { return indexedAt; }
    public void setIndexedAt(String indexedAt) { this.indexedAt = indexedAt; }

    public static SnippetDocumentBuilder builder() {
        return new SnippetDocumentBuilder();
    }

    public static class SnippetDocumentBuilder {
        private final SnippetDocument doc = new SnippetDocument();
        public SnippetDocumentBuilder filePath(String v) { doc.setFilePath(v); return this; }
        public SnippetDocumentBuilder projectId(String v) { doc.setProjectId(v); return this; }
        public SnippetDocumentBuilder language(String v) { doc.setLanguage(v); return this; }
        public SnippetDocumentBuilder indexedAt(String v) { doc.setIndexedAt(v); return this; }
        public SnippetDocument build() { return doc; }
    }
}
