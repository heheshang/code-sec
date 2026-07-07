package com.codesec.domain.service.ai;

import com.codesec.common.exception.NotFoundException;
import com.codesec.codex.CodexAdapter;
import com.codesec.codex.model.*;
import com.codesec.domain.entity.VulnFindingEntity;
import com.codesec.domain.repository.VulnFindingRepository;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;
import java.util.Map;

@Service
public class AiAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisService.class);

    private final CodexAdapter codexAdapter;
    private final VulnFindingRepository vulnRepo;

    public AiAnalysisService(CodexAdapter codexAdapter, VulnFindingRepository vulnRepo) {
        this.codexAdapter = codexAdapter;
        this.vulnRepo = vulnRepo;
    }

    public AiAnalysisResult analyze(Long vulnId) {
        VulnFindingEntity vuln = vulnRepo.findById(vulnId)
            .orElseThrow(() -> new NotFoundException("Vuln not found: " + vulnId));

        CodexRequest request = buildRequest(vuln);

        CodexResponse<String> analysisResp = codexAdapter.analyzeVuln(request);

        AiAnalysisResult result = new AiAnalysisResult();
        result.setVulnId(String.valueOf(vulnId));
       result.setAnalyzedAt(Instant.now().toString());
        result.setModelVersion(analysisResp.getModelVersion() != null ? analysisResp.getModelVersion() : "unknown");
        result.setDurationMs(analysisResp.getDuration() != null ? analysisResp.getDuration().toMillis() : 0);
        result.setFallbackLevel(analysisResp.getFallbackLevel() != null ? analysisResp.getFallbackLevel().name() : "NONE");

        if (analysisResp.isSuccess() && analysisResp.getData() != null) {
            String verdict = extractVerdict(analysisResp.getData());
            result.setAiVerdict(verdict);
            result.setAiConfidence(extractConfidence(analysisResp.getData(), verdict));
            result.setAiExplanation(analysisResp.getData());
        } else {
            result.setAiVerdict("suspicious");
            result.setAiConfidence(0.0);
            result.setAiExplanation(analysisResp.getErrorMessage() != null
                ? "Analysis failed: " + analysisResp.getErrorMessage() : "Analysis unavailable");
        }

        vuln.setAiVerdict(result.getAiVerdict());
        vuln.setAiConfidence(result.getAiConfidence());
        vuln.setAiExplanation(result.getAiExplanation());
        try {
            vulnRepo.save(vuln);
            log.info("AI analysis persisted: vulnId={}, verdict={}, confidence={}", vulnId, result.getAiVerdict(), result.getAiConfidence());
        } catch (Exception e) {
            log.error("Failed to persist AI analysis for vulnId={}: {}", vulnId, e.getMessage());
        }

        return result;
    }

    private CodexRequest buildRequest(VulnFindingEntity vuln) {
        CodexRequest req = new CodexRequest(
            String.valueOf(vuln.getScanTaskId()),
            String.valueOf(vuln.getId()),
            detectLanguage(vuln.getFilePath()),
            vuln.getCodeSnippet() != null ? vuln.getCodeSnippet() : "",
            vuln.getFilePath(),
            vuln.getLineStart(),
            vuln.getLineEnd()
        );
        req.setExtra(Map.of(
            "ruleId", vuln.getRuleId() != null ? vuln.getRuleId() : "",
            "severity", vuln.getSeverity() != null ? vuln.getSeverity() : "",
            "cwe", vuln.getCwe() != null ? vuln.getCwe() : "",
            "title", vuln.getTitle() != null ? vuln.getTitle() : ""
        ));
        return req;
    }

    private String detectLanguage(String filePath) {
        if (filePath == null) return "java";
        int dot = filePath.lastIndexOf('.');
        if (dot < 0) return "java";
        String ext = filePath.substring(dot);
        return switch (ext) {
            case ".java" -> "java";
            case ".kt" -> "java";
            case ".go" -> "go";
            case ".py" -> "python";
            case ".ts", ".tsx" -> "typescript";
            case ".js", ".jsx" -> "javascript";
            case ".php" -> "php";
            case ".cs" -> "csharp";
            default -> "java";
        };
    }

    private String extractVerdict(String analysis) {
        if (analysis == null) return "suspicious";
        String lower = analysis.toLowerCase();
        if (lower.contains("false_positive") || lower.contains("false positive")
            || lower.contains("no vulnerability") || lower.contains("not exploitable")) {
            return "false_positive";
        }
        if (lower.contains("exploitable") || lower.contains("confirmed")
            || lower.contains("vulnerable") || lower.contains("high risk")) {
            return "exploitable";
        }
        return "suspicious";
    }

    private double extractConfidence(String analysis, String verdict) {
        if (analysis == null) return 0.0;
        if (analysis.contains("confidence")) {
            try {
                int idx = analysis.indexOf("confidence");
                String sub = analysis.substring(idx);
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("([0-9]\\.[0-9]+)").matcher(sub);
                if (m.find()) return Double.parseDouble(m.group(1));
            } catch (Exception ignored) {}
        }
        return "exploitable".equals(verdict) ? 0.85 : 0.5;
    }
}
