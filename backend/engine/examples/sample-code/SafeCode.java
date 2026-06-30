package com.codesec.sample;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Sample code with no security vulnerabilities (negative test).
 * No findings expected.
 */
public class SafeCode {

    public String readFile(Path filePath) throws Exception {
        return Files.readString(filePath);
    }

    public int add(int a, int b) {
        return a + b;
    }

    public String greet(String name) {
        return "Hello, " + name + "!";
    }
}
