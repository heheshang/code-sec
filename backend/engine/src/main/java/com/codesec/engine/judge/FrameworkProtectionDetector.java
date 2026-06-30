package com.codesec.engine.judge;

import com.codesec.engine.model.Finding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Detects framework-level security protection in the call chain between
 * HTTP entry points and a vulnerable method.
 * <p>
 * Protection rules are loaded from YAML files on the classpath
 * ({@code rules/protection/*.yml}) and define three detection strategies:
 * <ul>
 *   <li>{@link ProtectionRule.Type#ANNOTATION ANNOTATION} — method-level annotations
 *       like {@code @PreAuthorize}, {@code @Secured}</li>
 *   <li>{@link ProtectionRule.Type#CLASS_ANNOTATION CLASS_ANNOTATION} — class-level
 *       annotations (re-parses the source file with JavaParser)</li>
 *   <li>{@link ProtectionRule.Type#METHOD_CALL METHOD_CALL} — protected API calls
 *       in the call chain (e.g. MyBatis {@code selectOne}, ESAPI {@code encodeForSQL})</li>
 * </ul>
 * <p>
 * Usage:
 * <pre>{@code
 * FrameworkProtectionDetector detector = new FrameworkProtectionDetector(graph);
 * AlgorithmResult result = detector.classify(finding);
 * if (result instanceof AlgorithmResult.No no) {
 *     // finding is protected, reason in no.reason()
 * }
 * }</pre>
 */
public final class FrameworkProtectionDetector implements Algorithm {

    private static final Logger log = LoggerFactory.getLogger(FrameworkProtectionDetector.class);

    private final List<ProtectionRule> rules;
    private final ProjectCallGraph graph;
    private final Map<String, String> sourceFiles;

    /**
     * Creates a detector with default protection rules loaded from classpath YAML.
     *
     * @param graph the populated project call graph
     */
    public FrameworkProtectionDetector(ProjectCallGraph graph) {
        this.graph = graph;
        this.rules = ProtectionRuleLoader.loadDefaultRules();
        this.sourceFiles = Map.of();
    }

    /**
     * Creates a detector with a custom list of protection rules.
     *
     * @param graph the populated project call graph
     * @param rules custom protection rules (for testing)
     */
    public FrameworkProtectionDetector(ProjectCallGraph graph, List<ProtectionRule> rules) {
        this.graph = graph;
        this.rules = List.copyOf(rules);
        this.sourceFiles = Map.of();
    }

    /**
     * Creates a detector with custom rules and source file contents for
     * class-level annotation inspection.
     *
     * @param graph       the populated project call graph
     * @param rules       custom protection rules
     * @param sourceFiles map of file path to source code content
     */
    FrameworkProtectionDetector(ProjectCallGraph graph, List<ProtectionRule> rules, Map<String, String> sourceFiles) {
        this.graph = graph;
        this.rules = List.copyOf(rules);
        this.sourceFiles = Map.copyOf(sourceFiles);
    }

    @Override
    public String name() {
        return "framework-protection";
    }

    /**
     * Classifies a single {@link Finding} against all protection rules.
     * <p>
     * Returns {@link AlgorithmResult.No} with the matching rule's reason if any
     * protection rule matches. Returns {@link AlgorithmResult.Undetermined}
     * if no rule matches or the vulnerable method cannot be located.
     *
     * @param finding the SAST finding to classify
     * @return classification result
     */
    @Override
    public AlgorithmResult classify(Finding finding) {
        Optional<MethodNode> optMethod = findVulnerableMethod(finding);
        if (optMethod.isEmpty()) {
            log.debug("Vulnerable method not found in call graph for {}:{}",
                finding.filePath(), finding.lineStart());
            return AlgorithmResult.undetermined("漏洞方法未在调用图中找到");
        }

        MethodNode vulnerableMethod = optMethod.get();
        String sourceCode = sourceFiles.get(finding.filePath());

        for (ProtectionRule rule : rules) {
            if (matchesRule(vulnerableMethod, sourceCode, rule)) {
                log.debug("Protection rule matched: {} for method {}", rule.name(), vulnerableMethod.key());
                return AlgorithmResult.no(rule.reason());
            }
        }

        return AlgorithmResult.undetermined();
    }

    /**
     * Attempts to match a single protection rule against the vulnerable method.
     */
    private boolean matchesRule(MethodNode method, String sourceCode, ProtectionRule rule) {
        return switch (rule.type()) {
            case ANNOTATION -> matchesAnnotation(method, rule);
            case CLASS_ANNOTATION -> matchesClassAnnotation(sourceCode, method, rule);
            case METHOD_CALL -> matchesMethodCall(method, rule);
        };
    }

    /**
     * Checks if the vulnerable method carries a matching annotation.
     */
    private boolean matchesAnnotation(MethodNode method, ProtectionRule rule) {
        String shortName = rule.match().annotationShortName();
        if (shortName.isEmpty()) {
            return false;
        }
        String annotationToFind = "@" + shortName;
        for (String annotation : method.annotations()) {
            if (annotation.equals(annotationToFind)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the vulnerable method's containing class carries a matching annotation.
     * <p>
     * Requires source code to be available. If source code is unavailable,
     * returns {@code false} (graceful degradation).
     */
    private boolean matchesClassAnnotation(String sourceCode, MethodNode method, ProtectionRule rule) {
        if (sourceCode == null || sourceCode.isEmpty()) {
            return false;
        }
        List<String> classAnnotations = SourceAnnotationExtractor.extractClassAnnotations(sourceCode, method.startLine());
        String shortName = rule.match().annotationShortName();
        if (shortName.isEmpty()) {
            return false;
        }
        for (String ann : classAnnotations) {
            if (shortName.equals(ann)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the vulnerable method or any method in the call chain
     * calls a protected API (METHOD_CALL rule).
     */
    private boolean matchesMethodCall(MethodNode method, ProtectionRule rule) {
        String targetMethodName = rule.match().methodName();
        if (targetMethodName == null || targetMethodName.isEmpty()) {
            return false;
        }

        // 1. Check direct outgoing calls from the vulnerable method itself
        for (CallEdge edge : graph.getOutgoingCalls(method)) {
            if (targetMethodName.equals(edge.calleeName())) {
                return true;
            }
        }

        // 2. Check call chain from entry points
        return callChainProtected(method, targetMethodName);
    }

    /**
     * Checks whether any method in the reachable call chain from entry points
     * calls the specified protected method.
     * <p>
     * Walks the reachable set from each entry point and inspects outgoing calls
     * of every method in the chain. If the vulnerable method itself is reachable
     * and any method in the same reachable set calls the protected API,
     * the chain is considered protected.
     *
     * @param vulnerableMethod    the vulnerable method to check
     * @param protectedMethodName the protected API method name to detect
     * @return {@code true} if a protected API call exists in the call chain
     */
    boolean callChainProtected(MethodNode vulnerableMethod, String protectedMethodName) {
        List<MethodNode> entries = graph.getEntryPoints();
        if (entries.isEmpty()) {
            return false;
        }

        for (MethodNode entry : entries) {
            Set<MethodNode> reachable = graph.getReachable(Set.of(entry));
            if (!reachable.contains(vulnerableMethod)) {
                continue;
            }
            for (MethodNode chainMethod : reachable) {
                for (CallEdge edge : graph.getOutgoingCalls(chainMethod)) {
                    if (protectedMethodName.equals(edge.calleeName())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Re-parses the source code to extract annotations from the class
     * containing the method at {@code method.startLine()}.
     * <p>
     * Delegates to {@link SourceAnnotationExtractor}. Provided for
     * backward compatibility with existing tests.
     *
     * @param sourceCode the Java source code to parse
     * @param method     the vulnerable method node with line range information
     * @return list of annotation short names (e.g. {@code "PreAuthorize"})
     */
    List<String> extractClassAnnotations(String sourceCode, MethodNode method) {
        return SourceAnnotationExtractor.extractClassAnnotations(sourceCode, method.startLine());
    }

    /**
     * Finds the {@link MethodNode} in the call graph that corresponds to
     * the given finding's file path and line range.
     */
    Optional<MethodNode> findVulnerableMethod(Finding finding) {
        String filePath = finding.filePath();
        int line = finding.lineStart();

        if (filePath == null) {
            return Optional.empty();
        }

        // First pass: match by file path suffix AND line range
        for (MethodNode m : graph.getAllMethods()) {
            String pathSuffix = m.className().replace('.', '/') + ".java";
            if (filePath.endsWith(pathSuffix) || filePath.contains(pathSuffix)) {
                if (line == 0 || (m.startLine() <= line && line <= m.endLine())) {
                    return Optional.of(m);
                }
            }
        }

        // Fallback: match by simple class name at the end of the file path
        for (MethodNode m : graph.getAllMethods()) {
            String simpleClassName = classNameFromPath(filePath);
            if (simpleClassName != null && m.className().endsWith(simpleClassName)) {
                if (line == 0 || (m.startLine() <= line && line <= m.endLine())) {
                    return Optional.of(m);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Extracts the simple class name from a file path.
     * <p>
     * Example: {@code /tmp/com/example/UserDao.java} → {@code "UserDao"}.
     */
    private static String classNameFromPath(String filePath) {
        if (filePath == null) {
            return null;
        }
        String normalized = filePath.replace('\\', '/');
        int lastSlash = normalized.lastIndexOf('/');
        String fileName = lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
        if (fileName.endsWith(".java")) {
            return fileName.substring(0, fileName.length() - 5);
        }
        return fileName;
    }

    /**
     * Parses a raw YAML map into a {@link ProtectionRule}.
     * Delegates to {@link ProtectionRuleLoader}. Provided for backward
     * compatibility with existing tests.
     *
     * @param raw  the deserialized YAML mapping
     * @param file the source file name (for error messages)
     * @return a parsed ProtectionRule
     * @throws IllegalArgumentException if required fields are missing or have invalid values
     */
    public static ProtectionRule parseRule(Map<String, ?> raw, String file) {
        return ProtectionRuleLoader.parseRule(raw, file);
    }

    /**
     * Returns the number of loaded protection rules.
     */
    public int ruleCount() {
        return rules.size();
    }
}
