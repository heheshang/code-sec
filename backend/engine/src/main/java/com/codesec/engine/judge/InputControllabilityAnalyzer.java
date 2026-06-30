package com.codesec.engine.judge;

import com.codesec.engine.model.Finding;
import com.codesec.engine.parser.ParsedFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

/**
 * Determines whether the parameters of a vulnerable method are user-controllable
 * (tainted by data originating from HTTP request input).
 *
 * <h2>Algorithm 2 — Input Controllability (v1)</h2>
 * <ol>
 *   <li>Locate the vulnerable method in the call graph that corresponds to
 *       the {@link Finding}'s file path and line range.</li>
 *   <li>Check whether any parameter of that method carries a Spring MVC
 *       taint annotation ({@code @RequestParam}, {@code @RequestBody}, …).
 *       If so, return {@link AlgorithmResult.Yes} with the parameter name
 *       and annotation.</li>
 *   <li>Otherwise, parse the method body and look for direct calls to
 *       HttpServletRequest input methods ({@code getParameter()},
 *       {@code getHeader()}, …). If found, return a conservative
 *       {@link AlgorithmResult.Yes}.</li>
 *   <li>If neither condition holds, return {@link AlgorithmResult.Undetermined}.</li>
 * </ol>
 *
 * <p>v1 scope: only <em>direct</em> taint sources. Transitive data-flow
 * tracing is deferred to v2.</p>
 */
public final class InputControllabilityAnalyzer implements Algorithm {

    private static final Logger log = LoggerFactory.getLogger(InputControllabilityAnalyzer.class);

    private final ProjectCallGraph graph;
    private final Map<String, ParsedFile> parsedFiles;

    /**
     * Creates an analyzer backed by the given call graph and parsed source files.
     *
     * @param graph       the project-level call graph built from the scan target
     * @param parsedFiles map from relative file path to the corresponding
     *                    parsed file (as produced by the parser phase)
     */
    public InputControllabilityAnalyzer(ProjectCallGraph graph, Map<String, ParsedFile> parsedFiles) {
        this.graph = graph;
        this.parsedFiles = parsedFiles;
    }

    @Override
    public String name() {
        return "user-controllable";
    }

    /**
     * Classifies whether the given finding's sink is user-controllable.
     *
     * @param finding the vulnerability finding to analyse
     * @return {@link AlgorithmResult.Yes} if user-controllable input is confirmed,
     *         {@link AlgorithmResult.Undetermined} otherwise
     */
    @Override
    public AlgorithmResult classify(Finding finding) {
        String filePath = finding.filePath();

        // 1. Locate the ParsedFile
        ParsedFile parsedFile = parsedFiles.get(filePath);
        if (parsedFile == null) {
            log.warn("ParsedFile not found for path '{}'; cannot classify", filePath);
            return AlgorithmResult.undetermined("无法找到源码文件: " + filePath);
        }

        // 2. Find the vulnerable method node in the call graph
        Optional<MethodNode> optMethod = findVulnerableMethod(finding);
        if (optMethod.isEmpty()) {
            log.debug("No matching method found in call graph for finding at {}:{}-{}",
                filePath, finding.lineStart(), finding.lineEnd());
            return AlgorithmResult.undetermined("无法在调用图中定位对应方法");
        }

        MethodNode method = optMethod.get();

        // 3. Create TaintTracker and run taint analysis
        TaintTracker tracker = new TaintTracker(parsedFile);

        // 3a. Check parameter-level Spring annotations
        Map<Integer, TaintSource> taintedParams = tracker.findTaintedParameters(method);
        if (!taintedParams.isEmpty()) {
            int paramIdx = taintedParams.keySet().iterator().next();
            TaintSource source = taintedParams.get(paramIdx);
            String paramName = paramNameFromMethod(method, paramIdx);
            return AlgorithmResult.yes("参数 " + paramName + " 来自 " + source.getDescription());
        }

        // 3b. Check body-level HttpServletRequest calls
        if (tracker.hasBodyTaint(method)) {
            var bodyTaints = tracker.findBodyTaints(method);
            if (!bodyTaints.isEmpty()) {
                TaintSource source = bodyTaints.get(0).source();
                return AlgorithmResult.yes("方法体内发现用户输入源: " + source.getDescription());
            }
            return AlgorithmResult.yes("方法体内发现用户输入源 (HttpServletRequest)");
        }

        // 4. No taint found
        return AlgorithmResult.undetermined("未发现用户可控输入源");
    }

