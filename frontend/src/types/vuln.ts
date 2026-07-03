/**
 * Core vulnerability domain types.
 * Shape mirrors the union schema from architecture §2.3.
 */

export type Severity = 'critical' | 'high' | 'medium' | 'low' | 'info'

export type Exploitability = 'exploitable' | 'potentially_exploitable' | 'not_exploitable'

export type VulnStatus =
  | 'pending_scan'
  | 'pending_audit'
  | 'confirmed'
  | 'false_positive'
  | 'pending_fix'
  | 'pending_retest'
  | 'fixing'
  | 'closed'

export type Engine = 'self_sast' | 'codeql' | 'sonar' | 'dependency_check' | 'bandit' | 'gosec'

export type Language = 'java' | 'go' | 'python' | 'typescript' | 'javascript' | 'php' | 'csharp'

/**
 * Exact shape of the API `/api/v1/vulns/:id` response.
 * All fields here are guaranteed to be present in the response.
 */
export interface VulnApiResponse {
  id: number
  scanTaskId: number
  projectId: number
  ruleId: string
  title: string
  severity: Severity
  exploitability: Exploitability
  filePath: string
  lineStart: number
  lineEnd: number
  codeSnippet: string
  description: string
  cwe: string
  cve: string | null
  engine: Engine
  discoveredAt: string
  aiVerdict?: string | null
  aiConfidence?: number | null
  aiExplanation?: string | null
  aiGeneratedPatch?: string | null
}

/**
 * Frontend vulnerability model.
 * Non-API fields are optional — they are populated after audit actions or
 * may be absent from the initial API response.
 */
export interface Vuln {
  id: string
  projectId: string
  ruleId: string
  title: string
  severity: Severity
  status?: VulnStatus
  exploitability: Exploitability
  exploitReason?: string
  filePath: string
  lineStart: number
  lineEnd: number
  codeSnippet: string
  description: string
  fixSuggestion?: string
  fixCodeSnippet?: string
  fixLanguage?: Language
  cwe: string
  cve: string | null
  engine: Engine
  engines?: Engine[]
  discoveredAt: string
  discoveredBy?: string
  assignee?: string | null
  deadline?: string | null
  closedAt?: string | null
  aiVerdict?: string
  aiConfidence?: number
  aiExplanation?: string
  aiGeneratedPatch?: string
}

/** Map an API response to the frontend Vuln model (string ids, undefined defaults). */
export function vulnFromApi(r: VulnApiResponse): Vuln {
  return {
    id: String(r.id),
    projectId: String(r.projectId),
    ruleId: r.ruleId,
    title: r.title,
    severity: r.severity,
    exploitability: r.exploitability,
    filePath: r.filePath,
    lineStart: r.lineStart,
    lineEnd: r.lineEnd,
    codeSnippet: r.codeSnippet,
    description: r.description,
    cwe: r.cwe,
    cve: r.cve,
    engine: r.engine,
    discoveredAt: r.discoveredAt,
    aiVerdict: r.aiVerdict ?? undefined,
    aiConfidence: r.aiConfidence ?? undefined,
    aiExplanation: r.aiExplanation ?? undefined,
    aiGeneratedPatch: r.aiGeneratedPatch ?? undefined,
  }
}

export interface VulnListQuery {
  projectId?: string
  severity?: Severity[]
  status?: VulnStatus[]
  exploitability?: Exploitability[]
  keyword?: string
  page: number
  pageSize: number
  sortBy?: 'severity' | 'discoveredAt' | 'projectId'
  sortOrder?: 'asc' | 'desc'
}

export interface PaginatedResult<T> {
  total: number
  page: number
  pageSize?: number
  size?: number
  items: T[]
}

export const SEVERITY_ORDER: Record<Severity, number> = {
  critical: 0,
  high: 1,
  medium: 2,
  low: 3,
  info: 4,
}

export const SEVERITY_LABEL: Record<Severity, string> = {
  critical: 'Critical',
  high: 'High',
  medium: 'Medium',
  low: 'Low',
  info: 'Info',
}

export const STATUS_LABEL: Record<VulnStatus, string> = {
  pending_scan: 'Pending scan',
  pending_audit: 'Pending audit',
  confirmed: 'Confirmed',
  false_positive: 'False positive',
  pending_fix: 'Pending fix',
  pending_retest: 'Pending retest',
  fixing: 'Fixing',
  closed: 'Closed',
}

export const EXPLOITABILITY_LABEL: Record<Exploitability, string> = {
  exploitable: '可利用',
  potentially_exploitable: '需审计',
  not_exploitable: '不可利用',
}

export const LANGUAGE_TO_CM: Record<Language, string> = {
  java: 'java',
  go: 'go',
  python: 'python',
  typescript: 'typescript',
  javascript: 'javascript',
  php: 'php',
  csharp: 'csharp',
}
