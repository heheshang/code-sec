package com.codesec.api.module.audit;

import com.codesec.domain.dto.*;
import com.codesec.domain.service.audit.AuditService;
import com.codesec.api.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuditController {
    private final AuditService auditService;

    @PostMapping("/audits")
    @PreAuthorize("@perm.check('vuln:audit')")
    @ResponseStatus(HttpStatus.CREATED)
    public AuditResponse submit(@RequestBody AuditSubmitRequest req, @AuthenticationPrincipal UserPrincipal user) {
        return auditService.submitAudit(req, user.getUserId());
    }

    @GetMapping("/vulns/{vulnId}/audits")
    @PreAuthorize("@perm.check('vuln:read')")
    public List<AuditResponse> history(@PathVariable Long vulnId) {
        return auditService.getAuditHistory(vulnId);
    }
}
