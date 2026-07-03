package com.codesec.codex.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RetryHandlerTest {

    @Test
    void shouldSucceedOnFirstAttempt() {
        RetryHandler retry = new RetryHandler(3, 10, 2);
        RetryHandler.RetryResult<String> result = retry.execute(() -> "ok");
        assertTrue(result.isSuccess());
        assertEquals("ok", result.getResult());
        assertEquals(1, result.getAttempts());
    }

    @Test
    void shouldRetryAndSucceed() {
        RetryHandler retry = new RetryHandler(3, 10, 2);
        int[] attempts = {0};
        RetryHandler.RetryResult<String> result = retry.execute(() -> {
            attempts[0]++;
            if (attempts[0] < 2) {
                throw new RuntimeException("transient error");
            }
            return "ok";
        });
        assertTrue(result.isSuccess());
        assertEquals("ok", result.getResult());
        assertEquals(2, result.getAttempts());
    }

    @Test
    void shouldExhaustRetries() {
        RetryHandler retry = new RetryHandler(3, 10, 2);
        RetryHandler.RetryResult<String> result = retry.execute(() -> {
            throw new RuntimeException("persistent error");
        });
        assertFalse(result.isSuccess());
        assertEquals(3, result.getAttempts());
        assertNotNull(result.getException());
    }

    @Test
    void shouldStopOnSuccess() {
        RetryHandler retry = new RetryHandler(5, 10, 2);
        int[] attempts = {0};
        RetryHandler.RetryResult<String> result = retry.execute(() -> {
            attempts[0]++;
            if (attempts[0] < 3) {
                throw new RuntimeException("transient");
            }
            return "ok";
        });
        assertTrue(result.isSuccess());
        assertEquals(3, result.getAttempts());
    }
}
