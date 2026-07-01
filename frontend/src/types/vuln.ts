/**
 * Core vulnerability domain types.
 * Shape mirrors the union schema from architecture §2.3.
 */

export type Severity = 'critical' | 'high' | 'medium' | 'low' | 'info'

export type Exploitability = 'EXPLOITABLE' | 'POTENTIALLY_EXPLOITABLE' | 'NOT_EXPLOITABLE'

export type VulnStatus =
  | 'pending_audit'
  | 'confirmed'
  | 'false_positive'
  | 'pending_retest'
  | 'fixing'
  | 'closed'

export type Engine = 'self_sast' | 'codeql' | 'sonar' | 'dependency_check' | 'bandit' | 'gosec'

export type Language = 'java' | 'go' | 'python' | 'typescript' | 'javascript' | 'php' | 'csharp'

export interface Vuln {
  id: string
  projectId: string
  ruleId: string
  title: string
  severity: Severity
  status: VulnStatus
  exploitability: Exploitability
  exploitReason: string
  filePath: string
  lineStart: number
  lineEnd: number
  codeSnippet: string
  description: string
  fixSuggestion: string
  fixCodeSnippet: string
  fixLanguage: Language
  cwe: string
  cve: string | null
  engine: Engine
  engines: Engine[]
  discoveredAt: string
  discoveredBy: string
  assignee: string | null
  deadline: string | null
  closedAt: string | null
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
  pending_audit: 'Pending audit',
  confirmed: 'Confirmed',
  false_positive: 'False positive',
  pending_retest: 'Pending retest',
  fixing: 'Fixing',
  closed: 'Closed',
}

export const EXPLOITABILITY_LABEL: Record<Exploitability, string> = {
  EXPLOITABLE: '可利用',
  POTENTIALLY_EXPLOITABLE: '需审计',
  NOT_EXPLOITABLE: '不可利用',
}

export const LANGUAGE_TO_MONACO: Record<Language, string> = {
  java: 'java',
  go: 'go',
  python: 'python',
  typescript: 'typescript',
  javascript: 'javascript',
  php: 'php',
  csharp: 'csharp',
}
