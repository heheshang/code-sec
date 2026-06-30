package com.codesec.engine.judge;

/**
 * Represents a call relationship from one method to another (potentially unresolved) method.
 * <p>
 * The callee is identified by name and parameter signature, not by a resolved
 * {@link MethodNode} reference, because the target method may be in a different
 * compilation unit or not yet known when the edge is created.
 */
public record CallEdge(
    MethodNode caller,
    String calleeName,
    String calleeSignature,
    int callSiteLine
) {

    /**
     * Creates a call edge with the given callee name and empty signature.
     * Used when the callee parameter types cannot be resolved from the call site.
     */
    public static CallEdge of(
        MethodNode caller,
        String calleeName,
        int callSiteLine
    ) {
        return new CallEdge(caller, calleeName, "", callSiteLine);
    }

    /**
     * Creates a call edge with explicit callee parameter signature.
     * The signature should be parameter type names joined by commas, e.g. {@code String,int}.
     */
    public static CallEdge of(
        MethodNode caller,
        String calleeName,
        String calleeSignature,
        int callSiteLine
    ) {
        return new CallEdge(caller, calleeName, calleeSignature, callSiteLine);
    }

    /**
     * Returns a composite identity string used for deduplication.
     * Format: {@code callerKey|calleeName(calleeSignature)|callSiteLine}
     */
    public String dedupKey() {
        return caller.key() + "|" + calleeName + "(" + calleeSignature + ")|" + callSiteLine;
    }

    @Override
    public String toString() {
        String sig = calleeSignature.isEmpty() ? "" : "(" + calleeSignature + ")";
        return caller.methodName() + " -> " + calleeName + sig + " @L" + callSiteLine;
    }
}
