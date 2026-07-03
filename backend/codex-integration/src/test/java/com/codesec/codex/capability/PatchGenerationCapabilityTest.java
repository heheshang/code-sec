package com.codesec.codex.capability;

import com.codesec.codex.client.AstCompiler;
import com.codesec.codex.client.CodexClient;
import com.codesec.codex.config.CodexProperties;
import com.codesec.codex.model.CodexRequest;
import com.codesec.codex.model.CodexResponse;
import com.codesec.codex.model.CodexCapability;
import com.codesec.codex.model.PatchResult;
import com.codesec.codex.prompt.PromptLoader;
import com.codesec.codex.prompt.PromptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PatchGenerationCapabilityTest {

    private PatchGenerationCapability capability;
    private CodexClient mockClient;
    private CodexProperties props;

    @BeforeEach
    void setUp() {
        mockClient = mock(CodexClient.class);
        when(mockClient.execute(any(), any(), any())).thenReturn(
            "public class Test {\n    public void foo() {\n        int x = 1;\n    }\n}");
        PromptLoader loader = new PromptLoader();
        PromptRepository repo = new PromptRepository(loader);
        props = new CodexProperties();
        props.getCapabilities().setPatchGeneration(true);
        AstCompiler compiler = new AstCompiler();
        capability = new PatchGenerationCapability(mockClient, repo, props, compiler);
    }

    @Test
    void getTypeReturnsPatchGeneration() {
        assertEquals(CodexCapability.PATCH_GENERATION, capability.getType());
    }

    @Test
    void isEnabledWhenConfigured() {
        assertTrue(capability.isEnabled());
    }

    @Test
    void isDisabledWhenConfiguredFalse() {
        props.getCapabilities().setPatchGeneration(false);
        assertFalse(capability.isEnabled());
    }

    @Test
    void executeReturnsCompilationStatus() {
        CodexRequest request = new CodexRequest();
        request.setLanguage("java");
        request.setCodeSnippet("public class Test { }");

        CodexResponse<PatchResult> response = capability.execute(request);
        assertTrue(response.isSuccess());
        PatchResult result = response.getData();
        assertNotNull(result.getCompilationStatus());
    }

    @Test
    void executeWhenDisabledReturnsFailure() {
        props.getCapabilities().setPatchGeneration(false);
        CodexRequest request = new CodexRequest();
        CodexResponse<PatchResult> response = capability.execute(request);
        assertFalse(response.isSuccess());
        assertTrue(response.getErrorMessage().contains("disabled"));
    }
}
