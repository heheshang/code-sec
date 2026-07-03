package com.codesec.codex.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RateLimiterTest {

    @Test
    void shouldAcquireTokenWithinCapacity() {
        RateLimiter limiter = new RateLimiter(10, 10);
        for (int i = 0; i < 10; i++) {
            assertTrue(limiter.tryAcquire(), "should acquire token " + i);
        }
    }

    @Test
    void shouldRejectWhenExhausted() {
        RateLimiter limiter = new RateLimiter(3, 1);
        for (int i = 0; i < 3; i++) {
            limiter.tryAcquire();
        }
        assertFalse(limiter.tryAcquire());
    }

    @Test
    void shouldRefillOverTime() throws InterruptedException {
        RateLimiter limiter = new RateLimiter(1, 10);
        assertTrue(limiter.tryAcquire());
        assertFalse(limiter.tryAcquire());
        Thread.sleep(5);
        assertFalse(limiter.tryAcquire());
        Thread.sleep(200);
        assertTrue(limiter.tryAcquire());
    }

    @Test
    void shouldAcquireMultiplePermits() {
        RateLimiter limiter = new RateLimiter(10, 10);
        assertTrue(limiter.tryAcquire(5));
        assertTrue(limiter.tryAcquire(5));
        assertFalse(limiter.tryAcquire(1));
    }
}
