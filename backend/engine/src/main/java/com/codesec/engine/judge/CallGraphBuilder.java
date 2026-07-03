package com.codesec.engine.judge;

import com.codesec.engine.config.CpgConfiguration;
import com.codesec.engine.parser.ParsedFile;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Builds a {@link ProjectCallGraph} from a list of parsed Java source files.
 * <p>
 * Uses JavaParser 3.26.2 to extract method declarations and method call expressions,
 * constructing method nodes and call edges. Parse errors are logged and skipped
 * gracefully without interrupting the overall build.
 * <p>
 * Usage:
 * <pre>{@code
 * CallGraphBuilder builder = new CallGraphBuilder();
 * ProjectCallGraph graph = builder.build(parsedFiles);
 * }</pre>
 */
public final class CallGraphBuilder {

    private static final Logger log = LoggerFactory.getLogger(CallGraphBuilder.class);

    /** Creates a new builder with default configuration. */
    public CallGraphBuilder() {
    }

    /**
     * Builds a project call graph from the given parsed files.
     * <p>
     * Only files with {@code language.equals("java")} are processed.
     * Files that fail to parse are logged at WARN level and skipped.
     *
     * @param files the list of parsed source files (may include non-Java files)
     * @return a populated project call graph
     */
    public ProjectCallGraph build(List<ParsedFile> files) {
        ProjectCallGraph graph = new ProjectCallGraph();

        if (files == null || files.isEmpty()) {
            log.info("No files provided; returning empty call graph");
            return graph;
        }

        List<CompilationUnit> compilationUnits = new ArrayList<>();

        for (ParsedFile file : files) {
            if (!"java".equals(file.language())) {
                log.debug("Skipping non-Java file: {}", file.path());
                continue;
            }

            try {
                CompilationUnit cu = StaticJavaParser.parse(file.sourceCode());
                compilationUnits.add(cu);
            } catch (Exception e) {
                log.warn("Failed to parse {}: {}", file.path(), e.getMessage());
            }
        }

        // Pass 1: extract all methods
        for (CompilationUnit cu : compilationUnits) {
            String packagePrefix = cu.getPackageDeclaration()
                .map(pd -> pd.getNameAsString() + ".")
                .orElse("");

            for (ClassOrInterfaceDeclaration classDecl : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                String fqn = packagePrefix + buildClassName(classDecl);

                for (MethodDeclaration methodDecl : classDecl.getMethods()) {
                    MethodNode node = methodNodeFromDeclaration(fqn, methodDecl);
                    graph.addMethod(node);
                }
            }
        }

        log.info("Extracted {} methods across {} compilation units",
            graph.size(), compilationUnits.size());

        // Pass 2: extract all call edges
        for (CompilationUnit cu : compilationUnits) {
            String packagePrefix = cu.getPackageDeclaration()
                .map(pd -> pd.getNameAsString() + ".")
                .orElse("");

            for (ClassOrInterfaceDeclaration classDecl : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                String fqn = packagePrefix + buildClassName(classDecl);

                for (MethodDeclaration methodDecl : classDecl.getMethods()) {
                    if (methodDecl.getBody().isEmpty()) {
                        continue;
                    }

                    MethodNode caller = resolveCaller(fqn, methodDecl, graph);
                    if (caller == null) {
                        continue;
                    }

                    for (MethodCallExpr callExpr : methodDecl.findAll(MethodCallExpr.class)) {
                        String calleeName = callExpr.getNameAsString();
                        String calleeSignature = buildCallSignature(callExpr);
                        int line = callExpr.getRange()
                            .map(r -> r.begin.line)
                            .orElse(0);

                        CallEdge edge = CallEdge.of(caller, calleeName, calleeSignature, line);
                        graph.addCall(edge);
                    }
                }
            }
        }

        log.info("Built call graph with {} methods and {} edges",
            graph.size(), graph.edgeCount());

        return graph;
    }

