-- ============================================
-- Flyway V5: PostgreSQL Full-Text Search support
-- Replaces Elasticsearch with PG tsvector/tsquery
-- Requires zhparser extension (Chinese word segmentation)
-- ============================================

-- 1. Install zhparser extension (fails gracefully if unavailable)
CREATE EXTENSION IF NOT EXISTS zhparser;

-- 2. Create custom text search configuration
-- zhparser handles Chinese word segmentation
-- simple dictionary passes tokens through (lowercased)
CREATE TEXT SEARCH CONFIGURATION codesec_cfg (PARSER = zhparser);
ALTER TEXT SEARCH CONFIGURATION codesec_cfg
    ADD MAPPING FOR n,v,a,i,e,l,m WITH simple;

-- 3. Add tsvector columns to vuln_finding
-- tsv_title_desc: combines title + description
-- tsv_code_snippet: covers code_snippet field
ALTER TABLE vuln_finding
    ADD COLUMN tsv_title_desc tsvector;

ALTER TABLE vuln_finding
    ADD COLUMN tsv_code_snippet tsvector;

-- 4. Create GIN indexes for fast @@ tsquery operations
CREATE INDEX idx_vuln_tsv_title_desc ON vuln_finding
    USING GIN (tsv_title_desc);

CREATE INDEX idx_vuln_tsv_code_snippet ON vuln_finding
    USING GIN (tsv_code_snippet);

-- 5. Create trigger function to auto-maintain tsvector columns
CREATE OR REPLACE FUNCTION vuln_finding_tsv_update()
RETURNS TRIGGER AS $$
BEGIN
    NEW.tsv_title_desc := to_tsvector('codesec_cfg',
        COALESCE(NEW.title, '') || ' ' || COALESCE(NEW.description, ''));
    NEW.tsv_code_snippet := to_tsvector('codesec_cfg',
        COALESCE(NEW.code_snippet, ''));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 6. Create trigger on INSERT or UPDATE
CREATE TRIGGER trg_vuln_finding_tsv
    BEFORE INSERT OR UPDATE ON vuln_finding
    FOR EACH ROW EXECUTE FUNCTION vuln_finding_tsv_update();

-- 7. Backfill tsvector for existing data
UPDATE vuln_finding
SET tsv_title_desc = to_tsvector('codesec_cfg',
        COALESCE(title, '') || ' ' || COALESCE(description, '')),
    tsv_code_snippet = to_tsvector('codesec_cfg',
        COALESCE(code_snippet, ''))
WHERE tsv_title_desc IS NULL;

COMMENT ON EXTENSION zhparser IS 'Chinese word segmentation for full-text search';
COMMENT ON TEXT SEARCH CONFIGURATION codesec_cfg IS 'CodeSec custom: zhparser (Chinese) + simple dictionary';
COMMENT ON COLUMN vuln_finding.tsv_title_desc IS 'tsvector for title + description (full-text search)';
COMMENT ON COLUMN vuln_finding.tsv_code_snippet IS 'tsvector for code_snippet (full-text search)';
COMMENT ON TRIGGER trg_vuln_finding_tsv ON vuln_finding IS 'Auto-update tsvector columns on insert/update';
