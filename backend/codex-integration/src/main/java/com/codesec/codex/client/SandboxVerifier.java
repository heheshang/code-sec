package com.codesec.codex.client;

import com.codesec.codex.model.PocResult;

public interface SandboxVerifier {
    SandboxResult verify(PocResult poc);

    enum SandboxResult {
        PASS,
        FAIL,
        PENDING
    }
}
