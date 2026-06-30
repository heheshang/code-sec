package com.codesec.search.dto;

import java.util.List;

/**
 * Highlight fragment DTO — conveys which field was matched and the highlighted text snippets.
 */
public class HighlightFragment {

    private String field;
    private List<String> fragments;

    public HighlightFragment() {}

    public HighlightFragment(String field, List<String> fragments) {
        this.field = field;
        this.fragments = fragments;
    }

    public String getField() { return field; }
    public void setField(String field) { this.field = field; }

    public List<String> getFragments() { return fragments; }
    public void setFragments(List<String> fragments) { this.fragments = fragments; }
}
