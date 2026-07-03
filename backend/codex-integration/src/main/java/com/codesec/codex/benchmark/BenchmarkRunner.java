package com.codesec.codex.benchmark;

import com.codesec.codex.client.CodexClient;
import com.codesec.codex.config.CodexProperties;
import com.codesec.codex.prompt.PromptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BenchmarkRunner {
    private static final Logger log = LoggerFactory.getLogger(BenchmarkRunner.class);

    private final CodexClient codeModelClient;
    private final CodexClient llmModelClient;
    private final PromptRepository promptRepo;
    private final CodexProperties props;

    public BenchmarkRunner(CodexClient codeModelClient, CodexClient llmModelClient,
                           PromptRepository promptRepo, CodexProperties props) {
        this.codeModelClient = codeModelClient;
        this.llmModelClient = llmModelClient;
        this.promptRepo = promptRepo;
        this.props = props;
    }

    public BenchmarkSummary runAll() {
        long start = System.currentTimeMillis();
        List<BenchmarkResult> results = new ArrayList<>();

        results.add(runVulnAnalysis());
        results.add(runFalsePositive());

        long totalDuration = System.currentTimeMillis() - start;
        String lastRunAt = java.time.Instant.now().toString();
        return new BenchmarkSummary(results, totalDuration, lastRunAt);
    }

    public BenchmarkResult runVulnAnalysis() {
        log.info("Starting VulnAnalysis benchmark...");
        List<VulnAnalysisSample> samples = loadSamples("benchmark/vuln-analysis-samples.yaml",
            VulnAnalysisSample.class);
        VulnAnalysisBenchmark benchmark = new VulnAnalysisBenchmark(
            codeModelClient, promptRepo, props, samples);
        return benchmark.run();
    }

    public BenchmarkResult runFalsePositive() {
        log.info("Starting FalsePositive benchmark...");
        List<FalsePositiveSample> samples = loadSamples("benchmark/false-positive-samples.yaml",
            FalsePositiveSample.class);
        FalsePositiveBenchmark benchmark = new FalsePositiveBenchmark(
            llmModelClient, promptRepo, props, samples);
        return benchmark.run();
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> loadSamples(String classpath, Class<T> clazz) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(classpath)) {
            if (is == null) {
                log.warn("Sample file not found: {}", classpath);
                return List.of();
            }
            Yaml yaml = new Yaml(new LoaderOptions());
            Iterable<Object> iterable = yaml.loadAll(is);
            List<T> result = new ArrayList<>();
            for (Object obj : iterable) {
                T converted = convert(obj, clazz);
                if (converted != null) result.add(converted);
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to load samples from {}: {}", classpath, e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T convert(Object obj, Class<T> clazz) {
        if (clazz.isInstance(obj)) return clazz.cast(obj);
        if (obj instanceof Map) {
            try {
                T instance = clazz.getDeclaredConstructor().newInstance();
                Map<String, Object> map = (Map<String, Object>) obj;
                for (var entry : map.entrySet()) {
                    String key = entry.getKey();
                    Object val = entry.getValue();
                    String setter = "set" + Character.toUpperCase(key.charAt(0)) + key.substring(1);
                    try {
                        var method = clazz.getMethod(setter, val != null ? val.getClass() : String.class);
                        method.invoke(instance, val);
                    } catch (NoSuchMethodException e) {
                        for (var m : clazz.getMethods()) {
                            if (m.getName().equals(setter) && m.getParameterCount() == 1) {
                                m.invoke(instance, convertValue(val, m.getParameterTypes()[0]));
                                break;
                            }
                        }
                    }
                }
                return instance;
            } catch (Exception e) {
                log.warn("Failed to convert map to {}: {}", clazz.getSimpleName(), e.getMessage());
                return null;
            }
        }
        return null;
    }

    private Object convertValue(Object val, Class<?> targetType) {
        if (val == null) return null;
        if (targetType == boolean.class || targetType == Boolean.class) {
            if (val instanceof Boolean) return val;
            return Boolean.valueOf(val.toString());
        }
        if (targetType == int.class || targetType == Integer.class) {
            if (val instanceof Integer) return val;
            if (val instanceof Double) return ((Double) val).intValue();
            return Integer.parseInt(val.toString());
        }
        if (targetType == double.class || targetType == Double.class) {
            if (val instanceof Double) return val;
            if (val instanceof Integer) return ((Integer) val).doubleValue();
            return Double.parseDouble(val.toString());
        }
        return val.toString();
    }
}
