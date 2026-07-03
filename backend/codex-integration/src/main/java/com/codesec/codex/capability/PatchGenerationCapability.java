package com.codesec.codex.capability;

import com.codesec.codex.client.AstCompiler;
import com.codesec.codex.client.CodexClient;
import com.codesec.codex.config.CodexProperties;
import com.codesec.codex.model.*;
import com.codesec.codex.prompt.PromptRepository;
import com.codesec.codex.prompt.PromptTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class PatchGenerationCapability implements Capability<PatchResult> {
    private static final Logger log = LoggerFactory.getLogger(PatchGenerationCapability.class);

    private final CodexClient codeModelClient;
    private final PromptRepository promptRepo;
    private final CodexProperties props;
    private final AstCompiler astCompiler;

    public PatchGenerationCapability(CodexClient codeModelClient,
                                     PromptRepository promptRepo, CodexProperties props,
                                     AstCompiler astCompiler) {
        this.codeModelClient = codeModelClient;
        this.promptRepo = promptRepo;
        this.props = props;
        this.astCompiler = astCompiler;
    }

    @Override
    public CodexCapability getType() { return CodexCapability.PATCH_GENERATION; }

    @Override
    public boolean isEnabled() { return props.getCapabilities().isPatchGeneration(); }

    @Override
    public CodexResponse<PatchResult> execute(CodexRequest request) {
        if (!isEnabled()) {
            return CodexResponse.failure("Patch generation is disabled", null);
        }
        long start = System.currentTimeMillis();
        try {
            PromptTemplate template = promptRepo.findByCapability(CodexCapability.PATCH_GENERATION);
            CodexContext ctx = buildContext(request);
            String rawPatch = codeModelClient.execute(ctx, template.getSystemPrompt(), template.getUserPromptTemplate());
            String language = request.getLanguage() != null ? request.getLanguage() : "java";
            PatchResult result = astCompiler.validate(rawPatch, language);
            result.setPatchCode(rawPatch);
            result.setLanguage(language);
            result.setFilePath(request.getFilePath());
            result.setLineStart(request.getLineStart());
            result.setLineEnd(request.getLineEnd());
            Duration duration = Duration.ofMillis(System.currentTimeMillis() - start);
            return CodexResponse.success(result, duration, ctx.getModel());
        } catch (Exception e) {
            log.error("Patch generation failed: {}", e.getMessage(), e);
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
        ctx.setTimeoutSeconds(cfg.getTimeoutSeconds());
        return ctx;
    }
}
