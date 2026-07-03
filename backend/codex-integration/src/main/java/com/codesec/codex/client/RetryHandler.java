package com.codesec.codex.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public class RetryHandler {
    private static final Logger log = LoggerFactory.getLogger(RetryHandler.class);

    private final int maxAttempts;
    private final long initialDelayMs;
    private final double multiplier;

    public RetryHandler(int maxAttempts, long initialDelayMs, double multiplier) {
        this.maxAttempts = maxAttempts;
        this.initialDelayMs = initialDelayMs;
        this.multiplier = multiplier;
    }

    public <T> RetryResult<T> execute(Supplier<T> action) {
        long delayMs = initialDelayMs;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                T result = action.get();
                if (attempt > 1) {
                    log.info("Retry succeeded on attempt {}", attempt);
                }
                return RetryResult.success(result, attempt);
            } catch (Exception e) {
                log.warn("Attempt {}/{} failed: {}", attempt, maxAttempts, e.getMessage());
                if (attempt == maxAttempts) {
                    return RetryResult.failure(e, attempt);
                }
                sleep(delayMs);
                delayMs = (long) (delayMs * multiplier);
            }
        }
        return RetryResult.failure(new RuntimeException("unreachable"), maxAttempts);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("retry interrupted", e);
        }
    }

    public static class RetryResult<T> {
        private final T result;
        private final Exception exception;
        private final int attempts;
        private final boolean success;

        private RetryResult(T result, Exception exception, int attempts, boolean success) {
            this.result = result;
            this.exception = exception;
            this.attempts = attempts;
            this.success = success;
        }

        public static <T> RetryResult<T> success(T result, int attempts) {
            return new RetryResult<>(result, null, attempts, true);
        }

        public static <T> RetryResult<T> failure(Exception exception, int attempts) {
            return new RetryResult<>(null, exception, attempts, false);
        }

        public T getResult() { return result; }
        public Exception getException() { return exception; }
        public int getAttempts() { return attempts; }
        public boolean isSuccess() { return success; }
    }
}
