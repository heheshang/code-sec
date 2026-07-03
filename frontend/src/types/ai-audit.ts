export type AiVerdict = 'exploitable' | 'false_positive' | 'suspicious'

export const AI_VERDICT_LABEL: Record<AiVerdict, string> = {
  exploitable: 'Exploitable',
  false_positive: 'False Positive',
  suspicious: 'Suspicious',
}

export const AI_VERDICT_COLOR: Record<AiVerdict, string> = {
  exploitable: 'var(--cs-severity-critical)',
  false_positive: 'var(--cs-status-fp)',
  suspicious: 'var(--cs-status-retest)',
}

export interface AiAnalysisResult {
  vulnId: string
  aiVerdict: AiVerdict
  aiConfidence: number
  aiExplanation: string
  aiGeneratedPatch?: string
  analyzedAt: string
  modelVersion: string
  durationMs: number
  fallbackLevel: string
}

export interface PipelineStageInfo {
  name: string
  success: boolean
}

export interface AiAuditProgress {
  vulnId: string
  status: 'queued' | 'analyzing' | 'completed' | 'failed'
  progress?: number
}

export type LogicVulnType =
  | 'BusinessLogicBypass'
  | 'AuthFlaw'
  | 'InputValidationError'
  | 'StateMachineViolation'
  | 'CryptoLogicError'
  | 'RaceCondition'

export const LOGIC_VULN_TYPE_LABEL: Record<LogicVulnType, string> = {
  BusinessLogicBypass: 'Business Logic Bypass',
  AuthFlaw: 'Auth / Authorization Flaw',
  InputValidationError: 'Input Validation Error',
  StateMachineViolation: 'State Machine Violation',
  CryptoLogicError: 'Cryptographic Logic Error',
  RaceCondition: 'Race Condition / TOCTOU',
}

export const LOGIC_VULN_COLOR: Record<string, string> = {
  CRITICAL: 'var(--cs-severity-critical)',
  HIGH: 'var(--cs-severity-high)',
  MEDIUM: 'var(--cs-severity-medium)',
  LOW: 'var(--cs-severity-low)',
}

export interface LogicVulnResult {
  vulnType: string
  evidenceChain: string[]
  exploitCondition: string
  riskLevel: string
  recommendedFix: string
  codeSnippet: string
  lineStart: number
  lineEnd: number
}

export type PocPatchStatus = 'PASS' | 'FAIL' | 'PENDING'

export const POC_PATCH_STATUS_LABEL: Record<PocPatchStatus, string> = {
  PASS: 'Verified',
  FAIL: 'Failed',
  PENDING: 'Pending',
}

export const POC_PATCH_STATUS_TYPE: Record<PocPatchStatus, 'success' | 'danger' | 'warning'> = {
  PASS: 'success',
  FAIL: 'danger',
  PENDING: 'warning',
}
