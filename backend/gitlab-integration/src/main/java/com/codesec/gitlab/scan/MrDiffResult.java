package com.codesec.gitlab.scan;

import java.util.*;

/**
 * Result of extracting file changes from a GitLab MR.
 *
 * @param relativeFiles ordered list of file paths to scan
 * @param fileContents  map from file path to its Git diff content
 * @param totalFiles    total number of changed files (before truncation)
 * @param truncated     true when the result was truncated to {@link DiffExtractor#MAX_FILES}
 */
public record MrDiffResult(
    List<String> relativeFiles,
    Map<String, String> fileContents,
    int totalFiles,
    boolean truncated
) {
    /** Empty diff result. */
    public static MrDiffResult empty() {
        return new MrDiffResult(List.of(), Map.of(), 0, false);
    }

    /** All changes were deleted files. */
    public static MrDiffResult emptyAllDeleted(int totalDeleted) {
        return new MrDiffResult(List.of(), Map.of(), totalDeleted, false);
    }

    /** Whether the diff is completely empty (no changes to scan). */
    public boolean isEmpty() {
        return relativeFiles.isEmpty();
    }
}
