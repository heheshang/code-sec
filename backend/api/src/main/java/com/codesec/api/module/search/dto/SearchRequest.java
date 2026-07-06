package com.codesec.api.module.search.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;

public class SearchRequest {

    @Size(max = 200, message = "SEARCH_QUERY_TOO_LONG")
    private String q;

    private List<String> severity;

    private List<String> exploitability;

    private List<String> projectId;

    private List<String> engine;

    private String discoveredAtFrom;

    private String discoveredAtTo;

    @Min(1)
    private int page = 1;

    @Min(1)
    @Max(100)
    private int pageSize = 20;

    private String sortBy = "_score";

    private String sortOrder = "desc";

    public SearchRequest() {}

    public String getQ() { return q; }
    public void setQ(String q) { this.q = q; }

    public List<String> getSeverity() { return severity; }
    public void setSeverity(List<String> severity) { this.severity = severity; }

    public List<String> getExploitability() { return exploitability; }
    public void setExploitability(List<String> exploitability) { this.exploitability = exploitability; }

    public List<String> getProjectId() { return projectId; }
    public void setProjectId(List<String> projectId) { this.projectId = projectId; }

    public List<String> getEngine() { return engine; }
    public void setEngine(List<String> engine) { this.engine = engine; }

    public String getDiscoveredAtFrom() { return discoveredAtFrom; }
    public void setDiscoveredAtFrom(String discoveredAtFrom) { this.discoveredAtFrom = discoveredAtFrom; }

    public String getDiscoveredAtTo() { return discoveredAtTo; }
    public void setDiscoveredAtTo(String discoveredAtTo) { this.discoveredAtTo = discoveredAtTo; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }

    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }

    public String getSortOrder() { return sortOrder; }
    public void setSortOrder(String sortOrder) { this.sortOrder = sortOrder; }
}
