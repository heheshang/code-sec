package com.codesec.engine.judge;

import com.codesec.engine.model.Finding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Determines whether a vulnerable method is reachable from any HTTP entry point
 * via the project call graph. Implements Algorithm 1 (code reachability) of
 * the ExploitabilityJudger feature.
 *
 * <p>Uses {@link ProjectCallGraph#getReachable(Set)} to perform the BFS
 * heavy lifting, then reconstructs a human-readable call path for positive
 * results.</p>
 */
public final class ReachableAnalyzer implements Algorithm {

    private static final Logger log = LoggerFactory.getLogger(ReachableAnalyzer.class);

    private final ProjectCallGraph graph;

    /**
     * Creates a new analyzer backed by the given project call graph.
     *
     * @param graph the pre-built project call graph
     */
    public ReachableAnalyzer(ProjectCallGraph graph) {
        this.graph = Objects.requireNonNull(graph, "graph must not be null");
    }

    @Override
    public String name() {
        return "reachable";
    }

    /**
     * Classify a SAST finding by checking whether the vulnerable method is
     * reachable from any HTTP entry point.
     *
     * @param finding the SAST finding to analyze; never {@code null}
     * @return an {@link AlgorithmResult} with determined status, answer, and
     *         a human-readable Chinese reason
     */
    @Override
    public AlgorithmResult classify(Finding finding) {
        Objects.requireNonNull(finding, "finding must not be null");

        // 1. Find the vulnerable method node
        Optional<MethodNode> methodOpt = findMethod(finding);
        if (methodOpt.isEmpty()) {
            String clsName = extractClassName(finding.filePath());
            String detail = clsName != null
                ? clsName + ":" + finding.lineStart()
                : finding.filePath() + ":" + finding.lineStart();
            log.info("未找到方法节点: {}", detail);
            return AlgorithmResult.undetermined(detail);
        }
        MethodNode target = methodOpt.get();

        // 2. Get entry points
        List<MethodNode> entryPoints = graph.getEntryPoints();
        if (entryPoints.isEmpty()) {
            log.info("项目无 HTTP 入口");
            return AlgorithmResult.undetermined("项目无 HTTP 入口");
        }

        log.info("已扫描 {} 个方法，发现 {} 个入口", graph.size(), entryPoints.size());

        // 3. Compute reachable set from all entry points
        Set<MethodNode> reachable = graph.getReachable(new HashSet<>(entryPoints));

        // 4. Check if target is in the reachable set
        boolean isReachable = false;
        for (MethodNode m : reachable) {
            if (m.equals(target)) {
                isReachable = true;
                break;
            }
        }

        if (!isReachable) {
            return AlgorithmResult.no("未被任何 HTTP 入口调用");
        }

        // 5. Build a human-readable call path
        String path = buildPath(entryPoints, target);
        return AlgorithmResult.yes("可达路径: " + path);
    }

    /**
     * Look up the {@link MethodNode} whose source range contains the finding's
     * line number.
     *
     * @param finding the SAST finding
     * @return the matching method node, or empty if none found
     */
    private Optional<MethodNode> findMethod(Finding finding) {
        String className = extractClassName(finding.filePath());
        if (className == null || className.isEmpty()) {
            return Optional.empty();
        }

        List<MethodNode> methods = graph.getMethodsByClass(className);
        if (methods.isEmpty()) {
            return Optional.empty();
        }

        int line = finding.lineStart();
        for (MethodNode m : methods) {
            if (m.startLine() <= line && line <= m.endLine()) {
                return Optional.of(m);
            }
        }
        return Optional.empty();
    }

    /**
     * Extract a fully qualified class name from a file path.
     *
     * <p>Handles two common forms:
     * <ul>
     *   <li>{@code src/main/java/com/example/Foo.java} &rarr; {@code com.example.Foo}</li>
     *   <li>{@code com/example/Foo.java} &rarr; {@code com.example.Foo}</li>
     * </ul>
     * Returns {@code null} for paths that cannot be parsed as a Java file.</p>
     *
     * @param filePath the file path from the finding
     * @return the fully qualified class name, or {@code null}
     */
    private static String extractClassName(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return null;
        }

        String path = filePath.replace('\\', '/');

        int javaIdx = path.indexOf("src/main/java/");
        if (javaIdx >= 0) {
            path = path.substring(javaIdx + "src/main/java/".length());
        }

        if (!path.endsWith(".java")) {
            return null;
        }
        path = path.substring(0, path.length() - ".java".length());

        if (path.isEmpty()) {
            return null;
        }

        return path.replace('/', '.');
    }

    /**
     * Build a human-readable call path from one of the entry points to the
     * target method via BFS with parent tracking.
     *
     * @param entryPoints the HTTP entry points
     * @param target      the vulnerable method node
     * @return a path string like {@code EntryClass.entryMethod() → Service.process() → Dao.query()}
     */
    private String buildPath(List<MethodNode> entryPoints, MethodNode target) {
        Map<MethodNode, MethodNode> parent = new HashMap<>();
        Deque<MethodNode> queue = new ArrayDeque<>();
        Set<MethodNode> visited = new HashSet<>();

        for (MethodNode entry : entryPoints) {
            visited.add(entry);
            queue.add(entry);
        }

        while (!queue.isEmpty()) {
            MethodNode current = queue.poll();

            if (current.equals(target)) {
                List<String> parts = new ArrayList<>();
                MethodNode node = target;
                while (node != null) {
                    parts.add(0, node.methodName() + "()");
                    node = parent.get(node);
                }
                return String.join(" → ", parts);
            }

            List<CallEdge> outgoing = graph.getOutgoingCalls(current);
            for (CallEdge edge : outgoing) {
                String sig = edge.calleeName() + "(" + edge.calleeSignature() + ")";
                for (MethodNode candidate : graph.getAllMethods()) {
                    if (candidate.signatureKey().equals(sig) && !visited.contains(candidate)) {
                        visited.add(candidate);
                        parent.put(candidate, current);
                        queue.add(candidate);
                    }
                }
            }
        }

        return target.methodName() + "()";
    }
}
