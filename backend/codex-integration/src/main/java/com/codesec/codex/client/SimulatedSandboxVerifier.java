package com.codesec.codex.client;

import com.codesec.codex.model.PocResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimulatedSandboxVerifier implements SandboxVerifier {
    private static final Logger log = LoggerFactory.getLogger(SimulatedSandboxVerifier.class);

    private final boolean alwaysPass;

    public SimulatedSandboxVerifier() {
        this(true);
    }

    public SimulatedSandboxVerifier(boolean alwaysPass) {
        this.alwaysPass = alwaysPass;
    }

    @Override
    public SandboxResult verify(PocResult poc) {
        if (poc.getPocType() == null || poc.getPayload() == null) {
            log.warn("Sandbox verification skipped: incomplete POC data");
            return SandboxResult.PENDING;
        }
        SandboxResult result = alwaysPass ? SandboxResult.PASS : SandboxResult.FAIL;
        log.info("Sandbox verification result: {} for POC type: {}", result, poc.getPocType());
        return result;
    }
}
