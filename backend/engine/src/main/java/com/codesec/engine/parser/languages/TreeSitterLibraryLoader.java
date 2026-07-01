package com.codesec.engine.parser.languages;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles loading of the tree-sitter native library via JNI.
 * <p>
 * When the native library is unavailable (CI / no brew install), all methods
 * gracefully degrade so that the engine can still operate with text-based
 * parsing — consistent with "CI compatibility: silent degradation" (spec § 3.1).
 * </p>
 *
 * <p><b>macOS setup:</b><pre>{@code brew install tree-sitter}</pre></p>
 * <p><b>CI / Docker:</b> ship {@code libtree-sitter.so} in the image.</p>
 */
public final class TreeSitterLibraryLoader {
    private static final Logger log = LoggerFactory.getLogger(TreeSitterLibraryLoader.class);

    private static final String[] LIB_NAMES = {
        "tree-sitter",       // Linux (libtree-sitter.so)
        "tree-sitter.0.23",  // macOS Homebrew (libtree-sitter.0.23.dylib)
    };

    private static volatile boolean available = false;
    private static volatile Throwable loadError = null;

    static {
        tryLoad();
    }

    private TreeSitterLibraryLoader() {
        // utility class
    }

    private static void tryLoad() {
        for (String libName : LIB_NAMES) {
            try {
                System.loadLibrary(libName);
                available = true;
                log.info("Tree-sitter native library loaded: {}", libName);
                return;
            } catch (UnsatisfiedLinkError e) {
                log.debug("Could not load tree-sitter library '{}': {}", libName, e.getMessage());
            }
        }
        loadError = new UnsatisfiedLinkError(
            "tree-sitter native library not found. Install with: brew install tree-sitter " +
            "(macOS) or ship libtree-sitter.so in your Docker image (Linux). " +
            "Falling back to text-based parsing."
        );
        log.warn(loadError.getMessage());
    }

    /**
     * Returns {@code true} iff the tree-sitter native library was loaded successfully.
     */
    public static boolean isAvailable() {
        return available;
    }

    /**
     * Returns the load error if the native library could not be loaded, or
     * {@code null} if loading succeeded.
     */
    public static Throwable loadError() {
        return loadError;
    }
}
