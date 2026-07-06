package com.codesec.codex.client;

import com.codesec.codex.model.PatchResult;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class AstCompiler {

    public PatchResult validate(String patchCode, String language) {
        PatchResult result = new PatchResult(patchCode, language);
        if (patchCode == null || patchCode.isBlank()) {
            result.setCompilationStatus("FAIL");
            result.setErrors(List.of("Empty patch code"));
            return result;
        }
        List<String> errors = new ArrayList<>();

        if ("java".equalsIgnoreCase(language)) {
            validateJavaSyntax(patchCode, errors);
        } else if ("go".equalsIgnoreCase(language)) {
            validateGoSyntax(patchCode, errors);
        } else if ("python".equalsIgnoreCase(language)) {
            validatePythonSyntax(patchCode, errors);
        }

        if (errors.isEmpty()) {
            // Bracket-balance is a coarse sanity check, not real compilation.
            // Do not claim PASS; report PENDING so callers know it is unverified.
            result.setCompilationStatus("PENDING");
        } else {
            result.setCompilationStatus("FAIL");
            result.setErrors(errors);
        }
        return result;
    }

    private void validateJavaSyntax(String code, List<String> errors) {
        if (!code.contains(";") && !code.contains("{") && !code.contains("}")
            && !code.contains("//") && !code.contains("/*")) {
            errors.add("Not valid Java code: missing statements, braces, or semicolons");
        }
        if (countChar(code, '{') != countChar(code, '}')) {
            errors.add("Mismatched braces: check your { and } counts");
        }
        if (countChar(code, '(') != countChar(code, ')')) {
            errors.add("Mismatched parentheses: check your ( and ) counts");
        }
        if (Pattern.compile("\\bimport\\b").matcher(code).find()) {
            if (Pattern.compile("\\bpackage\\b").matcher(code).find()) {
                if (!code.contains(";")) {
                    errors.add("Package/import statements must end with semicolon");
                }
            }
        }
    }

    private void validateGoSyntax(String code, List<String> errors) {
        if (countChar(code, '{') != countChar(code, '}')) {
            errors.add("Mismatched braces in Go code");
        }
        if (countChar(code, '(') != countChar(code, ')')) {
            errors.add("Mismatched parentheses in Go code");
        }
    }

    private void validatePythonSyntax(String code, List<String> errors) {
        // Python uses no braces; only flag obviously empty patches.
        if (code.lines().filter(l -> !l.trim().isEmpty() && !l.trim().startsWith("#")).toList().isEmpty()) {
            errors.add("No executable Python statements found");
        }
    }

    private int countChar(String s, char c) {
        return (int) s.chars().filter(ch -> ch == c).count();
    }
}
