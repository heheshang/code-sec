package com.codesec.codex.capability;

import com.codesec.codex.client.CodexClient;
import com.codesec.codex.config.CodexProperties;
import com.codesec.codex.model.CodexCapability;
import com.codesec.codex.model.CodexContext;
import com.codesec.codex.model.CodexRequest;
import com.codesec.codex.model.CodexResponse;
import com.codesec.codex.prompt.PromptRepository;
import com.codesec.codex.prompt.PromptTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class VulnAnalysisCapability implements Capability<String> {
    private static final Logger log = LoggerFactory.getLogger(VulnAnalysisCapability.class);

    private final CodexClient codeModelClient;
    private final CodexClient llmModelClient;
    private final PromptRepository promptRepo;
    private final CodexProperties props;

    public VulnAnalysisCapability(CodexClient codeModelClient, CodexClient llmModelClient,
                                  PromptRepository promptRepo, CodexProperties props) {
        this.codeModelClient = codeModelClient;
        this.llmModelClient = llmModelClient;
        this.promptRepo = promptRepo;
        this.props = props;
    }

    @Override
    public CodexCapability getType() { return CodexCapability.VULN_ANALYSIS; }

    @Override
    public boolean isEnabled() { return props.getCapabilities().isVulnAnalysis(); }

    @Override
    public CodexResponse<String> execute(CodexRequest request) {
        long start = System.currentTimeMillis();
        try {
            PromptTemplate template = promptRepo.findByCapability(CodexCapability.VULN_ANALYSIS, request.getLanguage());
            CodexContext ctx = buildContext(request);
            CodexClient client = codeModelClient.isAvailable() ? codeModelClient : llmModelClient;
            String response = client.execute(ctx, template.getSystemPrompt(), template.getUserPromptTemplate());
            Duration duration = Duration.ofMillis(System.currentTimeMillis() - start);
            return CodexResponse.success(response, duration, ctx.getModel());
        } catch (Exception e) {
            log.error("Vuln analysis failed: {}", e.getMessage(), e);
            Duration duration = Duration.ofMillis(System.currentTimeMillis() - start);
            return CodexResponse.failure(e.getMessage(), null);
        }
    }

    private CodexContext buildContext(CodexRequest request) {
        CodexProperties.ApiModelConfig cfg = props.getApi().getCodeModel();
        CodexContext ctx = new CodexContext();
        ctx.setApiKey(cfg.getApiKey());
        ctx.setEndpoint(cfg.getEndpoint());
        ctx.setModel(cfg.getModel());
        ctx.setTimeoutSeconds(request.getTimeout() != null
            ? (int) request.getTimeout().getSeconds() : cfg.getTimeoutSeconds());
        return ctx;
    }
}
