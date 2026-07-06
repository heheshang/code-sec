package com.codesec.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VulnDocument {

    @JsonProperty("id")
    private String id;

    @JsonProperty("project_id")
    private String projectId;

    @JsonProperty("rule_id")
    private String ruleId;

    @JsonProperty("severity")
    private String severity;

    @JsonProperty("exploitability")
    private String exploitability;

    @JsonProperty("title")
    private String title;

    @JsonProperty("description")
    private String description;

    @JsonProperty("code_snippet")
    private String codeSnippet;

    @JsonProperty("file_path")
    private String filePath;

    @JsonProperty("cwe")
    private String cwe;

    @JsonProperty("engine")
    private String engine;

    @JsonProperty("discovered_at")
    private String discoveredAt;

    @JsonProperty("discovered_by")
    private String discoveredBy;

    public VulnDocument() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getExploitability() { return exploitability; }
    public void setExploitability(String exploitability) { this.exploitability = exploitability; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCodeSnippet() { return codeSnippet; }
    public void setCodeSnippet(String codeSnippet) { this.codeSnippet = codeSnippet; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getCwe() { return cwe; }
    public void setCwe(String cwe) { this.cwe = cwe; }

    public String getEngine() { return engine; }
    public void setEngine(String engine) { this.engine = engine; }

    public String getDiscoveredAt() { return discoveredAt; }
    public void setDiscoveredAt(String discoveredAt) { this.discoveredAt = discoveredAt; }

    public String getDiscoveredBy() { return discoveredBy; }
    public void setDiscoveredBy(String discoveredBy) { this.discoveredBy = discoveredBy; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final VulnDocument doc = new VulnDocument();
        public Builder id(String v) { doc.setId(v); return this; }
        public Builder projectId(String v) { doc.setProjectId(v); return this; }
        public Builder ruleId(String v) { doc.setRuleId(v); return this; }
        public Builder severity(String v) { doc.setSeverity(v); return this; }
        public Builder exploitability(String v) { doc.setExploitability(v); return this; }
        public Builder title(String v) { doc.setTitle(v); return this; }
        public Builder description(String v) { doc.setDescription(v); return this; }
        public Builder codeSnippet(String v) { doc.setCodeSnippet(v); return this; }
        public Builder filePath(String v) { doc.setFilePath(v); return this; }
        public Builder cwe(String v) { doc.setCwe(v); return this; }
        public Builder engine(String v) { doc.setEngine(v); return this; }
        public Builder discoveredAt(String v) { doc.setDiscoveredAt(v); return this; }
        public Builder discoveredBy(String v) { doc.setDiscoveredBy(v); return this; }
        public VulnDocument build() { return doc; }
    }
}
