-- ============================================
-- Flyway V5: PostgreSQL Full-Text Search support
-- Replaces Elasticsearch with PG tsvector/tsquery
-- Uses zhparser for Chinese segmentation if available,
-- falls back to pg_catalog.simple otherwise.
-- ============================================

-- 1. Create custom text search configuration
-- zhparser handles Chinese word segmentation (prod);
-- pg_catalog.simple is the fallback for CI/local.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_available_extensions WHERE name = 'zhparser') THEN
        CREATE EXTENSION zhparser;
        CREATE TEXT SEARCH CONFIGURATION codesec_cfg (PARSER = zhparser);
        ALTER TEXT SEARCH CONFIGURATION codesec_cfg
            ADD MAPPING FOR n,v,a,i,e,l,m WITH simple;
        COMMENT ON EXTENSION zhparser IS 'Chinese word segmentation for full-text search';
        COMMENT ON TEXT SEARCH CONFIGURATION codesec_cfg IS 'CodeSec custom: zhparser (Chinese) + simple dictionary';
    ELSE
        CREATE TEXT SEARCH CONFIGURATION codesec_cfg (COPY = pg_catalog.simple);
        COMMENT ON TEXT SEARCH CONFIGURATION codesec_cfg IS 'CodeSec fallback: pg_catalog.simple (zhparser not available)';
        RAISE WARNING 'zhparser extension not found — using pg_catalog.simple. Chinese text search quality may be reduced. Install zhparser (https://github.com/amutu/zhparser) for production.';
    END IF;
END;
$$;

-- 3. Add tsvector columns to vuln_finding (idempotent)
-- tsv_title_desc: combines title + description
-- tsv_code_snippet: covers code_snippet field
ALTER TABLE vuln_finding
    ADD COLUMN IF NOT EXISTS tsv_title_desc tsvector;

ALTER TABLE vuln_finding
    ADD COLUMN IF NOT EXISTS tsv_code_snippet tsvector;

-- 4. Create GIN indexes for fast @@ tsquery operations (idempotent)
CREATE INDEX IF NOT EXISTS idx_vuln_tsv_title_desc ON vuln_finding
    USING GIN (tsv_title_desc);

CREATE INDEX IF NOT EXISTS idx_vuln_tsv_code_snippet ON vuln_finding
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
DROP TRIGGER IF EXISTS trg_vuln_finding_tsv ON vuln_finding;
CREATE TRIGGER trg_vuln_finding_tsv
    BEFORE INSERT OR UPDATE ON vuln_finding
    FOR EACH ROW EXECUTE FUNCTION vuln_finding_tsv_update();

-- 7. Backfill tsvector for existing data (idempotent)
UPDATE vuln_finding
SET tsv_title_desc = to_tsvector('codesec_cfg',
        COALESCE(title, '') || ' ' || COALESCE(description, '')),
    tsv_code_snippet = to_tsvector('codesec_cfg',
        COALESCE(code_snippet, ''))
WHERE tsv_title_desc IS NULL OR tsv_code_snippet IS NULL;

COMMENT ON COLUMN vuln_finding.tsv_title_desc IS 'tsvector for title + description (full-text search)';
COMMENT ON COLUMN vuln_finding.tsv_code_snippet IS 'tsvector for code_snippet (full-text search)';
COMMENT ON TRIGGER trg_vuln_finding_tsv ON vuln_finding IS 'Auto-update tsvector columns on insert/update';
