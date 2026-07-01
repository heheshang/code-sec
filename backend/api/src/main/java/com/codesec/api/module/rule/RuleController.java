package com.codesec.api.module.rule;

import com.codesec.api.interfaces.dto.PaginatedResult;
import com.codesec.api.module.rule.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RuleController {
    private final RuleService ruleService;

    // ========== Rule Metadata ==========

    @GetMapping("/api/v1/rules")
    @PreAuthorize("@perm.check('rule:read')")
    public PaginatedResult<RuleResponse> listRules(
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String engine,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ruleService.listRules(severity, language, engine, page, size);
    }

    @GetMapping("/api/v1/rules/{id}")
    @PreAuthorize("@perm.check('rule:read')")
    public RuleResponse getRule(@PathVariable Long id) {
        return ruleService.getRule(id);
    }

    @PutMapping("/api/v1/rules/{id}")
    @PreAuthorize("@perm.check('rule:update')")
    public RuleResponse updateRule(@PathVariable Long id, @RequestBody RuleUpdateRequest req) {
        return ruleService.updateRule(id, req);
    }

    @PostMapping("/api/v1/rules/sync")
    @PreAuthorize("@perm.check('rule:create')")
    @ResponseStatus(HttpStatus.OK)
    public java.util.Map<String, Object> syncRules() {
        int count = ruleService.syncFromEngine();
        return java.util.Map.of("synced", count);
    }

    // ========== Project Exemptions ==========

    @GetMapping("/api/v1/projects/{projectId}/exemptions")
    @PreAuthorize("@perm.check('rule:read')")
    public List<ExemptionResponse> listExemptions(@PathVariable Long projectId) {
        return ruleService.listExemptions(projectId);
    }

    @PostMapping("/api/v1/projects/{projectId}/exemptions")
    @PreAuthorize("@perm.check('rule:update')")
    @ResponseStatus(HttpStatus.CREATED)
    public ExemptionResponse addExemption(@PathVariable Long projectId, @RequestBody ExemptionRequest req) {
        return ruleService.addExemption(projectId, req);
    }

    @DeleteMapping("/api/v1/projects/{projectId}/exemptions/{ruleId}")
    @PreAuthorize("@perm.check('rule:delete')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeExemption(@PathVariable Long projectId, @PathVariable Long ruleId) {
        ruleService.removeExemption(projectId, ruleId);
    }
}
