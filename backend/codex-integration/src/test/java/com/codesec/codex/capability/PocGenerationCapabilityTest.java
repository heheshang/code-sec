package com.codesec.codex.capability;

import com.codesec.codex.client.CodexClient;
import com.codesec.codex.client.SandboxVerifier;
import com.codesec.codex.client.SimulatedSandboxVerifier;
import com.codesec.codex.config.CodexProperties;
import com.codesec.codex.model.CodexRequest;
import com.codesec.codex.model.CodexResponse;
import com.codesec.codex.model.CodexCapability;
import com.codesec.codex.model.PocResult;
import com.codesec.codex.prompt.PromptLoader;
import com.codesec.codex.prompt.PromptRepository;
import com.codesec.codex.prompt.PromptTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PocGenerationCapabilityTest {

    private PocGenerationCapability capability;
    private CodexClient mockClient;
    private CodexProperties props;

    @BeforeEach
    void setUp() {
        mockClient = mock(CodexClient.class);
        when(mockClient.execute(any(), any(), any())).thenReturn(
            "GET /api/users?id=1 HTTP/1.1\npayload: ' OR 1=1 --\nexpected: 200 OK");
        PromptLoader loader = new PromptLoader();
        PromptRepository repo = new PromptRepository(loader);
        props = new CodexProperties();
        props.getCapabilities().setPocGeneration(true);
        SandboxVerifier verifier = new SimulatedSandboxVerifier(true);
        capability = new PocGenerationCapability(mockClient, repo, props, verifier);
    }

    @Test
    void getTypeReturnsPocGeneration() {
        assertEquals(CodexCapability.POC_GENERATION, capability.getType());
    }

    @Test
    void isEnabledWhenConfigured() {
        assertTrue(capability.isEnabled());
    }

    @Test
    void isDisabledWhenConfiguredFalse() {
        props.getCapabilities().setPocGeneration(false);
        assertFalse(capability.isEnabled());
    }

    @Test
    void executeReturnsPocResultWithRequiredFields() {
        CodexRequest request = new CodexRequest();
        request.setLanguage("java");
        request.setCodeSnippet("String sql = \"SELECT * FROM users WHERE id = \" + userId;");

        CodexResponse<PocResult> response = capability.execute(request);
        assertTrue(response.isSuccess());
        PocResult poc = response.getData();
        assertNotNull(poc);
        assertNotNull(poc.getPocType());
        assertNotNull(poc.getHttpMethod());
        assertNotNull(poc.getSandboxStatus());
    }

    @Test
    void executeReturnsSandboxStatus() {
        CodexRequest request = new CodexRequest();
        request.setCodeSnippet("test");

        CodexResponse<PocResult> response = capability.execute(request);
        PocResult poc = response.getData();
        assertNotNull(poc.getSandboxStatus());
        assertTrue(poc.getSandboxStatus().equals("PASS")
            || poc.getSandboxStatus().equals("FAIL")
            || poc.getSandboxStatus().equals("PENDING"));
    }

    @Test
    void executeWhenDisabledReturnsFailure() {
        props.getCapabilities().setPocGeneration(false);
        CodexRequest request = new CodexRequest();
        CodexResponse<PocResult> response = capability.execute(request);
        assertFalse(response.isSuccess());
        assertTrue(response.getErrorMessage().contains("disabled"));
    }
}
