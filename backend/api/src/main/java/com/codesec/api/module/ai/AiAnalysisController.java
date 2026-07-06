package com.codesec.api.module.ai;

import com.codesec.domain.service.ai.AiAnalysisResult;
import com.codesec.domain.service.ai.AiAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiAnalysisController {

    private final AiAnalysisService aiAnalysisService;

    @PostMapping("/analyze/{vulnId}")
    @PreAuthorize("@perm.check('vuln:read')")
    public ResponseEntity<AiAnalysisResult> analyze(@PathVariable Long vulnId) {
        AiAnalysisResult result = aiAnalysisService.analyze(vulnId);
        return ResponseEntity.ok(result);
    }
}
