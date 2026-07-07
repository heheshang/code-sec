-- ============================================
-- V9: Fix audit CASCADE → SET NULL
-- For compliance: audit records must survive user deletion
-- ============================================

-- Fix audit_record.auditor_id: CASCADE → SET NULL
DO $$
DECLARE
    fk_name TEXT;
BEGIN
    SELECT conname INTO fk_name
    FROM pg_constraint
    WHERE conrelid = 'audit_record'::regclass
      AND confrelid = '"user"'::regclass
      AND contype = 'f'
    LIMIT 1;

    IF fk_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE audit_record DROP CONSTRAINT %I', fk_name);
    END IF;
END $$;

ALTER TABLE audit_record
    ADD CONSTRAINT fk_audit_record_auditor
    FOREIGN KEY (auditor_id) REFERENCES "user" (id) ON DELETE SET NULL;

-- Fix ticket_history.operator_id: CASCADE → SET NULL
DO $$
DECLARE
    fk_name TEXT;
BEGIN
    SELECT conname INTO fk_name
    FROM pg_constraint
    WHERE conrelid = 'ticket_history'::regclass
      AND confrelid = '"user"'::regclass
      AND contype = 'f'
    LIMIT 1;

    IF fk_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE ticket_history DROP CONSTRAINT %I', fk_name);
    END IF;
END $$;

ALTER TABLE ticket_history
    ADD CONSTRAINT fk_ticket_history_operator
    FOREIGN KEY (operator_id) REFERENCES "user" (id) ON DELETE SET NULL;
