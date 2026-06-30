package com.codesec.sample;

import javax.crypto.Cipher;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Sample code demonstrating weak cryptographic algorithm usage.
 * Finding expected at line 11 (MD5 usage - weak hash).
 */
public class CipherUtil {

    // VULNERABLE: MD5 is cryptographically broken - line 11
    public byte[] hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        return md.digest(password.getBytes());
    }

    // SAFE: SHA-256 is currently considered strong
    public byte[] hashPasswordSecure(String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return md.digest(password.getBytes());
    }
}