    /**
     * Builds a call graph and persists it to Neo4j asynchronously.
     * Falls back to in-memory-only if Neo4j is unavailable or disabled.
     *
     * @param files     the list of parsed source files
     * @param scanId    the scan ID for Neo4j scoping
     * @param cpgConfig the CPG store configuration
     * @return the in-memory ProjectCallGraph (always returned regardless of persistence)
     */
    public ProjectCallGraph buildAndPersist(List<ParsedFile> files, String scanId, CpgConfiguration cpgConfig) {
        ProjectCallGraph graph = build(files);

        if (cpgConfig != null && cpgConfig.isEnabled()) {
            CpgService cpgService = new CpgService(cpgConfig);
            try {
                cpgService.clearScan(scanId);
                cpgService.importGraph(graph, scanId);
                log.info("CPG persisted to Neo4j for scan: {}", scanId);
            } catch (Exception e) {
                log.warn("CPG Neo4j persistence skipped: {}", e.getMessage());
            }
        } else {
            log.info("CPG store is memory-only; skipping Neo4j persistence");
        }

        return graph;
    }

    /**
     * Converts a JavaParser MethodDeclaration into a MethodNode.
     */
    private MethodNode methodNodeFromDeclaration(String fqn, MethodDeclaration md) {
        String methodName = md.getNameAsString();

        List<String> parameterTypes = new ArrayList<>();
        for (Parameter param : md.getParameters()) {
            parameterTypes.add(param.getType().toString());
        }

        String returnType = md.getType().toString();

        List<String> annotations = new ArrayList<>();
        for (AnnotationExpr ann : md.getAnnotations()) {
            annotations.add("@" + ann.getNameAsString());
        }

        int startLine = md.getRange().map(r -> r.begin.line).orElse(0);
        int endLine = md.getRange().map(r -> r.end.line).orElse(0);
        boolean isStatic = md.isStatic();
        boolean isPublic = md.isPublic();

        return MethodNode.of(
            fqn, methodName, parameterTypes, returnType,
            annotations, startLine, endLine, isStatic, isPublic
        );
    }

    /**
     * Builds a lookup key string for the method declaration,
     * then retrieves the canonical {@link MethodNode} from the graph.
     *
     * @param fqn   the fully qualified class name
     * @param md    the method declaration
     * @param graph the project call graph to look up the canonical instance from
     * @return the canonical MethodNode, or {@code null} if not found
     */
    private MethodNode resolveCaller(String fqn, MethodDeclaration md, ProjectCallGraph graph) {
        String methodName = md.getNameAsString();
        List<String> paramTypes = new ArrayList<>();
        for (Parameter param : md.getParameters()) {
            paramTypes.add(param.getType().toString());
        }
        String params = String.join(",", paramTypes);
        String key = fqn + "." + methodName + "(" + params + ")";
        return graph.getMethodByKey(key).orElse(null);
    }

    /**
     * Builds a caller signature string from a MethodCallExpr's arguments.
     * Attempts to infer types for literal expressions; returns empty string
     * for arguments whose types cannot be resolved without symbol resolution.
     */
    private String buildCallSignature(MethodCallExpr callExpr) {
        List<String> argTypes = new ArrayList<>();
        for (Expression arg : callExpr.getArguments()) {
            argTypes.add(inferExpressionType(arg));
        }
        return String.join(",", argTypes);
    }

    /**
     * Attempts to infer the Java type name of an expression.
     * For literals, the type is known. For everything else, returns
     * an empty string indicating an unresolved type.
     */
    private String inferExpressionType(Expression expr) {
        if (expr instanceof StringLiteralExpr) {
            return "String";
        }
        if (expr instanceof IntegerLiteralExpr) {
            return "int";
        }
        if (expr instanceof BooleanLiteralExpr) {
            return "boolean";
        }
        if (expr instanceof CharLiteralExpr) {
            return "char";
        }
        if (expr instanceof DoubleLiteralExpr) {
            return "double";
        }
        if (expr instanceof LongLiteralExpr) {
            return "long";
        }
        if (expr instanceof NullLiteralExpr) {
            return "null";
        }
        return "";
    }

    /**
     * Builds the class name hierarchy for a (possibly nested) class declaration.
     * Walks up the AST parent chain collecting class names.
     * Example: {@code Outer.Inner} for a nested inner class.
     */
    private String buildClassName(ClassOrInterfaceDeclaration classDecl) {
        List<String> nameParts = new ArrayList<>();
        Node node = classDecl;
        while (node != null) {
            if (node instanceof ClassOrInterfaceDeclaration cid) {
                nameParts.add(0, cid.getNameAsString());
            }
            node = node.getParentNode().orElse(null);
        }
        return String.join(".", nameParts);
    }
}
