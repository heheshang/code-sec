package com.codesec.codex.pipeline;

import com.codesec.codex.CodexAdapter;
import com.codesec.codex.capability.*;
import com.codesec.codex.client.CodexClient;
import com.codesec.codex.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CodexAdapterImpl implements CodexAdapter {
    private static final Logger log = LoggerFactory.getLogger(CodexAdapterImpl.class);

    private final VulnAnalysisCapability vulnAnalysis;
    private final FalsePositiveFilterCapability fpFilter;
    private final LogicVulnMiningCapability logicVulnMining;
    private final PocGenerationCapability pocGeneration;
    private final PatchGenerationCapability patchGeneration;
    private final CodexClient codeModelClient;
    private final CodexClient llmModelClient;

    public CodexAdapterImpl(VulnAnalysisCapability vulnAnalysis,
                            FalsePositiveFilterCapability fpFilter,
                            LogicVulnMiningCapability logicVulnMining,
                            PocGenerationCapability pocGeneration,
                            PatchGenerationCapability patchGeneration,
                            CodexClient codeModelClient,
                            CodexClient llmModelClient) {
        this.vulnAnalysis = vulnAnalysis;
        this.fpFilter = fpFilter;
        this.logicVulnMining = logicVulnMining;
        this.pocGeneration = pocGeneration;
        this.patchGeneration = patchGeneration;
        this.codeModelClient = codeModelClient;
        this.llmModelClient = llmModelClient;
    }

    @Override
    public CodexResponse<String> analyzeVuln(CodexRequest request) {
        return vulnAnalysis.execute(request);
    }

    @Override
    public CodexResponse<List<CodexVerdict>> batchFilter(CodexRequest request) {
        CodexResponse<String> raw = fpFilter.execute(request);
        if (!raw.isSuccess() || raw.getData() == null) {
            return CodexResponse.failure(
                raw.getErrorMessage() != null ? raw.getErrorMessage() : "no data",
                raw.getFallbackLevel());
        }
        return CodexResponse.success(
            List.of(parseVerdict(request.getVulnId(), raw.getData())),
            raw.getDuration(), raw.getModelVersion());
    }

    private CodexVerdict parseVerdict(String vulnId, String analysis) {
        if (analysis == null) {
            return new CodexVerdict(vulnId, "suspicious", 0.0, "empty response");
        }
        String lower = analysis.toLowerCase();
        String verdict;
        if (lower.contains("false positive") || lower.contains("false_positive")
            || lower.contains("\"is_true_positive\": false")) {
            verdict = "false_positive";
        } else if (lower.contains("\"is_true_positive\": true")
            || lower.contains("true positive")) {
            verdict = "exploitable";
        } else {
            verdict = "suspicious";
        }
        return new CodexVerdict(vulnId, verdict, extractConfidence(analysis), analysis);
    }

    private double extractConfidence(String analysis) {
        java.util.regex.Matcher m = java.util.regex.Pattern
            .compile("(?:confidence\"?\\s*[:=]\\s*)([0-9]*\\.?[0-9]+)")
            .matcher(analysis);
        if (m.find()) {
            try { return Double.parseDouble(m.group(1)); }
            catch (NumberFormatException ignored) {}
        }
        return 0.5;
    }

    @Override
    public CodexResponse<PocResult> generatePoc(CodexRequest request) {
        return pocGeneration.execute(request);
    }

    @Override
    public CodexResponse<PatchResult> generatePatch(CodexRequest request) {
        return patchGeneration.execute(request);
    }

    @Override
    public CodexResponse<List<LogicVulnResult>> mineLogicVulns(CodexRequest request) {
        return logicVulnMining.execute(request);
    }

    @Override
    public CodexHealth health() {
        CodexHealth codeHealth = codeModelClient.health();
        CodexHealth llmHealth = llmModelClient.health();
        boolean overallOk = codeHealth.isOk() || llmHealth.isOk();
        return new CodexHealth(overallOk, "codex-integration", "1.0",
            codeHealth.getLatencyMs() + llmHealth.getLatencyMs());
    }
}
