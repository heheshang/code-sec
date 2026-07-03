package com.codesec.codex.capability;

import com.codesec.codex.client.CodexClient;
import com.codesec.codex.config.CodexProperties;
import com.codesec.codex.model.*;
import com.codesec.codex.prompt.PromptRepository;
import com.codesec.codex.prompt.PromptTemplate;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class LogicVulnMiningCapability implements Capability<List<LogicVulnResult>> {
    private static final Logger log = LoggerFactory.getLogger(LogicVulnMiningCapability.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final CodexClient codeModelClient;
    private final PromptRepository promptRepo;
    private final CodexProperties props;

    public LogicVulnMiningCapability(CodexClient codeModelClient,
                                     PromptRepository promptRepo, CodexProperties props) {
        this.codeModelClient = codeModelClient;
        this.promptRepo = promptRepo;
        this.props = props;
    }

    @Override
    public CodexCapability getType() { return CodexCapability.LOGIC_VULN_MINING; }

    @Override
    public boolean isEnabled() { return props.getCapabilities().isLogicVulnMining(); }

    @Override
    public CodexResponse<List<LogicVulnResult>> execute(CodexRequest request) {
        if (!isEnabled()) {
            return CodexResponse.failure("Logic vuln mining is disabled", null);
        }
        long start = System.currentTimeMillis();
        try {
            PromptTemplate template = promptRepo.findByCapability(CodexCapability.LOGIC_VULN_MINING);
            CodexContext ctx = buildContext(request);
            String userPrompt = fillPrompt(template.getUserPromptTemplate(), request);
            String raw = codeModelClient.execute(ctx, template.getSystemPrompt(), userPrompt);
            List<LogicVulnResult> results = parseResults(raw, request);
            Duration duration = Duration.ofMillis(System.currentTimeMillis() - start);
            return CodexResponse.success(results, duration, ctx.getModel());
        } catch (Exception e) {
            log.error("Logic vuln mining failed: {}", e.getMessage(), e);
            Duration duration = Duration.ofMillis(System.currentTimeMillis() - start);
            return CodexResponse.failure(e.getMessage(), null);
        }
    }

    private String fillPrompt(String template, CodexRequest req) {
        String sastFindings = "";
        if (req.getExtra() != null && req.getExtra().containsKey("sastFindings")) {
            sastFindings = req.getExtra().get("sastFindings");
        }
        return template
            .replace("{file_path}", req.getFilePath() != null ? req.getFilePath() : "unknown")
            .replace("{line_start}", String.valueOf(req.getLineStart()))
            .replace("{line_end}", String.valueOf(req.getLineEnd()))
            .replace("{language}", req.getLanguage() != null ? req.getLanguage() : "unknown")
            .replace("{code_snippet}", req.getCodeSnippet() != null ? req.getCodeSnippet() : "")
            .replace("{call_chain}", req.getCallChain() != null ? req.getCallChain() : "N/A")
            .replace("{data_source}", req.getDataSource() != null ? req.getDataSource() : "N/A")
            .replace("{reachable}", String.valueOf(req.isReachable()))
            .replace("{framework_protection}", req.getExtra() != null
                && req.getExtra().containsKey("frameworkProtection")
                ? req.getExtra().get("frameworkProtection") : "N/A")
            .replace("{sast_findings}", sastFindings.isEmpty() ? "无" : sastFindings);
    }

    private List<LogicVulnResult> parseResults(String raw, CodexRequest request) {
        try {
            if (raw == null || raw.trim().isEmpty()) {
                return Collections.emptyList();
            }
            String json = extractJson(raw);
            List<Map<String, Object>> rawList = MAPPER.readValue(json,
                new TypeReference<List<Map<String, Object>>>() {});
            return rawList.stream().map(m -> mapToResult(m, request)).toList();
        } catch (Exception e) {
            log.warn("Failed to parse logic vuln results from LLM output, returning empty: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String extractJson(String raw) {
        int start = raw.indexOf('[');
        int end = raw.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return raw;
    }

    @SuppressWarnings("unchecked")
    private LogicVulnResult mapToResult(Map<String, Object> m, CodexRequest req) {
        LogicVulnResult r = new LogicVulnResult();
        r.setVulnType(asString(m.get("vuln_type")));
        Object evidence = m.get("evidence_chain");
        if (evidence instanceof List) {
            r.setEvidenceChain((List<String>) evidence);
        }
        r.setExploitCondition(asString(m.get("exploit_condition")));
        r.setRiskLevel(asString(m.get("risk_level")));
        r.setRecommendedFix(asString(m.get("recommended_fix")));
        r.setCodeSnippet(asString(m.get("code_snippet"), req.getCodeSnippet()));
        r.setLineStart(asInt(m.get("line_start"), req.getLineStart()));
        r.setLineEnd(asInt(m.get("line_end"), req.getLineEnd()));
        return r;
    }

    private String asString(Object val) {
        return asString(val, null);
    }

    private String asString(Object val, String fallback) {
        return val != null ? val.toString() : fallback;
    }

    private int asInt(Object val, int fallback) {
        if (val instanceof Integer) return (Integer) val;
        if (val instanceof Double) return ((Double) val).intValue();
        if (val instanceof String) {
            try { return Integer.parseInt((String) val); } catch (NumberFormatException e) { return fallback; }
        }
        return fallback;
    }

    private CodexContext buildContext(CodexRequest request) {
        CodexProperties.ApiModelConfig cfg = props.getApi().getCodeModel();
        CodexContext ctx = new CodexContext();
        ctx.setApiKey(cfg.getApiKey());
        ctx.setEndpoint(cfg.getEndpoint());
        ctx.setModel(cfg.getModel());
        ctx.setTimeoutSeconds(cfg.getTimeoutSeconds());
        return ctx;
    }
}
