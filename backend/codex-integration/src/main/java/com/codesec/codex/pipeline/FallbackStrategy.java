package com.codesec.codex.pipeline;

import com.codesec.codex.model.CodexHealth;

public class FallbackStrategy {

    public AnalysisPath resolvePath(CodexHealth codeModelHealth, CodexHealth llmHealth) {
        if (codeModelHealth.isOk() && llmHealth.isOk()) {
            return AnalysisPath.DUAL_API;
        }
        if (!codeModelHealth.isOk() && llmHealth.isOk()) {
            return AnalysisPath.LLM_ONLY;
        }
        if (codeModelHealth.isOk() && !llmHealth.isOk()) {
            return AnalysisPath.CODE_PLUS_SAST;
        }
        return AnalysisPath.SAST_ONLY;
    }
}
