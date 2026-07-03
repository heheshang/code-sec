package com.codesec.codex.config;

import com.codesec.codex.CodexAdapter;
import com.codesec.codex.benchmark.BenchmarkController;
import com.codesec.codex.benchmark.BenchmarkService;
import com.codesec.codex.capability.*;
import com.codesec.codex.client.AstCompiler;
import com.codesec.codex.client.CodexClient;
import com.codesec.codex.client.SandboxVerifier;
import com.codesec.codex.client.SimulatedSandboxVerifier;
import com.codesec.codex.pipeline.*;
import com.codesec.codex.prompt.PromptLoader;
import com.codesec.codex.prompt.PromptRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

import java.util.List;

@Configuration
@EnableConfigurationProperties(CodexProperties.class)
@ComponentScan(basePackages = "com.codesec.codex")
public class CodexAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public PromptLoader promptLoader() {
        return new PromptLoader();
    }

    @Bean
    @ConditionalOnMissingBean
    public PromptRepository promptRepository(PromptLoader promptLoader) {
        return new PromptRepository(promptLoader);
    }

    @Bean
    @ConditionalOnMissingBean
    public AstCompiler astCompiler() {
        return new AstCompiler();
    }

    @Bean
    @ConditionalOnMissingBean
    public SandboxVerifier sandboxVerifier() {
        return new SimulatedSandboxVerifier();
    }

    @Bean
    @ConditionalOnMissingBean
    public VulnAnalysisCapability vulnAnalysisCapability(
            CodexClient codeModelClient, CodexClient llmModelClient,
            PromptRepository promptRepo, CodexProperties props) {
        return new VulnAnalysisCapability(codeModelClient, llmModelClient, promptRepo, props);
    }

    @Bean
    @ConditionalOnMissingBean
    public FalsePositiveFilterCapability falsePositiveFilterCapability(
            CodexClient codeModelClient, CodexClient llmModelClient,
            PromptRepository promptRepo, CodexProperties props) {
        return new FalsePositiveFilterCapability(codeModelClient, llmModelClient, promptRepo, props);
    }

    @Bean
    @ConditionalOnMissingBean
    public PocGenerationCapability pocGenerationCapability(
            CodexClient codeModelClient, PromptRepository promptRepo,
            CodexProperties props, SandboxVerifier sandboxVerifier) {
        return new PocGenerationCapability(codeModelClient, promptRepo, props, sandboxVerifier);
    }

    @Bean
    @ConditionalOnMissingBean
    public PatchGenerationCapability patchGenerationCapability(
            CodexClient codeModelClient, PromptRepository promptRepo,
            CodexProperties props, AstCompiler astCompiler) {
        return new PatchGenerationCapability(codeModelClient, promptRepo, props, astCompiler);
    }

    @Bean
    @ConditionalOnMissingBean
    public LogicVulnMiningCapability logicVulnMiningCapability(
            CodexClient codeModelClient, PromptRepository promptRepo,
            CodexProperties props) {
        return new LogicVulnMiningCapability(codeModelClient, promptRepo, props);
    }

    @Bean
    @ConditionalOnMissingBean
    public FallbackStrategy fallbackStrategy() {
        return new FallbackStrategy();
    }

    @Bean
    @ConditionalOnMissingBean
    public AnalysisPipeline analysisPipeline(
            VulnAnalysisCapability vulnAnalysis,
            FalsePositiveFilterCapability fpFilter,
            LogicVulnMiningCapability logicVulnMining,
            PocGenerationCapability pocGeneration,
            PatchGenerationCapability patchGeneration,
            FallbackStrategy fallbackStrategy) {
        return new AnalysisPipeline(vulnAnalysis, fpFilter, logicVulnMining,
            pocGeneration, patchGeneration, fallbackStrategy);
    }

    @Bean
    @ConditionalOnMissingBean
    public CodexAdapter codexAdapter(
            VulnAnalysisCapability vulnAnalysis,
            FalsePositiveFilterCapability fpFilter,
            LogicVulnMiningCapability logicVulnMining,
            PocGenerationCapability pocGeneration,
            PatchGenerationCapability patchGeneration,
            AnalysisPipeline pipeline,
            CodexClient codeModelClient,
            CodexClient llmModelClient) {
        return new CodexAdapterImpl(vulnAnalysis, fpFilter, logicVulnMining,
            pocGeneration, patchGeneration,
            pipeline, codeModelClient, llmModelClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public CodexHealthIndicator codexHealthIndicator(List<CodexClient> clients) {
        return new CodexHealthIndicator(clients);
    }
}
