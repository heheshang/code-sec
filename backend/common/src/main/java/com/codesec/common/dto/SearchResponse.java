package com.codesec.common.dto;

import java.util.List;
import java.util.Map;

public class SearchResponse<T> {

    private long total;
    private int page;
    private int pageSize;
    private long tookMs;
    private List<T> items;
    private Map<String, List<String>> highlights;
    private List<String> warnings;

    public SearchResponse() {}

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }

    public long getTookMs() { return tookMs; }
    public void setTookMs(long tookMs) { this.tookMs = tookMs; }

    public List<T> getItems() { return items; }
    public void setItems(List<T> items) { this.items = items; }

    public Map<String, List<String>> getHighlights() { return highlights; }
    public void setHighlights(Map<String, List<String>> highlights) { this.highlights = highlights; }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }

    public static <T> Builder<T> builder() { return new Builder<>(); }

    public static class Builder<T> {
        private final SearchResponse<T> resp = new SearchResponse<>();
        public Builder<T> total(long v) { resp.setTotal(v); return this; }
        public Builder<T> page(int v) { resp.setPage(v); return this; }
        public Builder<T> pageSize(int v) { resp.setPageSize(v); return this; }
        public Builder<T> tookMs(long v) { resp.setTookMs(v); return this; }
        public Builder<T> items(List<T> v) { resp.setItems(v); return this; }
        public Builder<T> highlights(Map<String, List<String>> v) { resp.setHighlights(v); return this; }
        public Builder<T> warnings(List<String> v) { resp.setWarnings(v); return this; }
        public SearchResponse<T> build() { return resp; }
    }
}
