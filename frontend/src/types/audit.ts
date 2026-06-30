import type { VulnStatus, Exploitability } from './vuln'

export type AuditAction = 'confirm' | 'false_positive' | 'need_retest'

export const AUDIT_ACTION_LABEL: Record<AuditAction, string> = {
  confirm: 'Confirm vulnerability',
  false_positive: 'Mark as false positive',
  need_retest: 'Request retest',
}

export const AUDIT_ACTION_DESCRIPTION: Record<AuditAction, string> = {
  confirm: 'The finding is real and must be remediated. Write the exploit path and standard fix.',
  false_positive: 'The engine flagged this incorrectly. Justify with a code-level reason.',
  need_retest: 'The fix is partial or the runtime behavior is unclear. Add a verification plan.',
}

export interface PocAttachment {
  id: string
  name: string
  type: 'image' | 'url' | 'script'
  preview: string
}

export interface AuditRecord {
  id: string
  vulnId: string
  auditorId: string
  auditorName: string
  action: AuditAction
  exploitCondition: string
  pocContent: string
  pocAttachments: PocAttachment[]
  impactScope: string
  businessScenario: string
  fixSuggestion: string
  fixCodeSnippet: string
  fixLanguage: string
  resultingStatus: VulnStatus
  resultingExploitability: Exploitability | null
  auditDurationSeconds: number
  auditedAt: string
}

export interface AuditSubmitPayload {
  vulnId: string
  action: AuditAction
  exploitCondition: string
  pocContent: string
  pocAttachments: PocAttachment[]
  impactScope: string
  businessScenario: string
  fixSuggestion: string
  fixCodeSnippet: string
  fixLanguage: string
}