    /**
     * Finds the {@link MethodNode} in the call graph that contains the
     * finding's source location.
     * <p>
     * Matches by converting the finding's file path to a fully-qualified
     * class name prefix, then finding a method whose start/end line range
     * overlaps with the finding's line. Falls back to matching by class
     * name only when the method's source location is unavailable (0,0).
     *
     * @param finding the vulnerability finding
     * @return the matching method node, or empty if not found
     */
    private Optional<MethodNode> findVulnerableMethod(Finding finding) {
        String filePath = finding.filePath();
        String classFqnPrefix = filePathToClassFqn(filePath);

        for (MethodNode method : graph.getAllMethods()) {
            if (!method.className().startsWith(classFqnPrefix)) {
                continue;
            }

            // Match by line range when the method has known source location
            if (method.startLine() > 0 && method.endLine() > 0) {
                if (finding.lineStart() >= method.startLine()
                    && finding.lineStart() <= method.endLine()) {
                    return Optional.of(method);
                }
            }
        }

        // Fallback: any method in the matching class (for synthetic methods with (0,0) range)
        for (MethodNode method : graph.getAllMethods()) {
            if (method.className().startsWith(classFqnPrefix)) {
                log.debug("Fallback match for finding at {}: method {}", filePath, method.key());
                return Optional.of(method);
            }
        }

        return Optional.empty();
    }

    /**
     * Converts a Java source file path to a fully-qualified class name.
     * <p>
     * Example: {@code src/main/java/com/example/UserController.java} →
     * {@code com.example.UserController}.
     */
    static String filePathToClassFqn(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }
        String path = filePath.replace('\\', '/');

        int javaIdx = path.indexOf("src/main/java/");
        if (javaIdx < 0) {
            javaIdx = path.indexOf("src/test/java/");
        }
        if (javaIdx >= 0) {
            path = path.substring(javaIdx + "src/main/java/".length());
        }

        if (path.endsWith(".java")) {
            path = path.substring(0, path.length() - 5);
        }

        return path.replace('/', '.');
    }

    /**
     * Extracts the parameter name at the given index from the method's
     * look-up in the call graph.
     * <p>
     * Falls back to {@code "param[" + index + "]"} when the parsed source
     * is not available.
     */
    private String paramNameFromMethod(MethodNode method, int index) {
        String filePath = classNameToFilePath(method.className());
        ParsedFile parsedFile = parsedFiles.get(filePath);
        if (parsedFile != null) {
            try {
                var cu = com.github.javaparser.StaticJavaParser.parse(parsedFile.sourceCode());
                for (var md : cu.findAll(com.github.javaparser.ast.body.MethodDeclaration.class)) {
                    if (md.getNameAsString().equals(method.methodName())
                        && md.getParameters().size() > index) {
                        int declStart = md.getRange().map(r -> r.begin.line).orElse(0);
                        int declEnd = md.getRange().map(r -> r.end.line).orElse(0);
                        if (method.startLine() == 0
                            || (method.startLine() >= declStart && method.startLine() <= declEnd)) {
                            return md.getParameters().get(index).getNameAsString();
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Failed to resolve parameter name for {}: {}", method.key(), e.getMessage());
            }
        }
        return "param[" + index + "]";
    }

    /**
     * Converts a fully-qualified class name back to a relative file path.
     * <p>
     * Tries both {@code src/main/java/} and {@code src/test/java/} prefixes.
     */
    private static String classNameToFilePath(String fqn) {
        String path = "src/main/java/" + fqn.replace('.', '/') + ".java";
        return path;
    }
}
