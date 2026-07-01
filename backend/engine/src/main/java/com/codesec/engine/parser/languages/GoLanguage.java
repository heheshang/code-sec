package com.codesec.engine.parser.languages;

import com.codesec.engine.parser.AstParser;
import com.codesec.engine.parser.ParsedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Go source-code parser implementing {@link AstParser}.
 * <p>
 * When the tree-sitter native library is available, parsing is delegated
 * to JNI-based CST construction. Otherwise, a simple text-based parse is
 * performed — sufficient for the regex and source-level detectors to operate.
 * </p>
 */
public final class GoLanguage extends TreeSitterAdapter {
    private static final Logger log = LoggerFactory.getLogger(GoLanguage.class);

    private static final String LANGUAGE = "go";

    public GoLanguage() {
        super(TreeSitterLibraryLoader.isAvailable());
    }

    @Override
    public boolean supports(Path filePath) {
        return filePath.getFileName().toString().endsWith(".go");
    }

    @Override
    public String languageName() {
        return LANGUAGE;
    }

    @Override
    protected Object parseWithTreeSitter(Path filePath, String sourceCode) {
        // TODO: replace with actual tree-sitter grammar parse when JNI binding is deployed
        // try (Language goLang = Language.load("/path/to/tree-sitter-go.wasm")) { ... }
        log.debug("tree-sitter JNI would parse Go file: {}", filePath);
        return null;
    }
}
