package com.codesec.engine.judge;

/**
 * A single protection rule loaded from YAML.
 * <p>
 * Defines what framework protection to detect. Each rule has a type that
 * determines how matching is performed:
 * <ul>
 *   <li>{@link Type#ANNOTATION} — check if the vulnerable method has a specific annotation</li>
 *   <li>{@link Type#CLASS_ANNOTATION} — check if the vulnerable method's containing class has the annotation</li>
 *   <li>{@link Type#METHOD_CALL} — check if the call chain includes a call to a specific protected API method</li>
 * </ul>
 *
 * <p>YAML format:
 * <pre>{@code
 * name: Spring Security @PreAuthorize
 * type: annotation
 * match:
 *   annotation: org.springframework.security.access.prepost.PreAuthorize
 *   scope: method
 * reason: Spring Security @PreAuthorize 注解保护
 * }</pre>
 *
 * @param name  human-readable rule name
 * @param type  detection strategy
 * @param match matching criteria for the rule
 * @param reason explanation used in exploit verdict
 */
public record ProtectionRule(
    String name,
    Type type,
    MatchSpec match,
    String reason
) {

    /**
     * Detection strategy for the rule.
     */
    public enum Type {
        /** Match against method-level annotations in {@link MethodNode#annotations()}. */
        ANNOTATION,
        /** Match against any method in the call chain that calls the specified protected API. */
        METHOD_CALL,
        /** Match against class-level annotations on the vulnerable method's containing class. */
        CLASS_ANNOTATION
    }

    /**
     * Matching criteria for a protection rule.
     *
     * @param annotation fully-qualified annotation name (e.g. {@code org.springframework.security.access.prepost.PreAuthorize})
     * @param scope      where to look: {@code "method"}, {@code "class"}, or {@code "both"}
     * @param methodName for METHOD_CALL rules: the method name to detect (e.g. {@code "selectOne"})
     * @param className  for METHOD_CALL rules: the hosting class FQN (informational)
     */
    public record MatchSpec(
        String annotation,
        String scope,
        String methodName,
        String className
    ) {
        /**
         * Extracts the simple (short) name from a fully-qualified annotation name.
         * <p>
         * Example: {@code org.springframework.security.access.prepost.PreAuthorize} → {@code "PreAuthorize"}.
         *
         * @return the last segment of the FQN, or empty string if null
         */
        public String annotationShortName() {
            if (annotation == null || annotation.isEmpty()) {
                return "";
            }
            int lastDot = annotation.lastIndexOf('.');
            return lastDot >= 0 ? annotation.substring(lastDot + 1) : annotation;
        }
    }
}
