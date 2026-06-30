package com.codesec.engine.parser;

import java.nio.file.Path;

public record ParsedFile(
    Path path,
    String language,
    String sourceCode,
    Object ast
) {
    public String relativePath() {
        return path.toString();
    }
}
