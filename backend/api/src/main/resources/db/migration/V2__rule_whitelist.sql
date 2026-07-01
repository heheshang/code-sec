-- ============================================
-- Flyway V2: Rule whitelist/project exemption system
-- Epic: E-S3-RULE
-- Tables: rule_metadata, project_rule_exemption
-- PostgreSQL dialect
-- ============================================

CREATE TABLE rule_metadata (
    id BIGSERIAL PRIMARY KEY,
    rule_id VARCHAR(128) NOT NULL,
    name VARCHAR(256) NOT NULL,
    severity VARCHAR(16) NOT NULL,
    cwe VARCHAR(32) DEFAULT NULL,
    language VARCHAR(32) NOT NULL DEFAULT 'java',
    engine VARCHAR(32) NOT NULL DEFAULT 'self_sast',
    detection_type VARCHAR(16) NOT NULL DEFAULT 'ast',
    description TEXT DEFAULT NULL,
    fix_suggestion TEXT DEFAULT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    imported_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_rule_metadata_rule_id UNIQUE (rule_id)
);

CREATE INDEX idx_rule_metadata_enabled ON rule_metadata (enabled);
CREATE INDEX idx_rule_metadata_severity ON rule_metadata (severity);

CREATE TRIGGER trg_rule_metadata_updated_at
    BEFORE UPDATE ON rule_metadata
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE rule_metadata IS 'Rule metadata imported from engine YAML rules';
COMMENT ON COLUMN rule_metadata.rule_id IS 'e.g. java/sql-injection-001';
COMMENT ON COLUMN rule_metadata.detection_type IS 'ast | regex';

CREATE TABLE project_rule_exemption (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    rule_id BIGINT NOT NULL,
    reason TEXT DEFAULT NULL,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP DEFAULT NULL,
    CONSTRAINT uk_project_rule UNIQUE (project_id, rule_id),
    FOREIGN KEY (rule_id) REFERENCES rule_metadata (id) ON DELETE CASCADE,
    FOREIGN KEY (project_id) REFERENCES repo (id) ON DELETE CASCADE
);

CREATE INDEX idx_exemption_project ON project_rule_exemption (project_id);

COMMENT ON TABLE project_rule_exemption IS 'Project-level rule exemptions/whitelist';
COMMENT ON COLUMN project_rule_exemption.reason IS 'Exemption reason';
COMMENT ON COLUMN project_rule_exemption.expires_at IS 'Optional expiration';
