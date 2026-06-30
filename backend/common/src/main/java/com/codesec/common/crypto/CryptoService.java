package com.codesec.common.crypto;

/** Crypto interface. Sprint 2 uses AES-256 yml key; Sprint 3 adds KMS implementation. */
public interface CryptoService {
    String encrypt(String plain);
    String decrypt(String cipher);
    /** Trigger key rotation. KMS mode: create new key version + re-encrypt. AES mode: no-op. */
    void rotate();
    /** Whether this provider uses a remote KMS. */
    boolean isKms();
}
