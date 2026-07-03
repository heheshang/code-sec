package com.codesec.codex.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CircuitBreakerTest {

    @Test
    void shouldStartClosed() {
        CircuitBreaker cb = new CircuitBreaker(5, 60000);
        assertEquals(CircuitBreaker.State.CLOSED, cb.getState());
        assertTrue(cb.isAvailable());
    }

    @Test
    void shouldOpenAfterThreshold() {
        CircuitBreaker cb = new CircuitBreaker(3, 60000);
        for (int i = 0; i < 3; i++) {
            cb.onFailure();
        }
        assertEquals(CircuitBreaker.State.OPEN, cb.getState());
        assertFalse(cb.isAvailable());
    }

    @Test
    void shouldTransitionToHalfOpenAfterResetTimeout() throws InterruptedException {
        CircuitBreaker cb = new CircuitBreaker(2, 100);
        cb.onFailure();
        cb.onFailure();
        assertEquals(CircuitBreaker.State.OPEN, cb.getState());
        Thread.sleep(150);
        assertTrue(cb.isAvailable());
        assertEquals(CircuitBreaker.State.HALF_OPEN, cb.getState());
    }

    @Test
    void shouldCloseOnSuccessFromHalfOpen() throws InterruptedException {
        CircuitBreaker cb = new CircuitBreaker(2, 50);
        cb.onFailure();
        cb.onFailure();
        assertEquals(CircuitBreaker.State.OPEN, cb.getState());
        Thread.sleep(100);
        assertTrue(cb.isAvailable());
        cb.onSuccess();
        assertEquals(CircuitBreaker.State.CLOSED, cb.getState());
        assertEquals(0, cb.getFailureCount());
    }

    @Test
    void shouldReopenOnFailureFromHalfOpen() throws InterruptedException {
        CircuitBreaker cb = new CircuitBreaker(2, 50);
        cb.onFailure();
        cb.onFailure();
        Thread.sleep(100);
        cb.isAvailable();
        cb.onFailure();
        assertEquals(CircuitBreaker.State.OPEN, cb.getState());
    }

    @Test
    void shouldResetManually() {
        CircuitBreaker cb = new CircuitBreaker(2, 60000);
        cb.onFailure();
        cb.onFailure();
        assertEquals(CircuitBreaker.State.OPEN, cb.getState());
        cb.reset();
        assertEquals(CircuitBreaker.State.CLOSED, cb.getState());
        assertEquals(0, cb.getFailureCount());
    }
}
