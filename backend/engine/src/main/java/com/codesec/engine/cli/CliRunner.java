package com.codesec.engine.cli;

import com.codesec.engine.Engine;
import com.codesec.engine.model.Finding;
import com.codesec.engine.rule.RuleRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
    name = "code-sec-engine",
    description = "CodeSec Self-Research SAST Engine",
    mixinStandardHelpOptions = true,
    subcommands = {CliRunner.ScanCommand.class}
)
public final class CliRunner implements Callable<Integer> {
    private static final Logger log = LoggerFactory.getLogger(CliRunner.class);

    @Override
    public Integer call() {
        new CommandLine(this).usage(System.out);
        return 0;
    }

    @Command(name = "scan", description = "Scan a directory for security vulnerabilities")
    static final class ScanCommand implements Callable<Integer> {
        private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        @Option(
            names = {"-i", "--input"},
            description = "Input directory containing source code to scan",
            required = true
        )
        private Path inputDir;

        @Option(
            names = {"-o", "--output"},
            description = "Output JSON file for findings (default: stdout)"
        )
        private Path outputFile;

        @Option(
            names = {"-r", "--rules"},
            description = "Rules directory (default: classpath rules/java/)",
            defaultValue = ""
        )
        private String rulesDir;

        @Override
        public Integer call() {
            try {
                if (!Files.isDirectory(inputDir)) {
                    log.error("Input directory does not exist: {}", inputDir);
                    return 1;
                }

                RuleRegistry registry = new RuleRegistry();

                if (rulesDir.isEmpty()) {
                    registry.loadFromClasspath("rules/java", "rules/go", "rules/python");
                } else {
                    registry.loadFromDirectory(Path.of(rulesDir));
                }

                log.info("Loaded {} rules", registry.size());

                Engine engine = Engine.create(registry);
                List<Finding> findings = engine.scan(inputDir);

                String json = MAPPER.writeValueAsString(findings);

                if (outputFile != null) {
                    Files.writeString(outputFile, json);
                    log.info("Wrote {} findings to {}", findings.size(), outputFile);
                } else {
                    System.out.println(json);
                }

                log.info("Scan complete: {} findings", findings.size());
                return 0;

            } catch (Exception e) {
                log.error("Scan failed", e);
                return 1;
            }
        }
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new CliRunner()).execute(args);
        System.exit(exitCode);
    }
}
