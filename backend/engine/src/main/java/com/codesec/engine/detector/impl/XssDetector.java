package com.codesec.engine.detector.impl;

import com.codesec.engine.detector.AstDetector;
import com.codesec.engine.parser.ParsedFile;
import com.codesec.engine.rule.Rule;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class XssDetector extends AstDetector {

    private static final Set<String> XSS_SINKS = Set.of("println", "print", "write", "append");
    private static final Set<String> UNTRUSTED_SOURCES = Set.of(
        "getParameter", "getParameterValues", "getQueryString", "getHeader", "getCookies"
    );

    @Override
    protected List<AstMatch> findMatches(CompilationUnit cu, ParsedFile file, Rule rule) {
        List<AstMatch> matches = new ArrayList<>();
        Set<String> taintedVars = new HashSet<>();

        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(MethodCallExpr call, Void arg) {
                if (isUntrustedSource(call) && call.getParentNode().isPresent()) {
                    var parent = call.getParentNode().get();
                    if (parent instanceof VariableDeclarator decl) {
                        taintedVars.add(decl.getNameAsString());
                    }
                }

                if (isXssSink(call) && hasTaintedInput(call, taintedVars)) {
                    int startLine = call.getBegin().map(p -> p.line).orElse(0);
                    int endLine = call.getEnd().map(p -> p.line).orElse(startLine);
                    String snippet = call.toString();
                    String reason = "Untrusted input flows to HTTP response sink ("
                        + call.getNameAsString() + ") without sanitization";
                    matches.add(new AstMatch(startLine, endLine, snippet, rule.name(), reason));
                }

                super.visit(call, arg);
            }
        }, null);

        return matches;
    }

    private static boolean isXssSink(MethodCallExpr call) {
        if (!XSS_SINKS.contains(call.getNameAsString())) {
            return false;
        }
        if (call.getScope().isEmpty()) {
            return false;
        }
        Expression scope = call.getScope().get();
        if (scope.isMethodCallExpr()) {
            MethodCallExpr scopeCall = scope.asMethodCallExpr();
            return scopeCall.getNameAsString().equals("getWriter")
                || scopeCall.getNameAsString().equals("getOutputStream");
        }
        return false;
    }

    private static boolean isUntrustedSource(MethodCallExpr call) {
        if (!UNTRUSTED_SOURCES.contains(call.getNameAsString())) {
            return false;
        }
        if (call.getScope().isPresent()) {
            String scopeStr = call.getScope().get().toString().toLowerCase();
            return scopeStr.contains("request") || scopeStr.contains("req");
        }
        return false;
    }

    private static boolean hasTaintedInput(MethodCallExpr call, Set<String> taintedVars) {
        for (Expression arg : call.getArguments()) {
            if (taintedExpr(arg, taintedVars)) {
                return true;
            }
        }
        return false;
    }

    private static boolean taintedExpr(Expression expr, Set<String> taintedVars) {
        if (expr.isNameExpr()) {
            return taintedVars.contains(expr.asNameExpr().getNameAsString());
        }
        if (expr.isBinaryExpr()) {
            BinaryExpr bin = expr.asBinaryExpr();
            return taintedExpr(bin.getLeft(), taintedVars)
                || taintedExpr(bin.getRight(), taintedVars);
        }
        if (expr.isMethodCallExpr()) {
            MethodCallExpr call = expr.asMethodCallExpr();
            if (isUntrustedSource(call)) {
                return true;
            }
            for (Expression arg : call.getArguments()) {
                if (taintedExpr(arg, taintedVars)) {
                    return true;
                }
            }
        }
        return false;
    }
}
