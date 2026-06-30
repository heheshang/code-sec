package com.codesec.search.repository;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * ES vuln index document — 13 fields mapped from MySQL vuln_finding table.
 * All fields must align with vuln.json mapping and E-S2-CRITICAL spec § 3.2 注 2.1.
 */
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

    // --- Getters and setters ---

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

    // --- Builder pattern for test convenience ---

    public static VulnDocumentBuilder builder() {
        return new VulnDocumentBuilder();
    }

    public static class VulnDocumentBuilder {
        private final VulnDocument doc = new VulnDocument();
        public VulnDocumentBuilder id(String v) { doc.setId(v); return this; }
        public VulnDocumentBuilder projectId(String v) { doc.setProjectId(v); return this; }
        public VulnDocumentBuilder ruleId(String v) { doc.setRuleId(v); return this; }
        public VulnDocumentBuilder severity(String v) { doc.setSeverity(v); return this; }
        public VulnDocumentBuilder exploitability(String v) { doc.setExploitability(v); return this; }
        public VulnDocumentBuilder title(String v) { doc.setTitle(v); return this; }
        public VulnDocumentBuilder description(String v) { doc.setDescription(v); return this; }
        public VulnDocumentBuilder codeSnippet(String v) { doc.setCodeSnippet(v); return this; }
        public VulnDocumentBuilder filePath(String v) { doc.setFilePath(v); return this; }
        public VulnDocumentBuilder cwe(String v) { doc.setCwe(v); return this; }
        public VulnDocumentBuilder engine(String v) { doc.setEngine(v); return this; }
        public VulnDocumentBuilder discoveredAt(String v) { doc.setDiscoveredAt(v); return this; }
        public VulnDocumentBuilder discoveredBy(String v) { doc.setDiscoveredBy(v); return this; }
        public VulnDocument build() { return doc; }
    }
}
