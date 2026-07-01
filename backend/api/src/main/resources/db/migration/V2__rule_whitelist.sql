-- ============================================
-- Flyway V2: Rule whitelist/project exemption system
-- Epic: E-S3-RULE
-- Tables: rule_metadata, project_rule_exemption
-- ============================================

CREATE TABLE `rule_metadata` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `rule_id` VARCHAR(128) NOT NULL UNIQUE COMMENT 'e.g. java/sql-injection-001',
    `name` VARCHAR(256) NOT NULL,
    `severity` VARCHAR(16) NOT NULL,
    `cwe` VARCHAR(32) DEFAULT NULL,
    `language` VARCHAR(32) NOT NULL DEFAULT 'java',
    `engine` VARCHAR(32) NOT NULL DEFAULT 'self_sast',
    `detection_type` VARCHAR(16) NOT NULL DEFAULT 'ast' COMMENT 'ast | regex',
    `description` TEXT DEFAULT NULL,
    `fix_suggestion` TEXT DEFAULT NULL,
    `enabled` BOOLEAN NOT NULL DEFAULT TRUE,
    `imported_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_rule_enabled` (`enabled`),
    INDEX `idx_rule_severity` (`severity`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Rule metadata imported from engine YAML rules';

CREATE TABLE `project_rule_exemption` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `project_id` BIGINT NOT NULL,
    `rule_id` BIGINT NOT NULL,
    `reason` TEXT DEFAULT NULL COMMENT 'Exemption reason',
    `created_by` VARCHAR(64) NOT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `expires_at` TIMESTAMP NULL DEFAULT NULL COMMENT 'Optional expiration',
    UNIQUE KEY `uk_project_rule` (`project_id`, `rule_id`),
    INDEX `idx_exemption_project` (`project_id`),
    FOREIGN KEY (`rule_id`) REFERENCES `rule_metadata` (`id`) ON DELETE CASCADE,
    FOREIGN KEY (`project_id`) REFERENCES `repo` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Project-level rule exemptions/whitelist';
