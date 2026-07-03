package com.codesec.codex.pipeline;

import com.codesec.codex.CodexAdapter;
import com.codesec.codex.capability.*;
import com.codesec.codex.client.CodexClient;
import com.codesec.codex.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

public class CodexAdapterImpl implements CodexAdapter {
    private static final Logger log = LoggerFactory.getLogger(CodexAdapterImpl.class);

    private final VulnAnalysisCapability vulnAnalysis;
    private final FalsePositiveFilterCapability fpFilter;
    private final LogicVulnMiningCapability logicVulnMining;
    private final PocGenerationCapability pocGeneration;
    private final PatchGenerationCapability patchGeneration;
    private final AnalysisPipeline pipeline;
    private final CodexClient codeModelClient;
    private final CodexClient llmModelClient;

    public CodexAdapterImpl(VulnAnalysisCapability vulnAnalysis,
                            FalsePositiveFilterCapability fpFilter,
                            LogicVulnMiningCapability logicVulnMining,
                            PocGenerationCapability pocGeneration,
                            PatchGenerationCapability patchGeneration,
                            AnalysisPipeline pipeline,
                            CodexClient codeModelClient,
                            CodexClient llmModelClient) {
        this.vulnAnalysis = vulnAnalysis;
        this.fpFilter = fpFilter;
        this.logicVulnMining = logicVulnMining;
        this.pocGeneration = pocGeneration;
        this.patchGeneration = patchGeneration;
        this.pipeline = pipeline;
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
            return CodexResponse.failure(raw.getErrorMessage(), raw.getFallbackLevel());
        }
        return CodexResponse.success(Collections.emptyList(), raw.getDuration(), raw.getModelVersion());
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
