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

public final class SqlInjectionDetector extends AstDetector {

    private static final Set<String> SQL_METHODS = Set.of("executeQuery", "executeUpdate", "execute");

    @Override
    protected List<AstMatch> findMatches(CompilationUnit cu, ParsedFile file, Rule rule) {
        List<AstMatch> matches = new ArrayList<>();
        Set<String> taintedVars = new HashSet<>();

        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(VariableDeclarator decl, Void arg) {
                decl.getInitializer().ifPresent(init -> {
                    if (containsStringConcat(init)) {
                        taintedVars.add(decl.getNameAsString());
                    }
                });
                super.visit(decl, arg);
            }

            @Override
            public void visit(MethodCallExpr call, Void arg) {
                if (isSqlMethod(call)) {
                    for (Expression argExpr : call.getArguments()) {
                        if (isRiskySqlArg(argExpr, taintedVars)) {
                            int startLine = call.getBegin().map(p -> p.line).orElse(0);
                            int endLine = call.getEnd().map(p -> p.line).orElse(startLine);
                            String snippet = call.toString();
                            String reason = "SQL method " + call.getNameAsString()
                                + "() receives potentially untrusted input via string concatenation";
                            matches.add(new AstMatch(startLine, endLine, snippet,
                                rule.name(), reason));
                            break;
                        }
                    }
                }
                super.visit(call, arg);
            }
        }, null);

        return matches;
    }

    private static boolean isSqlMethod(MethodCallExpr call) {
        return SQL_METHODS.contains(call.getNameAsString());
    }

    private static boolean isRiskySqlArg(Expression arg, Set<String> taintedVars) {
        if (arg.isBinaryExpr()) {
            return containsStringConcat(arg);
        }
        if (arg.isNameExpr()) {
            String varName = arg.asNameExpr().getNameAsString();
            return taintedVars.contains(varName);
        }
        return false;
    }

    private static boolean containsStringConcat(Expression expr) {
        if (expr.isBinaryExpr()) {
            BinaryExpr bin = expr.asBinaryExpr();
            if (bin.getOperator() == BinaryExpr.Operator.PLUS) {
                if (containsValueExpr(bin.getLeft()) || containsValueExpr(bin.getRight())) {
                    return true;
                }
                return containsStringConcat(bin.getLeft()) || containsStringConcat(bin.getRight());
            }
        }
        return false;
    }

    private static boolean containsValueExpr(Expression expr) {
        if (expr.isStringLiteralExpr()) {
            return true;
        }
        if (expr.isNameExpr()) {
            return true;
        }
        if (expr.isMethodCallExpr()) {
            return true;
        }
        return false;
    }
}
