package com.codesec.api.module.ai;

import com.codesec.codex.CodexAdapter;
import com.codesec.codex.model.CodexResponse;
import com.codesec.codex.model.FallbackLevel;
import com.codesec.domain.entity.VulnFindingEntity;
import com.codesec.domain.repository.VulnFindingRepository;
import com.codesec.domain.service.ai.AiAnalysisResult;
import com.codesec.domain.service.ai.AiAnalysisService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiAnalysisServiceTest {

    @Mock
    private CodexAdapter codexAdapter;

    @Mock
    private VulnFindingRepository vulnRepo;

    @InjectMocks
    private AiAnalysisService service;

    private VulnFindingEntity makeVuln(Long id) {
        VulnFindingEntity e = new VulnFindingEntity();
        e.setId(id);
        e.setScanTaskId(1L);
        e.setProjectId(1L);
        e.setRuleId("rule-1");
        e.setSeverity("high");
        e.setTitle("Test vuln");
        e.setDescription("A test vulnerability");
        e.setCodeSnippet("int x = 1;");
        e.setFilePath("src/Test.java");
        e.setLineStart(1);
        e.setLineEnd(10);
        return e;
    }

    @Test
    void analyze_WhenCodexSucceeds_SavesEntityWithAiFields() {
        VulnFindingEntity vuln = makeVuln(42L);
        when(vulnRepo.findById(42L)).thenReturn(Optional.of(vuln));
        when(codexAdapter.analyzeVuln(any())).thenReturn(
            CodexResponse.success("exploitable with confidence 0.92", Duration.ofMillis(1500), "gpt-4o")
        );

        AiAnalysisResult result = service.analyze(42L);

        assertEquals("exploitable", result.getAiVerdict());
        assertEquals(0.92, result.getAiConfidence(), 0.01);
        assertEquals("gpt-4o", result.getModelVersion());
        verify(vulnRepo).save(vuln);
    }

    @Test
    void analyze_WhenCodexFails_SavesEntityWithDefaultVerdict() {
        VulnFindingEntity vuln = makeVuln(99L);
        when(vulnRepo.findById(99L)).thenReturn(Optional.of(vuln));
        when(codexAdapter.analyzeVuln(any())).thenReturn(
            CodexResponse.failure("API rate limit exceeded", FallbackLevel.API_FALLBACK)
        );

        AiAnalysisResult result = service.analyze(99L);

        assertEquals("suspicious", result.getAiVerdict());
        assertEquals(0.0, result.getAiConfidence(), 0.01);
        assertTrue(result.getAiExplanation().contains("API rate limit exceeded"));
        verify(vulnRepo).save(vuln);
    }

    @Test
    void analyze_WhenSaveThrows_StillReturnsResult() {
        VulnFindingEntity vuln = makeVuln(7L);
        when(vulnRepo.findById(7L)).thenReturn(Optional.of(vuln));
        when(codexAdapter.analyzeVuln(any())).thenReturn(
            CodexResponse.success("false positive: not exploitable", Duration.ofMillis(500), "gpt-4o-mini")
        );
        doThrow(new RuntimeException("DB connection lost")).when(vulnRepo).save(any());

        AiAnalysisResult result = service.analyze(7L);

        assertEquals("false_positive", result.getAiVerdict());
        assertNotNull(result.getAiExplanation());
        verify(vulnRepo).save(vuln);
    }

    @Test
    void analyze_WhenVulnNotFound_Throws() {
        when(vulnRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.analyze(999L));
        verify(codexAdapter, never()).analyzeVuln(any());
        verify(vulnRepo, never()).save(any());
    }
}
