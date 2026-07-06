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

public class VulnAnalysisBenchmark {
    private static final Logger log = LoggerFactory.getLogger(VulnAnalysisBenchmark.class);
    private static final String[] LABELS = {"SQL Injection", "XSS", "Path Traversal", "Command Injection", "Safe"};

    private final CodexClient codeModelClient;
    private final PromptRepository promptRepo;
    private final CodexProperties props;
    private final List<VulnAnalysisSample> samples;

    public VulnAnalysisBenchmark(CodexClient codeModelClient, PromptRepository promptRepo,
                                 CodexProperties props, List<VulnAnalysisSample> samples) {
        this.codeModelClient = codeModelClient;
        this.promptRepo = promptRepo;
        this.props = props;
        this.samples = samples;
    }

    public BenchmarkResult run() {
        long start = System.currentTimeMillis();
        int n = LABELS.length;
        int[][] matrix = new int[n][n];
        int total = 0;

        for (VulnAnalysisSample sample : samples) {
            total++;
            int expected = labelIndex(sample);
            int actual = classify(sample);
            matrix[expected][actual]++;
        }

        long duration = System.currentTimeMillis() - start;
        double precision = calcPrecision(matrix, n);
        double recall = calcRecall(matrix, n);
        double f1 = calcF1(precision, recall);

        BenchmarkResult result = new BenchmarkResult("VulnAnalysis", precision, recall, f1, matrix, total, duration);
        log.info("VulnAnalysis benchmark: precision={}, recall={}, f1={}, samples={}", precision, recall, f1, total);
        return result;
    }

    private int classify(VulnAnalysisSample sample) {
        try {
            CodexRequest req = new CodexRequest("benchmark", sample.getId(), sample.getLanguage(),
                sample.getCode(), "benchmark/" + sample.getId() + "." + sample.getLanguage(), 1, 10);
            CodexContext ctx = buildContext();
            PromptTemplate template = promptRepo.findByCapability(
                com.codesec.codex.model.CodexCapability.VULN_ANALYSIS, sample.getLanguage());
            String response = codeModelClient.execute(ctx, template.getSystemPrompt(), template.getUserPromptTemplate());
            return labelFromResponse(response);
        } catch (Exception e) {
            log.warn("VulnAnalysis classify error for {}: {}", sample.getId(), e.getMessage());
            return LABELS.length - 1;
        }
    }

    private int labelIndex(VulnAnalysisSample sample) {
        if (sample.isSafe()) return LABELS.length - 1;
        String vuln = sample.getExpectedVuln();
        if (vuln == null) return LABELS.length - 1;
        for (int i = 0; i < LABELS.length; i++) {
            if (LABELS[i].equalsIgnoreCase(vuln)) return i;
        }
        return LABELS.length - 1;
    }

    private int labelFromResponse(String response) {
        if (response == null) return LABELS.length - 1;
        String lower = response.toLowerCase();
        if (lower.contains("sql") || lower.contains("cwe-89")) return 0;
        if (lower.contains("xss") || lower.contains("cross-site") || lower.contains("cwe-79")) return 1;
        if (lower.contains("path") || lower.contains("traversal") || lower.contains("cwe-22")) return 2;
        if (lower.contains("command") || lower.contains("cmd") || lower.contains("cwe-78")) return 3;
        if (lower.contains("no vuln") || lower.contains("safe") || lower.contains("not exploitable")) return 4;
        return 4;
    }

    private double calcPrecision(int[][] matrix, int n) {
        double sum = 0;
        int count = 0;
        for (int i = 0; i < n; i++) {
            int tp = matrix[i][i];
            int fp = 0;
            for (int j = 0; j < n; j++) if (j != i) fp += matrix[j][i];
            double p = tp + fp > 0 ? (double) tp / (tp + fp) : 0;
            sum += p;
            count++;
        }
        return count > 0 ? sum / count : 0;
    }

    private double calcRecall(int[][] matrix, int n) {
        double sum = 0;
        int count = 0;
        for (int i = 0; i < n; i++) {
            int tp = matrix[i][i];
            int fn = 0;
            for (int j = 0; j < n; j++) if (j != i) fn += matrix[i][j];
            double r = tp + fn > 0 ? (double) tp / (tp + fn) : 0;
            sum += r;
            count++;
        }
        return count > 0 ? sum / count : 0;
    }

    private double calcF1(double precision, double recall) {
        return precision + recall > 0 ? 2 * precision * recall / (precision + recall) : 0;
    }

    private CodexContext buildContext() {
        CodexProperties.ApiModelConfig cfg = props.getCodeModel();
        CodexContext ctx = new CodexContext();
        ctx.setModel(cfg.getModel());
        ctx.setTimeoutSeconds(cfg.getTimeoutSeconds());
        return ctx;
    }
}
