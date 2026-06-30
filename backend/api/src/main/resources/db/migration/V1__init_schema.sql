-- ============================================
-- Flyway V1: Core schema initialization
-- Epic: E-S2-CRITICAL Backend Business Services
-- 10 tables for code security audit platform
-- ============================================

-- 1. user table (RBAC)
CREATE TABLE `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(64) NOT NULL,
    `email` VARCHAR(128) NULL,
    `password_hash` VARCHAR(256) NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'active',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `last_login_at` TIMESTAMP NULL,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. role table (RBAC)
CREATE TABLE `role` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(32) NOT NULL,
    `description` VARCHAR(256) NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. permission table (RBAC)
CREATE TABLE `permission` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(64) NOT NULL,
    `resource` VARCHAR(32) NOT NULL,
    `action` VARCHAR(32) NOT NULL,
    UNIQUE KEY `uk_name` (`name`),
    UNIQUE KEY `uk_resource_action` (`resource`, `action`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. user_role join table
CREATE TABLE `user_role` (
    `user_id` BIGINT NOT NULL,
    `role_id` BIGINT NOT NULL,
    `granted_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `granted_by` BIGINT NULL,
    PRIMARY KEY (`user_id`, `role_id`),
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. role_permission join table
CREATE TABLE `role_permission` (
    `role_id` BIGINT NOT NULL,
    `permission_id` BIGINT NOT NULL,
    PRIMARY KEY (`role_id`, `permission_id`),
    FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON DELETE CASCADE,
    FOREIGN KEY (`permission_id`) REFERENCES `permission` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. repo table (Module 1)
CREATE TABLE `repo` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(128) NOT NULL,
    `platform` VARCHAR(16) NOT NULL DEFAULT 'gitlab',
    `gitlab_project_id` BIGINT NULL,
    `url` VARCHAR(512) NOT NULL,
    `access_token_encrypted` VARCHAR(512) NOT NULL,
    `webhook_secret` VARCHAR(128) NOT NULL DEFAULT '',
    `default_branch` VARCHAR(128) NOT NULL DEFAULT 'main',
    `business_line` VARCHAR(64) NULL,
    `owner_id` BIGINT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'active',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`owner_id`) REFERENCES `user` (`id`) ON DELETE SET NULL,
    INDEX `idx_business_line` (`business_line`),
    INDEX `idx_status` (`status`),
    INDEX `idx_gitlab_project_id` (`gitlab_project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. repo_branch table
CREATE TABLE `repo_branch` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `repo_id` BIGINT NOT NULL,
    `name` VARCHAR(256) NOT NULL,
    `last_commit_sha` VARCHAR(64) NULL,
    `last_scanned_at` TIMESTAMP NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'active',
    FOREIGN KEY (`repo_id`) REFERENCES `repo` (`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_repo_branch` (`repo_id`, `name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. scan_task table (Module 2)
CREATE TABLE `scan_task` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `repo_id` BIGINT NOT NULL,
    `branch` VARCHAR(256) NOT NULL DEFAULT 'main',
    `commit_sha` VARCHAR(64) NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'queued',
    `engine` VARCHAR(32) NOT NULL DEFAULT 'self_sast',
    `mode` VARCHAR(16) NOT NULL DEFAULT 'full',
    `scan_request_id` VARCHAR(64) NULL,
    `error_message` TEXT NULL,
    `started_at` TIMESTAMP NULL,
    `finished_at` TIMESTAMP NULL,
    `created_by` BIGINT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`repo_id`) REFERENCES `repo` (`id`) ON DELETE CASCADE,
    FOREIGN KEY (`created_by`) REFERENCES `user` (`id`) ON DELETE SET NULL,
    INDEX `idx_repo_status` (`repo_id`, `status`),
    INDEX `idx_started_at` (`started_at`),
    INDEX `idx_repo_commit_created` (`repo_id`, `commit_sha`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 9. vuln_finding table (Module 2/3 output)
CREATE TABLE `vuln_finding` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `scan_task_id` BIGINT NOT NULL,
    `project_id` BIGINT NOT NULL COMMENT 'Redundant: repo.gitlab_project_id, for ES index alignment',
    `rule_id` VARCHAR(64) NOT NULL,
    `severity` VARCHAR(16) NOT NULL,
    `exploitability` VARCHAR(32) NOT NULL DEFAULT 'potentially_exploitable',
    `title` VARCHAR(512) NOT NULL,
    `description` TEXT NULL,
    `code_snippet` MEDIUMTEXT NULL,
    `file_path` VARCHAR(1024) NOT NULL,
    `line_start` INT NOT NULL,
    `line_end` INT NOT NULL,
    `cwe` VARCHAR(16) NULL,
    `engine` VARCHAR(32) NOT NULL DEFAULT 'self_sast',
    `discovered_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `discovered_by` VARCHAR(64) NULL,
    `dedup_key` VARCHAR(256) NULL COMMENT 'Composite: scan_id/file_path/line_start/rule_id',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`scan_task_id`) REFERENCES `scan_task` (`id`) ON DELETE CASCADE,
    INDEX `idx_scan_task` (`scan_task_id`),
    INDEX `idx_project_severity` (`project_id`, `severity`),
    INDEX `idx_severity` (`severity`),
    INDEX `idx_exploitability` (`exploitability`),
    INDEX `idx_dedup_key` (`dedup_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 10. audit_record table (Module 3)
CREATE TABLE `audit_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `vuln_id` BIGINT NOT NULL,
    `auditor_id` BIGINT NOT NULL,
    `action` VARCHAR(32) NOT NULL,
    `exploit_condition` TEXT NULL,
    `poc_content` TEXT NULL,
    `poc_screenshot_url` VARCHAR(512) NULL,
    `impact_scope` VARCHAR(256) NULL,
    `fix_suggestion` TEXT NULL,
    `fix_code_snippet` MEDIUMTEXT NULL,
    `audit_duration_seconds` INT NULL,
    `resulting_status` VARCHAR(32) NULL,
    `resulting_exploitability` VARCHAR(32) NULL,
    `audited_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`vuln_id`) REFERENCES `vuln_finding` (`id`) ON DELETE CASCADE,
    FOREIGN KEY (`auditor_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    INDEX `idx_vuln_time` (`vuln_id`, `audited_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 11. vuln_ticket table (Module 4)
CREATE TABLE `vuln_ticket` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `vuln_id` BIGINT NOT NULL,
    `project_id` BIGINT NOT NULL COMMENT 'Redundant: for project-level queries without cross-JOIN',
    `status` VARCHAR(32) NOT NULL DEFAULT 'pending_scan',
    `severity` VARCHAR(16) NOT NULL,
    `assignee_id` BIGINT NULL,
    `reporter_id` BIGINT NULL,
    `deadline` DATE NULL,
    `fixed_at` TIMESTAMP NULL,
    `retest_at` TIMESTAMP NULL,
    `closed_at` TIMESTAMP NULL,
    `waiver_reason` VARCHAR(512) NULL,
    `waiver_approver_id` BIGINT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`vuln_id`) REFERENCES `vuln_finding` (`id`) ON DELETE CASCADE,
    FOREIGN KEY (`assignee_id`) REFERENCES `user` (`id`) ON DELETE SET NULL,
    FOREIGN KEY (`reporter_id`) REFERENCES `user` (`id`) ON DELETE SET NULL,
    FOREIGN KEY (`waiver_approver_id`) REFERENCES `user` (`id`) ON DELETE SET NULL,
    INDEX `idx_assignee_status` (`assignee_id`, `status`),
    INDEX `idx_project_status` (`project_id`, `status`),
    INDEX `idx_vuln_id` (`vuln_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 12. ticket_history table (Module 4)
CREATE TABLE `ticket_history` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `ticket_id` BIGINT NOT NULL,
    `from_status` VARCHAR(32) NULL,
    `to_status` VARCHAR(32) NOT NULL,
    `operator_id` BIGINT NOT NULL,
    `comment` TEXT NULL,
    `operated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`ticket_id`) REFERENCES `vuln_ticket` (`id`) ON DELETE CASCADE,
    FOREIGN KEY (`operator_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    INDEX `idx_ticket_time` (`ticket_id`, `operated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 13. operation_log table (RBAC audit trail)
CREATE TABLE `operation_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NULL,
    `action` VARCHAR(64) NOT NULL,
    `resource_type` VARCHAR(32) NOT NULL,
    `resource_id` BIGINT NULL,
    `ip_address` VARCHAR(45) NULL,
    `user_agent` VARCHAR(512) NULL,
    `request_body` TEXT NULL,
    `response_status` INT NULL,
    `operated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE SET NULL,
    INDEX `idx_user_time` (`user_id`, `operated_at`),
    INDEX `idx_resource` (`resource_type`, `resource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- seed data: 4 roles + 26 permissions
INSERT INTO `role` (`id`, `name`, `description`) VALUES
(1, 'SUPER_ADMIN', 'Full system access'),
(2, 'SECURITY_AUDITOR', 'Security audit and scan management'),
(3, 'PROJECT_OWNER', 'Project-level management'),
(4, 'DEVELOPER', 'Fix assigned tickets'),
(5, 'READONLY_VIEWER', 'Read-only access');

INSERT INTO `permission` (`id`, `name`, `resource`, `action`) VALUES
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
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES
(1,1),(1,2),(1,3),(1,4),(1,5),(1,6),(1,7),(1,8),(1,9),(1,10),
(1,11),(1,12),(1,13),(1,14),(1,15),(1,16),(1,17),(1,18),(1,19),(1,20),
(1,21),(1,22),(1,23),(1,24),(1,25),(1,26);

-- SECURITY_AUDITOR: repo read, scan CRUD, vuln CRUD, ticket read, rule read, report read
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES
(2,2),(2,5),(2,6),(2,7),(2,8),(2,9),(2,10),(2,11),(2,12),
(2,13),(2,19),(2,24);

-- PROJECT_OWNER: repo read (own), scan read, vuln read, ticket CRUD (own), rule read, report read (own)
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES
(3,2),(3,6),(3,8),(3,13),(3,14),(3,15),(3,16),(3,17),(3,18),(3,19),(3,24);

-- DEVELOPER: repo read (assigned), scan read, vuln read, ticket read/update/fix (own), report read
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES
(4,2),(4,6),(4,8),(4,13),(4,15),(4,17),(4,24);

-- READONLY_VIEWER: all read
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES
(5,2),(5,6),(5,8),(5,13),(5,19),(5,24);

-- seed default admin user (password: admin123, BCrypt encoded)
INSERT INTO `user` (`id`, `username`, `email`, `password_hash`, `status`) VALUES
(1, 'admin', 'admin@codesec.io', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'active');

-- admin gets SUPER_ADMIN role
INSERT INTO `user_role` (`user_id`, `role_id`, `granted_by`) VALUES (1, 1, NULL);
