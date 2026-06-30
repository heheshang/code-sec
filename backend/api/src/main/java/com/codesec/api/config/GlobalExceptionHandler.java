package com.codesec.api.config;

import com.codesec.api.module.ticket.statemachine.IllegalStateTransitionException;
import com.codesec.common.crypto.CryptoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalStateTransitionException.class)
    public ResponseEntity<Map<String, String>> handleIllegalTransition(IllegalStateTransitionException e) {
        return ResponseEntity.status(409).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(CryptoException.class)
    public ResponseEntity<Map<String, String>> handleCrypto(CryptoException e) {
        return ResponseEntity.status(500).body(Map.of("message", "Token decryption failed"));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException e) {
        String msg = e.getMessage() != null ? e.getMessage() : "Internal error";
        if (msg.contains("not found")) {
            return ResponseEntity.status(404).body(Map.of("message", msg));
        }
        if (msg.contains("Invalid")) {
            return ResponseEntity.status(400).body(Map.of("message", msg));
        }
        return ResponseEntity.status(500).body(Map.of("message", msg));
    }
}
