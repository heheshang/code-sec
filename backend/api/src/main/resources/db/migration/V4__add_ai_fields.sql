ALTER TABLE vuln_finding
    ADD COLUMN ai_verdict VARCHAR(32),
    ADD COLUMN ai_confidence DOUBLE PRECISION,
    ADD COLUMN ai_explanation TEXT,
    ADD COLUMN ai_generated_patch TEXT;

COMMENT ON COLUMN vuln_finding.ai_verdict IS 'AI 判定结论: exploitable / false_positive / suspicious';
COMMENT ON COLUMN vuln_finding.ai_confidence IS 'AI 判定置信度 (0.0 ~ 1.0)';
COMMENT ON COLUMN vuln_finding.ai_explanation IS 'AI 判定解释';
COMMENT ON COLUMN vuln_finding.ai_generated_patch IS 'AI 生成的修复代码片段';
