package com.codesec.api.module.admin.controller;

import com.codesec.common.crypto.KeyRotationService;
import com.codesec.common.crypto.KeyRotationService.RotationResult;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/crypto")
public class AdminCryptoController {

    private final KeyRotationService keyRotationService;

    public AdminCryptoController(KeyRotationService keyRotationService) {
        this.keyRotationService = keyRotationService;
    }

    @PostMapping("/rotate")
    @PreAuthorize("hasAuthority('admin:crypto')")
    public ResponseEntity<Map<String, Object>> rotate() {
        RotationResult result = keyRotationService.rotate();
        return ResponseEntity.ok(Map.of(
                "status", result.status(),
                "provider", result.provider(),
                "rotatedAt", result.rotatedAt()
        ));
    }
}
