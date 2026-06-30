package com.codesec.engine.judge;

import com.codesec.engine.parser.ParsedFile;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Scans a parsed Java source file for user-input taint sources.
 * <p>
 * Detects two categories of taint:
 * <ol>
 *   <li>Spring MVC parameter annotations ({@code @RequestParam}, {@code @RequestBody}, …)</li>
 *   <li>HttpServletRequest method calls ({@code getParameter()}, {@code getHeader()}, …)</li>
 * </ol>
 * <p>
 * v1 scope: only direct taint sources — no transitive data-flow tracing.
 */
public final class TaintTracker {

    private static final Logger log = LoggerFactory.getLogger(TaintTracker.class);

    private final ParsedFile file;

    /**
     * Creates a taint tracker for the given parsed file.
     *
     * @param file the parsed Java source file to scan
     */
    public TaintTracker(ParsedFile file) {
        this.file = file;
    }

    /**
     * Represents a single occurrence of a user-input source found in the file.
     *
     * @param source     the type of taint source detected
     * @param line       the source line number (1-based) where the taint occurs
     * @param expression a snippet of the expression (e.g., {@code request.getParameter("id")})
     */
    public record TaintOccurrence(TaintSource source, int line, String expression) {}

    /**
     * Finds all user-input taint source occurrences in the file.
     * <p>
     * Walks the entire compilation unit and reports:
     * <ul>
     *   <li>Every {@code MethodCallExpr} whose name matches a known
     *       {@link TaintSource#HTTP_REQUEST_METHODS HttpServletRequest method}</li>
     *   <li>Every {@code AnnotationExpr} on any method parameter whose name
     *       matches a known {@link TaintSource#SPRING_ANNOTATIONS Spring annotation}</li>
     * </ul>
     *
     * @return unmodifiable list of taint occurrences, sorted by line number
     */
    public List<TaintOccurrence> findTaintSources() {
        CompilationUnit cu;
        try {
            cu = StaticJavaParser.parse(file.sourceCode());
        } catch (Exception e) {
            log.warn("Failed to parse {}: {}", file.path(), e.getMessage());
            return List.of();
        }

        List<TaintOccurrence> occurrences = new ArrayList<>();

        // Detect HttpServletRequest method calls
        for (MethodCallExpr callExpr : cu.findAll(MethodCallExpr.class)) {
            String methodName = callExpr.getNameAsString();
            TaintSource source = TaintSource.fromServletMethod(methodName);
            if (source != null) {
                int line = callExpr.getRange().map(r -> r.begin.line).orElse(0);
                occurrences.add(new TaintOccurrence(source, line, callExpr.toString()));
            }
        }

        // Detect Spring annotations on parameters
        for (Parameter param : cu.findAll(Parameter.class)) {
            for (AnnotationExpr ann : param.getAnnotations()) {
                TaintSource source = TaintSource.fromSpringAnnotation(ann.getNameAsString());
                if (source != null) {
                    int line = param.getRange().map(r -> r.begin.line).orElse(0);
                    String expr = param.getTypeAsString() + " " + param.getNameAsString()
                        + " @" + ann.getNameAsString();
                    occurrences.add(new TaintOccurrence(source, line, expr));
                }
            }
        }

        occurrences.sort((a, b) -> Integer.compare(a.line, b.line));
        return Collections.unmodifiableList(occurrences);
    }

    /**
     * Determines which parameters of the given method are directly tainted
     * by Spring MVC annotations.
     * <p>
     * Matches the method declaration in the file by line range, then checks
     * each parameter's annotation list against
     * {@link TaintSource#SPRING_ANNOTATION_SIMPLE_NAMES}.
     * <p>
     * v1 scope: only Spring annotation parameters are returned here.
     * Body-level HttpServletRequest taint is checked separately by the caller.
     *
     * @param method the method node from the call graph
     * @return map from zero-based parameter index to the matched taint source;
     *         empty if the method cannot be found or no parameter is annotated
     */
    public Map<Integer, TaintSource> findTaintedParameters(MethodNode method) {
        Optional<MethodDeclaration> optDecl = findMethodDeclaration(method);
        if (optDecl.isEmpty()) {
            log.debug("Method {} not found in file {}, taint check skipped",
                method.key(), file.path());
            return Map.of();
        }

        MethodDeclaration decl = optDecl.get();
        Map<Integer, TaintSource> result = new HashMap<>();

        for (int i = 0; i < decl.getParameters().size(); i++) {
            Parameter param = decl.getParameters().get(i);
            for (AnnotationExpr ann : param.getAnnotations()) {
                String simpleName = ann.getNameAsString();
                if (TaintSource.SPRING_ANNOTATION_SIMPLE_NAMES.contains(simpleName)) {
                    TaintSource source = TaintSource.fromSpringAnnotation(simpleName);
                    if (source != null) {
                        result.put(i, source);
                    }
                }
            }
        }

        return Collections.unmodifiableMap(result);
    }

