package com.codesec.codex.capability;

import com.codesec.codex.model.CodexCapability;
import com.codesec.codex.model.CodexRequest;
import com.codesec.codex.model.CodexResponse;

public interface Capability<T> {
    CodexCapability getType();
    CodexResponse<T> execute(CodexRequest request);
    boolean isEnabled();
}
