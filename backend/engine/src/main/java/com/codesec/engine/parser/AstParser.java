package com.codesec.engine.parser;

import java.io.IOException;
import java.nio.file.Path;

public interface AstParser {
    ParsedFile parse(Path filePath) throws IOException;
    boolean supports(Path filePath);
    String languageName();
}
