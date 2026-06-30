package com.codesec.gitlab.scan;

import com.codesec.gitlab.model.MrChange;
import com.codesec.gitlab.model.MrChangesResponse;

import java.util.*;

/**
 * Extracts file paths from a GitLab MR Changes API response.
 *
 * <h3>Rules</h3>
 * <ul>
 *   <li>Deleted files ({@code deleted_file=true}) are excluded</li>
 *   <li>Renamed files use {@code new_path}</li>
 *   <li>When the number of files exceeds 500, the result is truncated to
 *       the first 500 files sorted by {@code new_path} lexicographically</li>
 * </ul>
 */
public final class DiffExtractor {

    /** Maximum number of files to extract per MR. */
    public static final int MAX_FILES = 500;

    private DiffExtractor() {}

    /**
     * Extracts the list of file paths to scan from an MR changes response.
     *
     * @param response the MR changes API response
     * @return the extraction result
     */
    public static MrDiffResult extract(MrChangesResponse response) {
        if (response == null || response.changes() == null || response.changes().isEmpty()) {
            return MrDiffResult.empty();
        }

        List<MrChange> scanChanges = response.changes().stream()
            .filter(MrChange::shouldScan)
            .toList();

        if (scanChanges.isEmpty()) {
            return MrDiffResult.emptyAllDeleted(response.changes().size());
        }

        // Sort by new_path for deterministic ordering
        List<MrChange> sorted = scanChanges.stream()
            .sorted(Comparator.comparing(MrChange::newPath, String::compareTo))
            .toList();

        boolean truncated = sorted.size() > MAX_FILES;
        List<MrChange> effective = truncated ? sorted.subList(0, MAX_FILES) : sorted;

        List<String> relativeFiles = effective.stream()
            .map(MrChange::effectivePath)
            .toList();

        Map<String, String> fileContents = new LinkedHashMap<>();
        for (MrChange change : effective) {
            String diff = change.diff();
            if (diff != null) {
                fileContents.put(change.effectivePath(), diff);
            }
        }

        return new MrDiffResult(relativeFiles, fileContents,
            scanChanges.size(), truncated);
    }

    /** Extract file list only (no diff contents). Convenience method. */
    public static List<String> extractFiles(MrChangesResponse response) {
        return extract(response).relativeFiles();
    }
}
