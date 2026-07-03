package com.codesec.codex.pipeline;

import com.codesec.codex.model.CodexHealth;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FallbackStrategyTest {

    private final FallbackStrategy strategy = new FallbackStrategy();

    @Test
    void shouldReturnDualApiWhenBothHealthy() {
        CodexHealth codeOk = new CodexHealth(true, "code-model", "1.0", 100);
        CodexHealth llmOk = new CodexHealth(true, "llm-model", "1.0", 200);
        assertEquals(AnalysisPath.DUAL_API, strategy.resolvePath(codeOk, llmOk));
    }

    @Test
    void shouldReturnLlmOnlyWhenCodeModelDown() {
        CodexHealth codeDown = new CodexHealth(false, "code-model", "1.0", 0);
        CodexHealth llmOk = new CodexHealth(true, "llm-model", "1.0", 200);
        assertEquals(AnalysisPath.LLM_ONLY, strategy.resolvePath(codeDown, llmOk));
    }

    @Test
    void shouldReturnCodePlusSastWhenLlmDown() {
        CodexHealth codeOk = new CodexHealth(true, "code-model", "1.0", 100);
        CodexHealth llmDown = new CodexHealth(false, "llm-model", "1.0", 0);
        assertEquals(AnalysisPath.CODE_PLUS_SAST, strategy.resolvePath(codeOk, llmDown));
    }

    @Test
    void shouldReturnSastOnlyWhenBothDown() {
        CodexHealth codeDown = new CodexHealth(false, "code-model", "1.0", 0);
        CodexHealth llmDown = new CodexHealth(false, "llm-model", "1.0", 0);
        assertEquals(AnalysisPath.SAST_ONLY, strategy.resolvePath(codeDown, llmDown));
    }
}
