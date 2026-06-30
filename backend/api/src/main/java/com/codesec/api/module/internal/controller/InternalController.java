package com.codesec.api.module.internal.controller;

import com.codesec.api.domain.entity.ScanTaskEntity;
import com.codesec.api.domain.repository.ScanTaskRepository;
import com.codesec.api.module.vuln.VulnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/internal")
@RequiredArgsConstructor
@Slf4j
public class InternalController {
    private final VulnService vulnService;
    private final ScanTaskRepository scanTaskRepo;

    @PostMapping("/scan-callback")
    public Map<String, Object> scanCallback(@RequestBody Map<String, Object> body) {
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
