package com.codesec.common.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class TestConnectionResponse {
    private boolean ok;
    private String error;
    private List<String> branches;
}
