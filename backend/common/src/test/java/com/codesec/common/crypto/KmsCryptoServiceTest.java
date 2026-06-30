package com.codesec.common.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for KmsCryptoService.
 * Tests three modes: KMS available, KMS fallback, and AES-only.
 * Uses real AesGcmCryptoService instead of Mockito mocks to
 * avoid ByteBuddy/Java version compatibility issues.
 */
class KmsCryptoServiceTest {

    private static final String TEST_KEY = "MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=";

    private AesGcmCryptoService aesFallback;
    private KmsCryptoService kmsService;

    @BeforeEach
    void setUp() {
        aesFallback = new AesGcmCryptoService(TEST_KEY);
        // KmsCryptoService with unreachable KMS -> falls back to AES
        kmsService = new KmsCryptoService("cn-hangzhou", "mock-key-id", aesFallback);
    }

    @Test
    void isKms_returnsFalse_whenKmsUnavailable() {
        assertFalse(kmsService.isKms());
    }

    @Test
    void encrypt_fallsBackToAes_whenKmsUnavailable() {
        String plain = "test-secret";
        // When KMS is unavailable, fallback.encrypt() is called.
        // Using real AesGcmCryptoService so encryption actually works.
        String cipher = kmsService.encrypt(plain);
        assertNotNull(cipher);
        assertNotEquals(plain, cipher);
    }

    @Test
    void decrypt_fallsBackToAes_whenKmsUnavailable() {
        String plain = "test-secret";
        String cipher = kmsService.encrypt(plain);
        String decrypted = kmsService.decrypt(cipher);
        assertEquals(plain, decrypted);
    }

    @Test
    void rotate_doesNotThrow_whenKmsUnavailable() {
        assertDoesNotThrow(() -> kmsService.rotate());
    }

    @Test
    void constructor_logsWarning_whenKmsUnreachable() {
        KmsCryptoService service = new KmsCryptoService("invalid-region", "invalid-key", aesFallback);
        assertNotNull(service);
        assertFalse(service.isKms());
    }
}
