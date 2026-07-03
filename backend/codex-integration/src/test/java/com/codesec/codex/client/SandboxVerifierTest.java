package com.codesec.codex.client;

import com.codesec.codex.model.PocResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SandboxVerifierTest {

    @Test
    void simulatedVerifierReturnsPass() {
        SimulatedSandboxVerifier verifier = new SimulatedSandboxVerifier(true);
        PocResult poc = new PocResult("SQL_INJECTION", "GET",
            "/api/users?id=1", "' OR 1=1 --", "HTTP 200", "test");
        assertEquals(SandboxVerifier.SandboxResult.PASS, verifier.verify(poc));
    }

    @Test
    void simulatedVerifierReturnsFail() {
        SimulatedSandboxVerifier verifier = new SimulatedSandboxVerifier(false);
        PocResult poc = new PocResult("SQL_INJECTION", "GET",
            "/api/users?id=1", "' OR 1=1 --", "HTTP 200", "test");
        assertEquals(SandboxVerifier.SandboxResult.FAIL, verifier.verify(poc));
    }

    @Test
    void incompletePocReturnsPending() {
        SimulatedSandboxVerifier verifier = new SimulatedSandboxVerifier(true);
        PocResult poc = new PocResult();
        assertEquals(SandboxVerifier.SandboxResult.PENDING, verifier.verify(poc));
    }
}
