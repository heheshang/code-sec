package com.codesec.domain.dto;

import java.util.List;

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
