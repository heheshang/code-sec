package com.codesec.engine.judge;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts class-level annotations from Java source code.
 * Extracted from {@link FrameworkProtectionDetector} for size control.
 */
public final class SourceAnnotationExtractor {

    private static final Logger log = LoggerFactory.getLogger(SourceAnnotationExtractor.class);

    private SourceAnnotationExtractor() {
        /* utility class */
    }

    /**
     * Re-parses the source code to extract annotations from the class
     * containing the method at the given line.
     *
     * @param sourceCode the Java source code to parse
     * @param methodLine the start line of the method (1-based)
     * @return list of annotation short names (e.g. {@code "PreAuthorize"})
     */
    public static List<String> extractClassAnnotations(String sourceCode, int methodLine) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(sourceCode);
            for (MethodDeclaration md : cu.findAll(MethodDeclaration.class)) {
                if (methodWithinDeclaration(md, methodLine)) {
                    return collectAnnotationsFromEnclosingClasses(md);
                }
            }
        } catch (Exception e) {
            log.debug("Failed to parse source for class-level annotations: {}", e.getMessage());
        }
        return List.of();
    }

    /**
     * Walks up from a method declaration through parent nodes, collecting
     * annotations from all enclosing {@link ClassOrInterfaceDeclaration}
     * nodes (innermost first, outermost last).
     */
    private static List<String> collectAnnotationsFromEnclosingClasses(MethodDeclaration md) {
        List<String> annotations = new ArrayList<>();
        Node current = md.getParentNode().orElse(null);

        while (current != null) {
            if (current instanceof ClassOrInterfaceDeclaration cid) {
                for (AnnotationExpr ann : cid.getAnnotations()) {
                    annotations.add(ann.getNameAsString());
                }
            }
            current = current.getParentNode().orElse(null);
        }

        return annotations;
    }

    /**
     * Returns {@code true} if the method declaration's line range contains
     * the given line number.
     */
    private static boolean methodWithinDeclaration(MethodDeclaration md, int line) {
        return md.getRange()
            .filter(r -> r.begin.line <= line && line <= r.end.line)
            .isPresent();
    }
}
