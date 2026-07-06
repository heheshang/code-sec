package com.codesec.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class ScanCreateRequest {
    @NotNull private Long repoId;
    private String mode = "full";
    private String branch = "main";
    private String commitSha;
    private List<String> engines = List.of("self_sast");
}
