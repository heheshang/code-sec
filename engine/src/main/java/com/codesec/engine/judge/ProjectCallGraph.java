package com.codesec.engine.judge;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * In-memory project-level call graph for Java source code.
 * <p>
 * Maintains bidirectional method-to-method call relationships and provides
 * BFS-based transitive reachability queries. Not thread-safe; intended for
 * single-threaded use within a scan session.
 */
public final class ProjectCallGraph {

    private final Map<String, MethodNode> methodsByKey;
    private final Map<String, Set<MethodNode>> methodsBySignatureKey;
    private final Map<String, List<CallEdge>> outgoingEdgesByCallerKey;
    private final Map<String, List<CallEdge>> incomingEdgesByCalleeSig;
    private final Set<String> knownEdgeKeys;

    /**
     * Creates an empty project call graph.
     */
    public ProjectCallGraph() {
        this.methodsByKey = new HashMap<>();
        this.methodsBySignatureKey = new HashMap<>();
        this.outgoingEdgesByCallerKey = new HashMap<>();
        this.incomingEdgesByCalleeSig = new HashMap<>();
        this.knownEdgeKeys = new HashSet<>();
    }

    /**
     * Adds a method node to the graph. Silently deduplicates by key:
     * if a method with the same key already exists, this call is a no-op.
     */
    public void addMethod(MethodNode method) {
        String key = method.key();
        if (methodsByKey.containsKey(key)) {
            return;
        }
        methodsByKey.put(key, method);
        methodsBySignatureKey
            .computeIfAbsent(method.signatureKey(), k -> new HashSet<>())
            .add(method);
    }

    /**
     * Adds a call edge to the graph. Silently deduplicates:
     * edges with the same caller key, callee name, callee signature,
     * and call site line are treated as duplicates and ignored.
     */
    public void addCall(CallEdge edge) {
        String dedupKey = edge.dedupKey();
        if (knownEdgeKeys.contains(dedupKey)) {
            return;
        }
        knownEdgeKeys.add(dedupKey);

        String callerKey = edge.caller().key();
        outgoingEdgesByCallerKey
            .computeIfAbsent(callerKey, k -> new ArrayList<>())
            .add(edge);

        String calleeSigKey = edge.calleeName() + "(" + edge.calleeSignature() + ")";
        incomingEdgesByCalleeSig
            .computeIfAbsent(calleeSigKey, k -> new ArrayList<>())
            .add(edge);
    }

    /**
     * Returns the method node for the given key, or empty if not found.
     *
     * @param key the method key, as produced by {@link MethodNode#key()}
     */
    public Optional<MethodNode> getMethodByKey(String key) {
        return Optional.ofNullable(methodsByKey.get(key));
    }

    /**
     * Returns all methods in the graph as an unmodifiable list.
     */
    public List<MethodNode> getAllMethods() {
        return List.copyOf(methodsByKey.values());
    }

    /**
     * Returns all methods declared in the given fully qualified class,
     * as an unmodifiable list.
     *
     * @param fqn the fully qualified class name
     */
    public List<MethodNode> getMethodsByClass(String fqn) {
        List<MethodNode> result = new ArrayList<>();
        for (MethodNode m : methodsByKey.values()) {
            if (m.className().equals(fqn)) {
                result.add(m);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Returns all methods that are recognized as HTTP or framework entry points.
     *
     * @see MethodNode#isEntryPoint()
     */
    public List<MethodNode> getEntryPoints() {
        List<MethodNode> result = new ArrayList<>();
        for (MethodNode m : methodsByKey.values()) {
            if (m.isEntryPoint()) {
                result.add(m);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Returns all outgoing call edges from the given caller method,
     * as an unmodifiable list. Returns an empty list if the caller has no
     * known outgoing edges.
     */
    public List<CallEdge> getOutgoingCalls(MethodNode caller) {
        List<CallEdge> edges = outgoingEdgesByCallerKey.get(caller.key());
        if (edges == null) {
            return List.of();
        }
        return Collections.unmodifiableList(edges);
    }

    /**
     * Returns all incoming call edges targeting methods matching the given
     * callee signature, as an unmodifiable list.
     *
     * @param calleeSigKey signature key in the format {@code methodName(paramType1,paramType2)}
     */
    public List<CallEdge> getIncomingCalls(String calleeSigKey) {
        List<CallEdge> edges = incomingEdgesByCalleeSig.get(calleeSigKey);
        if (edges == null) {
            return List.of();
        }
        return Collections.unmodifiableList(edges);
    }

    /**
     * Performs a breadth-first search forward from the given entry methods,
     * returning all transitively reachable methods (including the entries themselves).
     * <p>
     * Handles cycles and self-recursion through a visited set — no method
     * is visited more than once.
     *
     * @param entries the set of starting methods; must be methods already in this graph
     * @return all methods reachable from any entry, including the entries themselves
     */
    public Set<MethodNode> getReachable(Set<MethodNode> entries) {
        Set<MethodNode> visited = new HashSet<>();
        Deque<MethodNode> queue = new ArrayDeque<>();

        for (MethodNode entry : entries) {
            if (methodsByKey.containsKey(entry.key())) {
                visited.add(entry);
                queue.add(entry);
            }
        }

        while (!queue.isEmpty()) {
            MethodNode current = queue.poll();
            List<CallEdge> outgoing = outgoingEdgesByCallerKey.get(current.key());
            if (outgoing == null) {
                continue;
            }
            for (CallEdge edge : outgoing) {
                String sigKey = edge.calleeName() + "(" + edge.calleeSignature() + ")";
                Set<MethodNode> targets = methodsBySignatureKey.get(sigKey);
                if (targets == null) {
                    continue;
                }
                for (MethodNode target : targets) {
                    if (!visited.contains(target)) {
                        visited.add(target);
                        queue.add(target);
                    }
                }
            }
        }

        return visited;
    }

    /**
     * Returns the total number of methods in the graph.
     */
    public int size() {
        return methodsByKey.size();
    }

    /**
     * Returns the total number of call edges in the graph.
     */
    public int edgeCount() {
        return knownEdgeKeys.size();
    }
}
