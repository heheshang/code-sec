package com.codesec.common.crypto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Orchestrates crypto key rotation across the system.
 * <p>
 * Rotation workflow:
 * <ol>
 *   <li>Call {@link CryptoService#rotate()} to rotate the key at the provider level</li>
 *   <li>Log rotation result with timestamp and provider info</li>
 *   <li>In KMS mode, the Alibaba Cloud KMS rotates the CMK version</li>
 *   <li>In AES mode, rotation is a no-op (key is set via config)</li>
 * </ol>
 * <p>
 * Future: add async re-encryption of existing data and persistence of rotation records.
 */
@Service
public class KeyRotationService {

    private static final Logger log = LoggerFactory.getLogger(KeyRotationService.class);

    private final CryptoService cryptoService;

    public KeyRotationService(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    /**
     * Execute a key rotation cycle.
     *
     * @return rotation result with status, provider, and timestamp
     */
    public RotationResult rotate() {
        String provider = cryptoService.isKms() ? "kms" : "aes";
        Instant start = Instant.now();

        try {
            cryptoService.rotate();
            log.info("Key rotation completed successfully: provider={}", provider);
            return new RotationResult("ok", provider, start.toString());
        } catch (Exception e) {
            log.error("Key rotation failed: provider={}", provider, e);
            return new RotationResult("failed", provider, start.toString());
        }
    }

    /** Rotation result DTO. */
    public record RotationResult(String status, String provider, String rotatedAt) {}
}
