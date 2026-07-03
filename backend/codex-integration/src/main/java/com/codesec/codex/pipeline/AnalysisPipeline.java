package com.codesec.codex.pipeline;

import com.codesec.codex.capability.*;
import com.codesec.codex.model.CodexHealth;
import com.codesec.codex.model.CodexRequest;
import com.codesec.codex.model.CodexResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AnalysisPipeline {
    private static final Logger log = LoggerFactory.getLogger(AnalysisPipeline.class);

    private final VulnAnalysisCapability vulnAnalysis;
    private final FalsePositiveFilterCapability fpFilter;
    private final LogicVulnMiningCapability logicVulnMining;
    private final PocGenerationCapability pocGeneration;
    private final PatchGenerationCapability patchGeneration;
    private final FallbackStrategy fallbackStrategy;

    public AnalysisPipeline(VulnAnalysisCapability vulnAnalysis,
                            FalsePositiveFilterCapability fpFilter,
                            LogicVulnMiningCapability logicVulnMining,
                            PocGenerationCapability pocGeneration,
                            PatchGenerationCapability patchGeneration,
                            FallbackStrategy fallbackStrategy) {
        this.vulnAnalysis = vulnAnalysis;
        this.fpFilter = fpFilter;
        this.logicVulnMining = logicVulnMining;
        this.pocGeneration = pocGeneration;
        this.patchGeneration = patchGeneration;
        this.fallbackStrategy = fallbackStrategy;
    }

    public AnalysisPath determinePath(CodexHealth codeModelHealth, CodexHealth llmHealth) {
        return fallbackStrategy.resolvePath(codeModelHealth, llmHealth);
    }

    public PipelineResult execute(CodexRequest request,
                                  CodexHealth codeModelHealth, CodexHealth llmHealth) {
        AnalysisPath path = determinePath(codeModelHealth, llmHealth);
        log.info("Analysis pipeline executing with path: {}", path);

        List<PipelineStage> stages = new ArrayList<>();
        List<CodexResponse<?>> results = new ArrayList<>();

        executeStage(stages, results, "false_positive_filter", () -> {
            if (!fpFilter.isEnabled()) return null;
            return fpFilter.execute(request);
        });

        executeStage(stages, results, "vuln_analysis", () -> {
            if (!vulnAnalysis.isEnabled()) return null;
            return vulnAnalysis.execute(request);
        });

        executeStage(stages, results, "logic_vuln_mining", () -> {
            if (!logicVulnMining.isEnabled()) return null;
            return logicVulnMining.execute(request);
        });

        if (path != AnalysisPath.LLM_ONLY && path != AnalysisPath.SAST_ONLY) {
            executeStage(stages, results, "poc_generation", () -> {
                if (!pocGeneration.isEnabled()) return null;
                return pocGeneration.execute(request);
            });

            executeStage(stages, results, "patch_generation", () -> {
                if (!patchGeneration.isEnabled()) return null;
                return patchGeneration.execute(request);
            });
        }

        return new PipelineResult(stages, results, path);
    }

    private void executeStage(List<PipelineStage> stages, List<CodexResponse<?>> results,
                              String name, java.util.function.Supplier<CodexResponse<?>> supplier) {
        try {
            CodexResponse<?> result = supplier.get();
            stages.add(new PipelineStage(name, result != null ? result.isSuccess() : false));
            if (result != null) results.add(result);
        } catch (Exception e) {
            log.error("Pipeline stage '{}' failed: {}", name, e.getMessage());
            stages.add(new PipelineStage(name, false));
        }
    }

    public record PipelineStage(String name, boolean success) {}

    public record PipelineResult(List<PipelineStage> stages, List<CodexResponse<?>> results, AnalysisPath path) {
        public boolean allSucceeded() {
            return stages.stream().allMatch(PipelineStage::success);
        }
    }
}