    /**
     * Checks whether the method body contains any direct call to an
     * HttpServletRequest user-input method (e.g., {@code getParameter()},
     * {@code getHeader()}, …).
     * <p>
     * v1 scope: only checks for the presence of such calls inside the method
     * body. Does not trace how the returned value flows.
     *
     * @param method the method node from the call graph
     * @return {@code true} if at least one taint source call is found in the body
     */
    public boolean hasBodyTaint(MethodNode method) {
        Optional<MethodDeclaration> optDecl = findMethodDeclaration(method);
        if (optDecl.isEmpty()) {
            return false;
        }

        MethodDeclaration decl = optDecl.get();
        for (MethodCallExpr callExpr : decl.findAll(MethodCallExpr.class)) {
            if (TaintSource.fromServletMethod(callExpr.getNameAsString()) != null) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the list of taint occurrences that fall within the body of the
     * given method.
     *
     * @param method the method node from the call graph
     * @return taint occurrences within the method body (may be empty)
     */
    public List<TaintOccurrence> findBodyTaints(MethodNode method) {
        Optional<MethodDeclaration> optDecl = findMethodDeclaration(method);
        if (optDecl.isEmpty()) {
            return List.of();
        }

        MethodDeclaration decl = optDecl.get();
        int methodStart = decl.getRange().map(r -> r.begin.line).orElse(0);
        int methodEnd = decl.getRange().map(r -> r.end.line).orElse(Integer.MAX_VALUE);

        return findTaintSources().stream()
            .filter(t -> t.line >= methodStart && t.line <= methodEnd)
            .toList();
    }

    /**
     * Locates the {@link MethodDeclaration} in the parsed file that corresponds
     * to the given {@link MethodNode}.
     * <p>
     * Matching strategy (in order):
     * <ol>
     *   <li>If the method node carries a start line &gt; 0, match by
     *       declaration range (start–end lines must overlap).</li>
     *   <li>Fallback: match by method name, parameter count, and class name
     *       suffix (for partial signature resolution).</li>
     * </ol>
     *
     * @param method the call-graph method node to locate
     * @return the matching declaration, or empty if not found
     */
    private Optional<MethodDeclaration> findMethodDeclaration(MethodNode method) {
        CompilationUnit cu;
        try {
            cu = StaticJavaParser.parse(file.sourceCode());
        } catch (Exception e) {
            log.warn("Failed to parse {}: {}", file.path(), e.getMessage());
            return Optional.empty();
        }

        List<MethodDeclaration> candidates = cu.findAll(MethodDeclaration.class);

        // Strategy 1: match by line range
        if (method.startLine() > 0) {
            for (MethodDeclaration md : candidates) {
                int declStart = md.getRange().map(r -> r.begin.line).orElse(0);
                int declEnd = md.getRange().map(r -> r.end.line).orElse(0);
                if (method.startLine() >= declStart && method.startLine() <= declEnd) {
                    return Optional.of(md);
                }
            }
        }

        // Strategy 2: fallback — match by method name, parameter count,
        // and class name suffix
        String methodName = method.methodName();
        int paramCount = method.parameterTypes().size();
        String classSimpleName = simpleClassName(method.className());

        for (MethodDeclaration md : candidates) {
            if (!md.getNameAsString().equals(methodName)) {
                continue;
            }
            if (md.getParameters().size() != paramCount) {
                continue;
            }
            if (enclosingClassMatches(md, classSimpleName)) {
                return Optional.of(md);
            }
        }

        return Optional.empty();
    }

    /**
     * Extracts the simple class name from a fully-qualified class name.
     */
    private static String simpleClassName(String fqn) {
        int lastDot = fqn.lastIndexOf('.');
        return lastDot >= 0 ? fqn.substring(lastDot + 1) : fqn;
    }

    /**
     * Checks whether the enclosing class of the given method declaration
     * matches the expected simple name by walking up the AST.
     */
    private static boolean enclosingClassMatches(MethodDeclaration md, String simpleName) {
        return md.getParentNode()
            .filter(p -> p instanceof com.github.javaparser.ast.body.ClassOrInterfaceDeclaration)
            .map(p -> ((com.github.javaparser.ast.body.ClassOrInterfaceDeclaration) p).getNameAsString())
            .orElse("")
            .equals(simpleName);
    }
}
