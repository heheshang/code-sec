package com.codesec.engine.parser.languages;

import com.codesec.engine.parser.AstParser;
import com.codesec.engine.parser.ParsedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Abstract base class for tree-sitter based {@link AstParser} implementations.
 * <p>
 * When the tree-sitter native library is unavailable, subclasses automatically
 * fall back to text-based (source-only) parsing — the {@code ast} field in
 * the returned {@link ParsedFile} will be {@code null}, but detectors that
 * work on {@code sourceCode()} directly will still operate correctly.
 * </p>
 */
public abstract class TreeSitterAdapter implements AstParser {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final boolean nativeAvailable;

    /**
     * @param nativeAvailable pre-populated from {@link TreeSitterLibraryLoader#isAvailable()}
     */
    protected TreeSitterAdapter(boolean nativeAvailable) {
        this.nativeAvailable = nativeAvailable;
    }

    @Override
    public final ParsedFile parse(Path filePath) throws IOException {
        String sourceCode = Files.readString(filePath);

        if (nativeAvailable) {
            try {
                Object cst = parseWithTreeSitter(filePath, sourceCode);
                log.debug("Parsed {} with tree-sitter: {}", languageName(), filePath);
                return new ParsedFile(filePath, languageName(), sourceCode, cst);
            } catch (Exception e) {
                log.warn("Tree-sitter parse failed for {} ({}), falling back to text", filePath, e.getMessage());
            }
        }

        log.debug("Text-parsed {} file: {}", languageName(), filePath);
        return new ParsedFile(filePath, languageName(), sourceCode, null);
    }

    /**
     * Parse source code with the tree-sitter native library.
     * <p>
     * Only called when {@link #nativeAvailable} is {@code true}. Subclasses
     * that have a JNI grammar loaded should produce a CST object here.
     * </p>
     *
     * @param filePath   the source file path (for diagnostics)
     * @param sourceCode the full source text
     * @return a CST / AST representation, or {@code null}
     */
    protected abstract Object parseWithTreeSitter(Path filePath, String sourceCode);
}
