package com.codesec.codex.capability;

import com.codesec.codex.client.CodexClient;
import com.codesec.codex.client.SandboxVerifier;
import com.codesec.codex.config.CodexProperties;
import com.codesec.codex.model.*;
import com.codesec.codex.prompt.PromptRepository;
import com.codesec.codex.prompt.PromptTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class PocGenerationCapability implements Capability<PocResult> {
    private static final Logger log = LoggerFactory.getLogger(PocGenerationCapability.class);

    private final CodexClient codeModelClient;
    private final PromptRepository promptRepo;
    private final CodexProperties props;
    private final SandboxVerifier sandboxVerifier;

    public PocGenerationCapability(CodexClient codeModelClient,
                                   PromptRepository promptRepo, CodexProperties props,
                                   SandboxVerifier sandboxVerifier) {
        this.codeModelClient = codeModelClient;
        this.promptRepo = promptRepo;
        this.props = props;
        this.sandboxVerifier = sandboxVerifier;
    }

    @Override
    public CodexCapability getType() { return CodexCapability.POC_GENERATION; }

    @Override
    public boolean isEnabled() { return props.getCapabilities().isPocGeneration(); }

    @Override
    public CodexResponse<PocResult> execute(CodexRequest request) {
        if (!isEnabled()) {
            return CodexResponse.failure("POC generation is disabled", null);
        }
        long start = System.currentTimeMillis();
        try {
            PromptTemplate template = promptRepo.findByCapability(CodexCapability.POC_GENERATION);
            CodexContext ctx = buildContext(request);
            String raw = codeModelClient.execute(ctx, template.getSystemPrompt(), template.getUserPromptTemplate());
            PocResult poc = parsePoc(raw, request);
            SandboxVerifier.SandboxResult sandboxResult = sandboxVerifier.verify(poc);
            poc.setSandboxStatus(sandboxResult.name());
            Duration duration = Duration.ofMillis(System.currentTimeMillis() - start);
            return CodexResponse.success(poc, duration, ctx.getModel());
        } catch (Exception e) {
            log.error("POC generation failed: {}", e.getMessage(), e);
            Duration duration = Duration.ofMillis(System.currentTimeMillis() - start);
            return CodexResponse.failure(e.getMessage(), null);
        }
    }

    private PocResult parsePoc(String raw, CodexRequest request) {
        PocResult poc = new PocResult();
        poc.setPocType(detectPocType(request));
        poc.setHttpMethod(detectHttpMethod(raw));
        poc.setEndpoint(extractEndpoint(raw, request));
        poc.setPayload(extractPayload(raw));
        poc.setExpectedResult(detectExpectedResult(raw));
        poc.setCodeSnippet(raw);
        return poc;
    }

    private String detectPocType(CodexRequest request) {
        if (request.getExtra() != null && request.getExtra().containsKey("vulnType")) {
            return request.getExtra().get("vulnType");
        }
        return "GENERIC";
    }

    private String detectHttpMethod(String raw) {
        if (raw.contains("GET") || raw.contains("get")) return "GET";
        if (raw.contains("POST") || raw.contains("post")) return "POST";
        if (raw.contains("PUT") || raw.contains("put")) return "PUT";
        if (raw.contains("DELETE") || raw.contains("delete")) return "DELETE";
        return "GET";
    }

    private String extractEndpoint(String raw, CodexRequest request) {
        if (raw.contains("http") || raw.contains("localhost") || raw.contains("/api/")) {
            int start = Math.max(raw.indexOf("http"), raw.indexOf("/api/"));
            if (start < 0) start = 0;
            String sub = raw.substring(start);
            int end = sub.indexOf("\n");
            if (end > 0) return sub.substring(0, end).trim();
            return sub.trim();
        }
        return request.getFilePath() != null ? request.getFilePath() : "/unknown";
    }

    private String extractPayload(String raw) {
        String[] lines = raw.split("\n");
        StringBuilder payload = new StringBuilder();
        boolean inPayload = false;
        for (String line : lines) {
            if (line.contains("payload") || line.contains("body") || line.contains("params")) {
                inPayload = true;
            }
            if (inPayload) {
                payload.append(line).append("\n");
                if (line.trim().endsWith("'") || line.trim().endsWith("\"") || line.trim().endsWith("}")) {
                    break;
                }
            }
        }
        return payload.length() > 0 ? payload.toString().trim() : raw.substring(0, Math.min(200, raw.length()));
    }

    private String detectExpectedResult(String raw) {
        if (raw.contains("200") || raw.contains("success") || raw.contains("true")) {
            return "Expected HTTP 200 / success response";
        }
        if (raw.contains("500") || raw.contains("error")) {
            return "Expected HTTP 500 / error response";
        }
        return "Expected exploit to succeed";
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
