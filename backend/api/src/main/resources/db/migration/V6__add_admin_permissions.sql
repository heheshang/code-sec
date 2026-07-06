-- ============================================
-- Flyway V6: Add admin/visibility permissions
-- Adds admin:crypto, admin:benchmark, dashboard:read, cpg:read
-- assigned to SUPER_ADMIN (role_id=1)
-- ============================================

INSERT INTO permission (id, name, resource, action) VALUES
    (27, 'admin:crypto',     'admin',     'crypto'),
    (28, 'admin:benchmark',  'admin',     'benchmark'),
    (29, 'dashboard:read',   'dashboard', 'read'),
    (30, 'cpg:read',         'cpg',       'read');

-- SUPER_ADMIN gets all 4 new permissions
INSERT INTO role_permission (role_id, permission_id) VALUES
    (1, 27), (1, 28), (1, 29), (1, 30);

-- Reset permission sequence to account for manual id inserts
SELECT setval('permission_id_seq', (SELECT MAX(id) FROM permission));
