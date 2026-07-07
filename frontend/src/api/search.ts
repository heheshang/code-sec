import { http } from './client'

export interface VulnSearchResult {
  id: string
  projectId: string
  ruleId: string
  severity: 'critical' | 'high' | 'medium' | 'low' | 'info'
  exploitability: 'EXPLOITABLE' | 'POTENTIALLY_EXPLOITABLE' | 'NOT_EXPLOITABLE'
  title: string
  description: string
  codeSnippet: string
  filePath: string
  cwe: string
  engine: string
  discoveredAt: string
  discoveredBy: string
}

export interface SnippetSearchResult {
  filePath: string
  projectId: string
  language: string
  codeSnippet?: string
  lineStart?: number
  indexedAt: string
}

export interface HighlightEntry {
  field: string
  fragments: string[]
}

export interface SearchResponse<T> {
  total: number
  page: number
  pageSize: number
  tookMs: number
  items: T[]
  highlights?: Record<string, string[]>
  warnings?: string[]
}

export interface SearchQuery {
  q?: string
  severity?: string[]
  exploitability?: string[]
  projectId?: string[]
  engine?: string[]
  discoveredAtFrom?: string
  discoveredAtTo?: string
  page?: number
  pageSize?: number
  sortBy?: '_score' | 'discovered_at'
  sortOrder?: 'asc' | 'desc'
}

/**
 * Search vulnerabilities via PostgreSQL full-text search.
 */
export async function searchVulns(query: SearchQuery): Promise<SearchResponse<VulnSearchResult>> {
  const params = buildParams(query)
  const resp = await http.get<SearchResponse<VulnSearchResult>>('/search/vulns', { params })
  return resp.data
}

/**
 * Search code snippets via PostgreSQL (v1: file_path prefix only).
 */
export async function searchSnippets(query: SearchQuery): Promise<SearchResponse<SnippetSearchResult>> {
  const params = buildParams(query)
  const resp = await http.get<SearchResponse<SnippetSearchResult>>('/search/snippets', { params })
  return resp.data
}

function buildParams(query: SearchQuery): Record<string, unknown> {
  const params: Record<string, unknown> = {}
  if (query.q) params.q = query.q
  if (query.severity?.length) params.severity = query.severity.join(',')
  if (query.exploitability?.length) params.exploitability = query.exploitability.join(',')
  if (query.projectId?.length) params.projectId = query.projectId.join(',')
  if (query.engine?.length) params.engine = query.engine.join(',')
  if (query.discoveredAtFrom) params.discoveredAtFrom = query.discoveredAtFrom
  if (query.discoveredAtTo) params.discoveredAtTo = query.discoveredAtTo
  if (query.page !== undefined) params.page = String(query.page)
  if (query.pageSize !== undefined) params.pageSize = String(query.pageSize)
  if (query.sortBy) params.sortBy = query.sortBy
  if (query.sortOrder) params.sortOrder = query.sortOrder
  return params
}
