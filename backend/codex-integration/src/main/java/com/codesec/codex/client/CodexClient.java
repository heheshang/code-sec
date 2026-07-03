package com.codesec.codex.client;

import com.codesec.codex.model.ClientType;
import com.codesec.codex.model.CodexContext;
import com.codesec.codex.model.CodexHealth;

public interface CodexClient {

    String execute(CodexContext context, String systemPrompt, String userPrompt);

    boolean isAvailable();

    CodexHealth health();

    ClientType type();
}
