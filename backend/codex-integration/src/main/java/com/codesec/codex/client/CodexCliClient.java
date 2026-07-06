package com.codesec.codex.client;

import com.codesec.codex.model.ClientType;
import com.codesec.codex.model.CodexContext;
import com.codesec.codex.model.CodexHealth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Delegates analysis to the local {@code codex} CLI by spawning
 * {@code codex exec --ephemeral --skip-git-repo-check} as a subprocess.
 * The combined system + user prompt is fed via stdin; the agent's final
 * message is captured through {@code --output-last-message}. stdout and
 * stderr are redirected to temp files so the OS pipe buffer can never fill
 * up and stall the subprocess while {@code waitFor} sleeps.
 * <p>
 * This client goes through the Codex CLI layer so the agent sandbox, tool
 * access, and auth are managed by Codex itself.
 */
public class CodexCliClient implements CodexClient {
    private static final Logger log = LoggerFactory.getLogger(CodexCliClient.class);

    private static final String BINARY = "codex";

    private final String modelLabel;
    private final int maxTokens;
    private final double temperature;
    private final String workspaceDir;
    private volatile boolean available = true;

    public CodexCliClient(String modelLabel, String workspaceDir) {
        this(modelLabel, workspaceDir, 4096, 0.1);
    }

    public CodexCliClient(String modelLabel, String workspaceDir, int maxTokens, double temperature) {
        this.modelLabel = modelLabel;
        this.workspaceDir = workspaceDir;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
    }

    @Override
    public String execute(CodexContext context, String systemPrompt, String userPrompt) {
        Path outputMessageFile = null;
        Path stdoutFile = null;
        Path stderrFile = null;
        try {
            outputMessageFile = Files.createTempFile("codex-out-", ".txt");
            stdoutFile = Files.createTempFile("codex-stdout-", ".log");
            stderrFile = Files.createTempFile("codex-stderr-", ".log");
            ProcessBuilder pb = new ProcessBuilder(
                BINARY, "exec",
                "--skip-git-repo-check",
                "--ephemeral",
                "-o", outputMessageFile.toString()
            );
            if (context.getModel() != null && !context.getModel().isBlank()) {
                pb.command().add("-m");
                pb.command().add(context.getModel());
            }
            if (workspaceDir != null && !workspaceDir.isBlank()) {
                pb.command().add("-C");
                pb.command().add(workspaceDir);
            }
            // Redirect stdout/stderr to files so the pipe buffer never fills
            // up and stalls the subprocess while waitFor sleeps.
            pb.redirectOutput(stdoutFile.toFile());
            pb.redirectError(stderrFile.toFile());

            Process proc = pb.start();

            // Write the combined prompt to stdin, then close it.
            String prompt = buildPrompt(systemPrompt, userPrompt);
            try (OutputStream os = proc.getOutputStream()) {
                os.write(prompt.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int timeoutSeconds = context.getTimeoutSeconds() > 0
                ? context.getTimeoutSeconds() : 120;
            boolean finished = proc.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                proc.destroyForcibly();
                throw new TimeoutException("codex exec timed out after " + timeoutSeconds + "s");
            }
            int exit = proc.exitValue();
            String stdout = readTrimmed(stdoutFile);
            String stderr = readTrimmed(stderrFile);
            if (exit != 0) {
                String msg = stderr != null && !stderr.isBlank() ? stderr : stdout;
                throw new RuntimeException("codex exec exited " + exit + ": " + trim(msg, 800));
            }

            String result = Files.readString(outputMessageFile, StandardCharsets.UTF_8).trim();
            if (result.isEmpty()) {
                throw new RuntimeException("codex exec produced an empty response (stderr: "
                    + trim(stderr, 300) + ")");
            }
            available = true;
            return result;
        } catch (TimeoutException e) {
            available = false;
            throw new RuntimeException(e.getMessage(), e);
        } catch (IOException e) {
            available = false;
            throw new RuntimeException("Failed to run codex CLI: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("codex exec interrupted", e);
        } finally {
            for (Path p : new Path[]{outputMessageFile, stdoutFile, stderrFile}) {
                if (p != null) { try { Files.deleteIfExists(p); } catch (IOException ignored) {} }
            }
        }
    }

    private String readTrimmed(Path file) {
        if (file == null) return null;
        try {
            String s = Files.readString(file, StandardCharsets.UTF_8);
            return s.isBlank() ? null : s.trim();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public CodexHealth health() {
        long start = System.currentTimeMillis();
        // Cheap check: can we locate and run the binary?
        boolean ok;
        try {
            Process proc = new ProcessBuilder(BINARY, "--version").redirectErrorStream(true).start();
            ok = proc.waitFor(10, TimeUnit.SECONDS) && proc.exitValue() == 0;
        } catch (Exception e) {
            ok = false;
        }
        if (!ok) available = false;
        long latency = System.currentTimeMillis() - start;
        return new CodexHealth(ok, modelLabel, "cli", latency);
    }

    @Override
    public ClientType type() {
        return ClientType.CLI;
    }

    private String buildPrompt(String systemPrompt, String userPrompt) {
        StringBuilder sb = new StringBuilder();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            sb.append(systemPrompt).append("\n\n---\n\n");
        }
        sb.append(userPrompt != null ? userPrompt : "");
        return sb.toString();
    }

    private String trim(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
