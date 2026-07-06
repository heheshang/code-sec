package com.codesec.api.module.internal.controller;

import com.codesec.domain.entity.ScanTaskEntity;
import com.codesec.domain.repository.ScanTaskRepository;
import com.codesec.domain.service.VulnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/internal")
@Slf4j
public class InternalController {
    private final VulnService vulnService;
    private final ScanTaskRepository scanTaskRepo;
    private final String internalSecret;

    public InternalController(VulnService vulnService, ScanTaskRepository scanTaskRepo,
                              @Value("${codesec.internal.secret:}") String internalSecret) {
        this.vulnService = vulnService;
        this.scanTaskRepo = scanTaskRepo;
        this.internalSecret = internalSecret;
    }

    @PostMapping("/scan-callback")
    public Map<String, Object> scanCallback(@RequestBody Map<String, Object> body,
                                             @RequestHeader(value = "X-Internal-Token", required = false) String token) {
        if (internalSecret == null || internalSecret.isBlank()) {
            log.error("Internal endpoint called but codesec.internal.secret is not configured");
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Internal secret not configured");
        }
        if (token == null || !token.equals(internalSecret)) {
            log.warn("Internal callback rejected: missing or invalid X-Internal-Token");
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid internal token");
        }

        Object scanIdObj = body.get("scanId");
        Long scanId = scanIdObj instanceof Number n ? n.longValue() : null;
        String status = (String) body.getOrDefault("status", "completed");

        if (scanId != null) {
            scanTaskRepo.findById(scanId).ifPresent(task -> {
                task.setStatus(status);
                if ("completed".equals(status)) {
                    task.setFinishedAt(LocalDateTime.now());
                }
                if ("failed".equals(status)) {
                    task.setFinishedAt(LocalDateTime.now());
                    task.setErrorMessage((String) body.getOrDefault("error", "Unknown error"));
                }
                scanTaskRepo.save(task);
            });
        }

        return Map.of("acknowledged", true);
    }
}
