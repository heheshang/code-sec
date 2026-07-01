-- ============================================
-- Flyway V1: Core schema initialization
-- Epic: E-S2-CRITICAL Backend Business Services
-- 10 tables for code security audit platform
-- PostgreSQL dialect
-- ============================================

-- Helper function: auto-update updated_at columns
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- 1. user table (RBAC)
-- ============================================
CREATE TABLE "user" (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    email VARCHAR(128),
    password_hash VARCHAR(256) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_username UNIQUE (username)
);

CREATE TRIGGER trg_user_updated_at
    BEFORE UPDATE ON "user"
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- 2. role table (RBAC)
-- ============================================
CREATE TABLE role (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(32) NOT NULL,
    description VARCHAR(256),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_role_name UNIQUE (name)
);

-- ============================================
-- 3. permission table (RBAC)
-- ============================================
CREATE TABLE permission (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    resource VARCHAR(32) NOT NULL,
    action VARCHAR(32) NOT NULL,
    CONSTRAINT uk_permission_name UNIQUE (name),
    CONSTRAINT uk_resource_action UNIQUE (resource, action)
);

-- ============================================
-- 4. user_role join table
-- ============================================
CREATE TABLE user_role (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    granted_by BIGINT,
    CONSTRAINT uk_user_role UNIQUE (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES "user" (id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES role (id) ON DELETE CASCADE
);

-- ============================================
-- 5. role_permission join table
-- ============================================
CREATE TABLE role_permission (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    CONSTRAINT uk_role_permission UNIQUE (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES role (id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permission (id) ON DELETE CASCADE
);

-- ============================================
-- 6. repo table (Module 1)
-- ============================================
CREATE TABLE repo (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    platform VARCHAR(16) NOT NULL DEFAULT 'gitlab',
    gitlab_project_id BIGINT,
    url VARCHAR(512) NOT NULL,
    access_token_encrypted VARCHAR(512) NOT NULL,
    webhook_secret VARCHAR(128) NOT NULL DEFAULT '',
    default_branch VARCHAR(128) NOT NULL DEFAULT 'main',
    business_line VARCHAR(64),
    owner_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES "user" (id) ON DELETE SET NULL
);

CREATE INDEX idx_repo_business_line ON repo (business_line);
CREATE INDEX idx_repo_status ON repo (status);
CREATE INDEX idx_repo_gitlab_project_id ON repo (gitlab_project_id);

CREATE TRIGGER trg_repo_updated_at
    BEFORE UPDATE ON repo
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- 7. repo_branch table
-- ============================================
CREATE TABLE repo_branch (
    id BIGSERIAL PRIMARY KEY,
    repo_id BIGINT NOT NULL,
    name VARCHAR(256) NOT NULL,
    last_commit_sha VARCHAR(64),
    last_scanned_at TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    FOREIGN KEY (repo_id) REFERENCES repo (id) ON DELETE CASCADE,
    CONSTRAINT uk_repo_branch UNIQUE (repo_id, name)
);

-- ============================================
-- 8. scan_task table (Module 2)
-- ============================================
CREATE TABLE scan_task (
    id BIGSERIAL PRIMARY KEY,
    repo_id BIGINT NOT NULL,
    branch VARCHAR(256) NOT NULL DEFAULT 'main',
    commit_sha VARCHAR(64),
    status VARCHAR(20) NOT NULL DEFAULT 'queued',
    engine VARCHAR(32) NOT NULL DEFAULT 'self_sast',
    mode VARCHAR(16) NOT NULL DEFAULT 'full',
    scan_request_id VARCHAR(64),
    error_message TEXT,
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (repo_id) REFERENCES repo (id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES "user" (id) ON DELETE SET NULL
);

CREATE INDEX idx_scan_task_repo_status ON scan_task (repo_id, status);
CREATE INDEX idx_scan_task_started_at ON scan_task (started_at);
CREATE INDEX idx_scan_task_repo_commit_created ON scan_task (repo_id, commit_sha, created_at);

CREATE TRIGGER trg_scan_task_updated_at
    BEFORE UPDATE ON scan_task
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- 9. vuln_finding table (Module 2/3 output)
-- ============================================
CREATE TABLE vuln_finding (
    id BIGSERIAL PRIMARY KEY,
    scan_task_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    rule_id VARCHAR(64) NOT NULL,
    severity VARCHAR(16) NOT NULL,
    exploitability VARCHAR(32) NOT NULL DEFAULT 'potentially_exploitable',
    title VARCHAR(512) NOT NULL,
    description TEXT,
    code_snippet TEXT,
    file_path VARCHAR(1024) NOT NULL,
    line_start INT NOT NULL,
    line_end INT NOT NULL,
    cwe VARCHAR(16),
    engine VARCHAR(32) NOT NULL DEFAULT 'self_sast',
    discovered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    discovered_by VARCHAR(64),
    dedup_key VARCHAR(256),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (scan_task_id) REFERENCES scan_task (id) ON DELETE CASCADE
);

CREATE INDEX idx_vuln_scan_task ON vuln_finding (scan_task_id);
CREATE INDEX idx_vuln_project_severity ON vuln_finding (project_id, severity);
CREATE INDEX idx_vuln_severity ON vuln_finding (severity);
CREATE INDEX idx_vuln_exploitability ON vuln_finding (exploitability);
CREATE INDEX idx_vuln_dedup_key ON vuln_finding (dedup_key);

CREATE TRIGGER trg_vuln_finding_updated_at
    BEFORE UPDATE ON vuln_finding
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- 10. audit_record table (Module 3)
-- ============================================
CREATE TABLE audit_record (
    id BIGSERIAL PRIMARY KEY,
    vuln_id BIGINT NOT NULL,
    auditor_id BIGINT NOT NULL,
    action VARCHAR(32) NOT NULL,
    exploit_condition TEXT,
    poc_content TEXT,
    poc_screenshot_url VARCHAR(512),
    impact_scope VARCHAR(256),
    fix_suggestion TEXT,
    fix_code_snippet TEXT,
    audit_duration_seconds INT,
    resulting_status VARCHAR(32),
    resulting_exploitability VARCHAR(32),
    audited_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (vuln_id) REFERENCES vuln_finding (id) ON DELETE CASCADE,
    FOREIGN KEY (auditor_id) REFERENCES "user" (id) ON DELETE CASCADE
);

CREATE INDEX idx_audit_vuln_time ON audit_record (vuln_id, audited_at);

-- ============================================
-- 11. vuln_ticket table (Module 4)
-- ============================================
CREATE TABLE vuln_ticket (
    id BIGSERIAL PRIMARY KEY,
    vuln_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'pending_scan',
    severity VARCHAR(16) NOT NULL,
    assignee_id BIGINT,
    reporter_id BIGINT,
    deadline DATE,
    fixed_at TIMESTAMP,
    retest_at TIMESTAMP,
    closed_at TIMESTAMP,
    waiver_reason VARCHAR(512),
    waiver_approver_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (vuln_id) REFERENCES vuln_finding (id) ON DELETE CASCADE,
    FOREIGN KEY (assignee_id) REFERENCES "user" (id) ON DELETE SET NULL,
    FOREIGN KEY (reporter_id) REFERENCES "user" (id) ON DELETE SET NULL,
    FOREIGN KEY (waiver_approver_id) REFERENCES "user" (id) ON DELETE SET NULL
);

CREATE INDEX idx_ticket_assignee_status ON vuln_ticket (assignee_id, status);
CREATE INDEX idx_ticket_project_status ON vuln_ticket (project_id, status);
CREATE INDEX idx_ticket_vuln_id ON vuln_ticket (vuln_id);

CREATE TRIGGER trg_vuln_ticket_updated_at
    BEFORE UPDATE ON vuln_ticket
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- 12. ticket_history table (Module 4)
-- ============================================
CREATE TABLE ticket_history (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    from_status VARCHAR(32),
    to_status VARCHAR(32) NOT NULL,
    operator_id BIGINT NOT NULL,
    comment TEXT,
    operated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ticket_id) REFERENCES vuln_ticket (id) ON DELETE CASCADE,
    FOREIGN KEY (operator_id) REFERENCES "user" (id) ON DELETE CASCADE
);

CREATE INDEX idx_ticket_history_time ON ticket_history (ticket_id, operated_at);

-- ============================================
-- 13. operation_log table (RBAC audit trail)
-- ============================================
CREATE TABLE operation_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(64) NOT NULL,
    resource_type VARCHAR(32) NOT NULL,
    resource_id BIGINT,
    ip_address VARCHAR(45),
    user_agent VARCHAR(512),
    request_body TEXT,
    response_status INT,
    operated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES "user" (id) ON DELETE SET NULL
);

CREATE INDEX idx_oplog_user_time ON operation_log (user_id, operated_at);
CREATE INDEX idx_oplog_resource ON operation_log (resource_type, resource_id);

-- ============================================
-- Seed data: 4 roles + 26 permissions
-- ============================================
INSERT INTO role (id, name, description) VALUES
(1, 'SUPER_ADMIN', 'Full system access'),
(2, 'SECURITY_AUDITOR', 'Security audit and scan management'),
(3, 'PROJECT_OWNER', 'Project-level management'),
(4, 'DEVELOPER', 'Fix assigned tickets'),
(5, 'READONLY_VIEWER', 'Read-only access');

INSERT INTO permission (id, name, resource, action) VALUES
(1, 'repo:create', 'repo', 'create'),
(2, 'repo:read', 'repo', 'read'),
(3, 'repo:update', 'repo', 'update'),
(4, 'repo:delete', 'repo', 'delete'),
(5, 'scan:create', 'scan', 'create'),
(6, 'scan:read', 'scan', 'read'),
(7, 'scan:cancel', 'scan', 'cancel'),
(8, 'vuln:read', 'vuln', 'read'),
(9, 'vuln:audit', 'vuln', 'audit'),
(10, 'vuln:confirm', 'vuln', 'confirm'),
(11, 'vuln:false_positive', 'vuln', 'false_positive'),
(12, 'vuln:need_retest', 'vuln', 'need_retest'),
(13, 'ticket:read', 'ticket', 'read'),
(14, 'ticket:assign', 'ticket', 'assign'),
(15, 'ticket:fix', 'ticket', 'fix'),
(16, 'ticket:close', 'ticket', 'close'),
(17, 'ticket:retest', 'ticket', 'retest'),
(18, 'ticket:waive', 'ticket', 'waive'),
(19, 'rule:read', 'rule', 'read'),
(20, 'rule:create', 'rule', 'create'),
(21, 'rule:update', 'rule', 'update'),
(22, 'rule:delete', 'rule', 'delete'),
(23, 'rule:gray_release', 'rule', 'gray_release'),
(24, 'report:read', 'report', 'read'),
(25, 'webhook:receive', 'webhook', 'receive'),
(26, 'internal:vuln-index', 'internal', 'vuln-index');

-- SUPER_ADMIN: all 26 permissions
INSERT INTO role_permission (role_id, permission_id) VALUES
(1,1),(1,2),(1,3),(1,4),(1,5),(1,6),(1,7),(1,8),(1,9),(1,10),
(1,11),(1,12),(1,13),(1,14),(1,15),(1,16),(1,17),(1,18),(1,19),(1,20),
(1,21),(1,22),(1,23),(1,24),(1,25),(1,26);

-- SECURITY_AUDITOR: repo read, scan CRUD, vuln CRUD, ticket read, rule read, report read
INSERT INTO role_permission (role_id, permission_id) VALUES
(2,2),(2,5),(2,6),(2,7),(2,8),(2,9),(2,10),(2,11),(2,12),
(2,13),(2,19),(2,24);

-- PROJECT_OWNER: repo read (own), scan read, vuln read, ticket CRUD (own), rule read, report read (own)
INSERT INTO role_permission (role_id, permission_id) VALUES
(3,2),(3,6),(3,8),(3,13),(3,14),(3,15),(3,16),(3,17),(3,18),(3,19),(3,24);

-- DEVELOPER: repo read (assigned), scan read, vuln read, ticket read/update/fix (own), report read
INSERT INTO role_permission (role_id, permission_id) VALUES
(4,2),(4,6),(4,8),(4,13),(4,15),(4,17),(4,24);

-- READONLY_VIEWER: all read
INSERT INTO role_permission (role_id, permission_id) VALUES
(5,2),(5,6),(5,8),(5,13),(5,19),(5,24);

-- seed default admin user (password: admin123, BCrypt encoded)
INSERT INTO "user" (id, username, email, password_hash, status) VALUES
(1, 'admin', 'admin@codesec.io', '$2a$10$AfgLuTxCSwM8OE6HXDD4Lu.vyT6wvtmj/ohcIVXpTJ8kBcK.29VDq', 'active');

-- admin gets SUPER_ADMIN role
INSERT INTO user_role (user_id, role_id, granted_by) VALUES (1, 1, NULL);

-- Reset sequence to account for manual id inserts
SELECT setval('role_id_seq', (SELECT MAX(id) FROM role));
SELECT setval('permission_id_seq', (SELECT MAX(id) FROM permission));
SELECT setval('"user_id_seq"', (SELECT MAX(id) FROM "user"));
