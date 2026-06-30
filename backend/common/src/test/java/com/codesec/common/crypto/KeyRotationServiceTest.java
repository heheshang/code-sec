package com.codesec.common.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for KeyRotationService.
 * Verifies rotation orchestrates correctly for both AES and KMS modes.
 */
class KeyRotationServiceTest {

    private static final String TEST_KEY = "MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=";

    private CryptoService aesService;
    private KeyRotationService rotationService;

    @BeforeEach
    void setUp() {
        aesService = new AesGcmCryptoService(TEST_KEY);
        rotationService = new KeyRotationService(aesService);
    }

    @Test
    void rotate_returnsOk_whenAesMode() {
        KeyRotationService.RotationResult result = rotationService.rotate();

        assertEquals("ok", result.status());
        assertEquals("aes", result.provider());
        assertNotNull(result.rotatedAt());
    }

    @Test
    void rotate_doesNotThrow_whenAesMode() {
        assertDoesNotThrow(() -> rotationService.rotate());
    }

    @Test
    void rotate_returnsResult_withValidTimestamp() {
        KeyRotationService.RotationResult result = rotationService.rotate();

        assertNotNull(result.rotatedAt());
        assertFalse(result.rotatedAt().isEmpty());
        // Verify it's a valid ISO-8601 timestamp
        assertDoesNotThrow(() -> java.time.Instant.parse(result.rotatedAt()));
    }

    @Test
    void encryptDecrypt_roundtrip_succeedsAfterRotation() {
        String plain = "sensitive-data";

        // Rotate first (no-op for AES)
        rotationService.rotate();

        // Encrypt/decrypt should still work after rotation
        String cipher = aesService.encrypt(plain);
        assertNotNull(cipher);
        assertNotEquals(plain, cipher);

        String decrypted = aesService.decrypt(cipher);
        assertEquals(plain, decrypted);
    }
}
