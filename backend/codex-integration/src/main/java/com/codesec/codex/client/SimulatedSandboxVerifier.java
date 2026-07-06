package com.codesec.codex.client;

import com.codesec.codex.model.PocResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimulatedSandboxVerifier implements SandboxVerifier {
    private static final Logger log = LoggerFactory.getLogger(SimulatedSandboxVerifier.class);

    public SimulatedSandboxVerifier() {}

    @Override
    public SandboxResult verify(PocResult poc) {
        if (poc.getPocType() == null || poc.getPayload() == null) {
            log.warn("Sandbox verification skipped: incomplete POC data");
            return SandboxResult.PENDING;
        }
        // No real sandbox backend is wired; report PENDING so the UI does not
        // present an unverified PoC as verified.
        log.info("Sandbox verification unavailable (no backend): PENDING for POC type: {}", poc.getPocType());
        return SandboxResult.PENDING;
    }
}
