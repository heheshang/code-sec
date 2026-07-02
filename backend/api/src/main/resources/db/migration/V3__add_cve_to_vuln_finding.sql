-- ============================================
-- Flyway V3: Add cve column to vuln_finding
-- Epic: schema alignment — engine produces cve,
-- but entity, DTO, and persistence drop the field
-- ============================================

ALTER TABLE vuln_finding
    ADD COLUMN cve VARCHAR(64);

CREATE INDEX idx_vuln_cve ON vuln_finding (cve);
