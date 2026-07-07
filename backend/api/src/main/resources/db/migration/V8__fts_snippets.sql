-- ============================================
-- Flyway V8: Snippet search indexes
-- Adds file_path prefix index + GIN trigram
-- index for fuzzy path matching.
-- ============================================

-- 1. B-tree index for file_path prefix search (LIKE 'prefix%')
CREATE INDEX IF NOT EXISTS idx_vuln_file_path_prefix ON vuln_finding (file_path varchar_pattern_ops);

-- 2. GIN trigram index for fuzzy file_path search (ILIKE '%term%')
CREATE INDEX IF NOT EXISTS idx_vuln_file_path_trgm ON vuln_finding USING GIN (file_path gin_trgm_ops);
