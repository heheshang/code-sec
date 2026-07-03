package com.codesec.codex.client;

import java.time.Instant;

public class RateLimiter {
    private final long capacity;
    private final double refillRate;
    private double tokens;
    private Instant lastRefill;

    public RateLimiter(long capacity, double refillRate) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.tokens = capacity;
        this.lastRefill = Instant.now();
    }

    public synchronized boolean tryAcquire() {
        refill();
        if (tokens >= 1.0) {
            tokens -= 1.0;
            return true;
        }
        return false;
    }

    public synchronized boolean tryAcquire(int permits) {
        refill();
        if (tokens >= permits) {
            tokens -= permits;
            return true;
        }
        return false;
    }

    private void refill() {
        Instant now = Instant.now();
        long elapsedMs = now.toEpochMilli() - lastRefill.toEpochMilli();
        if (elapsedMs > 0) {
            tokens = Math.min(capacity, tokens + elapsedMs * refillRate / 1_000);
            lastRefill = now;
        }
    }
}
