package com.codesec.engine.judge;

import java.util.List;
import java.util.Set;

/**
 * Represents a Java method node in the project call graph.
 * <p>
 * Identified by its fully qualified class name, method name, and parameter types.
 * Carries metadata about visibility, modifiers, annotations, and source location.
 */
public record MethodNode(
    String className,
    String methodName,
    List<String> parameterTypes,
    String returnType,
    List<String> annotations,
    int startLine,
    int endLine,
    boolean isStatic,
    boolean isPublic
) {

    private static final Set<String> ENTRY_POINT_ANNOTATIONS = Set.of(
        "@RestController",
        "@Controller",
        "@RequestMapping",
        "@GetMapping",
        "@PostMapping",
        "@PutMapping",
        "@DeleteMapping",
        "@PatchMapping"
    );

    /**
     * Full factory method with explicit values for all fields.
     */
    public static MethodNode of(
        String className,
        String methodName,
        List<String> parameterTypes,
        String returnType,
        List<String> annotations,
        int startLine,
        int endLine,
        boolean isStatic,
        boolean isPublic
    ) {
        return new MethodNode(
            className, methodName,
            List.copyOf(parameterTypes),
            returnType,
            List.copyOf(annotations),
            startLine, endLine,
            isStatic, isPublic
        );
    }

    /**
     * Convenience factory defaulting to instance method (non-static), public,
     * with zero-based source location.
     */
    public static MethodNode of(
        String className,
        String methodName,
        List<String> parameterTypes,
        String returnType,
        List<String> annotations
    ) {
        return of(className, methodName, parameterTypes, returnType, annotations, 0, 0, false, true);
    }

    /**
     * Returns a unique key for this method in the call graph.
     * <p>
     * Format: {@code className.methodName(paramType1,paramType2,...)}.
     * No-arg methods end with {@code ()}.
     */
    public String key() {
        String params = String.join(",", parameterTypes);
        return className + "." + methodName + "(" + params + ")";
    }

    /**
     * Returns a signature key for matching methods by name and parameter types,
     * ignoring the declaring class.
     * <p>
     * Format: {@code methodName(paramType1,paramType2,...)}.
     */
    public String signatureKey() {
        String params = String.join(",", parameterTypes);
        return methodName + "(" + params + ")";
    }

    /**
     * Returns {@code true} if any annotation on this method is recognized
     * as an HTTP entry point (Spring MVC controller or request mapping).
     */
    public boolean isEntryPoint() {
        for (String annotation : annotations) {
            if (ENTRY_POINT_ANNOTATIONS.contains(annotation)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Two MethodNodes are equal if they have the same identity:
     * class name, method name, and parameter types.
     * Other metadata such as annotations, visibility, and source location
     * do not participate in equality.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MethodNode other)) return false;
        return className.equals(other.className)
            && methodName.equals(other.methodName)
            && parameterTypes.equals(other.parameterTypes);
    }

    @Override
    public int hashCode() {
        int result = className.hashCode();
        result = 31 * result + methodName.hashCode();
        result = 31 * result + parameterTypes.hashCode();
        return result;
    }

    /**
     * Returns the annotations as comma-separated string for debugging.
     */
    @Override
    public String toString() {
        String annos = annotations.isEmpty() ? "" : " [" + String.join(", ", annotations) + "]";
        return key() + annos;
    }
}
