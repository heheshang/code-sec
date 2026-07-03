package com.codesec.codex.client;

public class CircuitBreaker {
    public enum State { CLOSED, HALF_OPEN, OPEN }

    private final int failureThreshold;
    private final long resetTimeoutMs;
    private State state;
    private int failureCount;
    private long lastFailureTime;

    public CircuitBreaker(int failureThreshold, long resetTimeoutMs) {
        this.failureThreshold = failureThreshold;
        this.resetTimeoutMs = resetTimeoutMs;
        this.state = State.CLOSED;
        this.failureCount = 0;
    }

    public synchronized boolean isAvailable() {
        if (state == State.CLOSED) {
            return true;
        }
        if (state == State.HALF_OPEN) {
            return true;
        }
        if (System.currentTimeMillis() - lastFailureTime >= resetTimeoutMs) {
            state = State.HALF_OPEN;
            return true;
        }
        return false;
    }

    public synchronized void onSuccess() {
        if (state == State.HALF_OPEN) {
            state = State.CLOSED;
        }
        failureCount = 0;
    }

    public synchronized void onFailure() {
        if (state == State.HALF_OPEN) {
            state = State.OPEN;
            lastFailureTime = System.currentTimeMillis();
            return;
        }
        failureCount++;
        if (failureCount >= failureThreshold) {
            state = State.OPEN;
            lastFailureTime = System.currentTimeMillis();
        }
    }

    public synchronized State getState() { return state; }
    public synchronized int getFailureCount() { return failureCount; }

    public synchronized void reset() {
        state = State.CLOSED;
        failureCount = 0;
    }
}
