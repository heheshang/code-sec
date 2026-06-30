package com.codesec.sample;

/**
 * Sample code demonstrating hardcoded password vulnerability.
 * Finding expected at line 8 (hardcoded database password).
 */
public class Config {
    // VULNERABLE: Hardcoded password - line 8
    public static final String DB_PASSWORD = "admin123!";

    // VULNERABLE: Hardcoded API key - line 11
    public static final String API_KEY_SECRET = "sk-1234567890abcdef";

    // SAFE: Password loaded from environment
    public static final String DB_USERNAME = System.getenv("DB_USERNAME");

    // SAFE: Port number is not a secret
    public static final int DB_PORT = 5432;
}
