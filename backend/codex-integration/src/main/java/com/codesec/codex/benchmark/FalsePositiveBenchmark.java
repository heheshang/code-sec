package com.codesec.codex.benchmark;

import com.codesec.codex.client.CodexClient;
import com.codesec.codex.config.CodexProperties;
import com.codesec.codex.model.CodexContext;
import com.codesec.codex.model.CodexRequest;
import com.codesec.codex.prompt.PromptRepository;
import com.codesec.codex.prompt.PromptTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class FalsePositiveBenchmark {
    private static final Logger log = LoggerFactory.getLogger(FalsePositiveBenchmark.class);
    private static final String[] LABELS = {"True Positive", "False Positive"};

    private final CodexClient llmModelClient;
    private final PromptRepository promptRepo;
    private final CodexProperties props;
    private final List<FalsePositiveSample> samples;

    public FalsePositiveBenchmark(CodexClient llmModelClient, PromptRepository promptRepo,
                                  CodexProperties props, List<FalsePositiveSample> samples) {
        this.llmModelClient = llmModelClient;
        this.promptRepo = promptRepo;
        this.props = props;
        this.samples = samples;
    }

    public BenchmarkResult run() {
        long start = System.currentTimeMillis();
        int n = LABELS.length;
        int[][] matrix = new int[n][n];
        int total = 0;

        for (FalsePositiveSample sample : samples) {
            total++;
            int expected = sample.isTruePositive() ? 0 : 1;
            int actual = classify(sample);
            matrix[expected][actual]++;
        }

        long duration = System.currentTimeMillis() - start;
        double precision = calcMetric(matrix, true);
        double recall = calcMetric(matrix, false);
        double f1 = precision + recall > 0 ? 2 * precision * recall / (precision + recall) : 0;

        BenchmarkResult result = new BenchmarkResult("FalsePositiveFilter", precision, recall, f1, matrix, total, duration);
        log.info("FalsePositive benchmark: precision={}, recall={}, f1={}, samples={}", precision, recall, f1, total);
        return result;
    }

    private int classify(FalsePositiveSample sample) {
        try {
            CodexRequest req = new CodexRequest("benchmark", sample.getId(), sample.getLanguage(),
                sample.getCode(), "benchmark/" + sample.getId(), 1, 5);
            req.setCallChain(sample.getCallChain());
            req.setDataSource(sample.getDataSource());
            req.setReachable(sample.isReachable());
            req.setExtra(Map.of("frameworkProtection", sample.getFrameworkProtection(),
                "ruleId", sample.getRuleId(), "title", sample.getTitle(),
                "severity", sample.getSeverity()));

            CodexContext ctx = buildContext();
            PromptTemplate template = promptRepo.findByCapability(
                com.codesec.codex.model.CodexCapability.FALSE_POSITIVE_FILTER);
            String userPrompt = template.getUserPromptTemplate()
                .replace("{rule_id}", sample.getRuleId())
                .replace("{title}", sample.getTitle())
                .replace("{severity}", sample.getSeverity())
                .replace("{file_path}", "benchmark/" + sample.getId())
                .replace("{line_start}", "1")
                .replace("{line_end}", "5")
                .replace("{language}", sample.getLanguage())
                .replace("{code_snippet}", sample.getCode())
                .replace("{call_chain}", sample.getCallChain())
                .replace("{data_source}", sample.getDataSource())
                .replace("{reachable}", String.valueOf(sample.isReachable()))
                .replace("{framework_protection}", sample.getFrameworkProtection());
            String response = llmModelClient.execute(ctx, template.getSystemPrompt(), userPrompt);
            return labelFromResponse(response);
        } catch (Exception e) {
            log.warn("FalsePositive classify error for {}: {}", sample.getId(), e.getMessage());
            return 1;
        }
    }

    private int labelFromResponse(String response) {
        if (response == null) return 1;
        String lower = response.toLowerCase();
        if (lower.contains("true") && lower.contains("positive")) return 0;
        if (lower.contains("\"is_true_positive\": true") || lower.contains("is_true_positive: true")) return 0;
        return 1;
    }

    private double calcMetric(int[][] matrix, boolean precision) {
        int tp = matrix[0][0];
        int fp = matrix[1][0];
        int fn = matrix[0][1];
        double denom = precision ? (tp + fp) : (tp + fn);
        return denom > 0 ? tp / denom : 0;
    }

    private CodexContext buildContext() {
        CodexProperties.ApiModelConfig cfg = props.getLlmModel();
        CodexContext ctx = new CodexContext();
        ctx.setModel(cfg.getModel());
        ctx.setTimeoutSeconds(cfg.getTimeoutSeconds());
        return ctx;
    }
}
