package com.codesec.codex.capability;

import com.codesec.codex.client.AstCompiler;
import com.codesec.codex.model.PatchResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AstCompilerTest {

    private final AstCompiler compiler = new AstCompiler();

    @Test
    void validJavaCodeReturnsPass() {
        String code = "public class Test {\n    public void foo() {\n        int x = 1;\n    }\n}";
        PatchResult result = compiler.validate(code, "java");
        assertEquals("PASS", result.getCompilationStatus());
    }

    @Test
    void mismatchedBracesReturnsFail() {
        String code = "public class Test {\n    public void foo() {\n        int x = 1;\n    }\n";
        PatchResult result = compiler.validate(code, "java");
        assertEquals("FAIL", result.getCompilationStatus());
        assertNotNull(result.getErrors());
        assertFalse(result.getErrors().isEmpty());
    }

    @Test
    void emptyCodeReturnsFail() {
        PatchResult result = compiler.validate("", "java");
        assertEquals("FAIL", result.getCompilationStatus());
    }

    @Test
    void nullCodeReturnsFail() {
        PatchResult result = compiler.validate(null, "java");
        assertEquals("FAIL", result.getCompilationStatus());
    }

    @Test
    void validGoCodeReturnsPass() {
        String code = "package main\nfunc main() {\n    println(\"hello\")\n}";
        PatchResult result = compiler.validate(code, "go");
        assertEquals("PASS", result.getCompilationStatus());
    }
}
