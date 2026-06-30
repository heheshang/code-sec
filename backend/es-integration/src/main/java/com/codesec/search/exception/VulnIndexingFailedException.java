package com.codesec.search.exception;

/**
 * Thrown when ES indexing fails — triggers transaction rollback.
 * Per E-S2-CRITICAL § 3.8: ES index failure → VulnIndexingFailedException → full rollback.
 * Worker retries 3x with exponential backoff (1s/3s/5s).
 */
public class VulnIndexingFailedException extends RuntimeException {

    private final int attemptNumber;

    public VulnIndexingFailedException(String message, int attemptNumber) {
        super(message);
        this.attemptNumber = attemptNumber;
    }

    public VulnIndexingFailedException(String message, Throwable cause, int attemptNumber) {
        super(message, cause);
        this.attemptNumber = attemptNumber;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }
}
