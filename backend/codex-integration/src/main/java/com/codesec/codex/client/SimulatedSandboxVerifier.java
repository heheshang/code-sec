package com.codesec.codex.client;

import com.codesec.codex.model.PocResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "codex.sandbox.enabled", havingValue = "true", matchIfMissing = true)
public class SimulatedSandboxVerifier implements SandboxVerifier {
    private static final Logger log = LoggerFactory.getLogger(SimulatedSandboxVerifier.class);

    @Override
    public SandboxResult verify(PocResult poc) {
        if (poc.getPocType() == null || poc.getPayload() == null) {
            log.warn("Sandbox verification skipped: incomplete POC data");
            return SandboxResult.PENDING;
        }
        log.info("Sandbox verification unavailable (no backend): PENDING for POC type: {}", poc.getPocType());
        return SandboxResult.PENDING;
    }
}
