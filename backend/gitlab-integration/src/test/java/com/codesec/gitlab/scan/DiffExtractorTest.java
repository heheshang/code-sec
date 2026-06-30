package com.codesec.gitlab.scan;

import com.codesec.gitlab.model.MrChange;
import com.codesec.gitlab.model.MrChangesResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("DiffExtractor")
class DiffExtractorTest {

    @Nested
    @DisplayName("basic extraction")
    class BasicExtraction {

        @Test
        @DisplayName("should extract file paths from changes")
        void extractFiles() {
            MrChangesResponse response = createResponse(
                change("src/main/java/Foo.java", false, false),
                change("src/main/java/Bar.java", false, false)
            );

            MrDiffResult result = DiffExtractor.extract(response);

            assertThat(result.relativeFiles()).containsExactly(
                "src/main/java/Bar.java",
                "src/main/java/Foo.java"
            );
            assertThat(result.truncated()).isFalse();
            assertThat(result.totalFiles()).isEqualTo(2);
        }

        @Test
        @DisplayName("should filter out deleted files")
        void filterDeletedFiles() {
            MrChangesResponse response = createResponse(
                change("src/main/java/Foo.java", false, false),
                change("src/main/java/Old.java", true, false)
            );

            MrDiffResult result = DiffExtractor.extract(response);

            assertThat(result.relativeFiles()).containsExactly("src/main/java/Foo.java");
            assertThat(result.totalFiles()).isEqualTo(1);
        }

        @Test
        @DisplayName("should use new_path for renamed files")
        void renamedFiles() {
            MrChangesResponse response = createResponse(
                change("src/main/java/OldName.java", "src/main/java/NewName.java",
                    false, false, true)
            );

            MrDiffResult result = DiffExtractor.extract(response);

            assertThat(result.relativeFiles()).containsExactly("src/main/java/NewName.java");
        }

        @Test
        @DisplayName("should handle empty response")
        void emptyResponse() {
            MrDiffResult result = DiffExtractor.extract(null);
            assertThat(result.isEmpty()).isTrue();

            MrDiffResult result2 = DiffExtractor.extract(createResponse());
            assertThat(result2.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("should return empty when all files are deleted")
        void allDeleted() {
            MrChangesResponse response = createResponse(
                change("src/main/java/Del1.java", true, false),
                change("src/main/java/Del2.java", true, false)
            );

            MrDiffResult result = DiffExtractor.extract(response);

            assertThat(result.isEmpty()).isTrue();
            assertThat(result.totalFiles()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("truncation")
    class Truncation {

        @Test
        @DisplayName("should truncate to 500 files when exceeding limit")
        void truncateAt500() {
            List<MrChange> changes = new ArrayList<>();
            for (int i = 0; i < 600; i++) {
                changes.add(change(String.format("src/main/java/File%03d.java", i), false, false));
            }

            MrChangesResponse response = createResponse(changes);
            MrDiffResult result = DiffExtractor.extract(response);

            assertThat(result.truncated()).isTrue();
            assertThat(result.totalFiles()).isEqualTo(600);
            assertThat(result.relativeFiles()).hasSize(DiffExtractor.MAX_FILES);
        }

        @Test
        @DisplayName("should not truncate when exactly 500 files")
        void noTruncationAt500() {
            List<MrChange> changes = new ArrayList<>();
            for (int i = 0; i < 500; i++) {
                changes.add(change(String.format("src/main/java/File%03d.java", i), false, false));
            }

            MrChangesResponse response = createResponse(changes);
            MrDiffResult result = DiffExtractor.extract(response);

            assertThat(result.truncated()).isFalse();
            assertThat(result.relativeFiles()).hasSize(500);
        }

        @Test
        @DisplayName("should sort files lexicographically before truncation")
        void sortedTruncation() {
            List<MrChange> changes = new ArrayList<>();
            // Add files in reverse order
            for (int i = 600; i > 0; i--) {
                changes.add(change(String.format("src/main/java/File%03d.java", i), false, false));
            }

            MrChangesResponse response = createResponse(changes);
            MrDiffResult result = DiffExtractor.extract(response);

            // First file should be File001 (lexicographically first)
            assertThat(result.relativeFiles().get(0)).isEqualTo("src/main/java/File001.java");
        }
    }

    @Nested
    @DisplayName("extractFiles convenience")
    class ExtractFiles {

        @Test
        @DisplayName("should return file list only")
        void convenienceMethod() {
            MrChangesResponse response = createResponse(
                change("src/a.java", false, false),
                change("src/b.java", false, false)
            );

            List<String> files = DiffExtractor.extractFiles(response);
            assertThat(files).hasSize(2);
            assertThat(files).contains("src/a.java", "src/b.java");
        }
    }

    // --- helpers ---

    private static MrChangesResponse createResponse(MrChange... changes) {
        return createResponse(List.of(changes));
    }

    private static MrChangesResponse createResponse(List<MrChange> changes) {
        return new MrChangesResponse(
            1L, 1L, "Test MR", "feature", "main",
            changes,
            "abc123",
            new MrChangesResponse.DiffRefs("base", "head", "start")
        );
    }

    private static MrChange change(String path, boolean deleted, boolean renamed) {
        return new MrChange(path, path, false, deleted, renamed, "diff content");
    }

    private static MrChange change(String oldPath, String newPath, boolean newFile,
                                    boolean deleted, boolean renamed) {
        return new MrChange(oldPath, newPath, newFile, deleted, renamed, "diff content");
    }
}
