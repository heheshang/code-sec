import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { http } from '@/api/client'
import type { Vuln, VulnListQuery, PaginatedResult } from '@/types/vuln'
import { SEVERITY_ORDER } from '@/types/vuln'

interface Filters {
  projectId: string | null
  severity: Vuln['severity'][]
  status: Vuln['status'][]
  exploitability: Vuln['exploitability'][]
  keyword: string
  sortBy: 'severity' | 'discoveredAt' | 'projectId'
  sortOrder: 'asc' | 'desc'
}

const defaultFilters = (): Filters => ({
  projectId: null,
  severity: [],
  status: [],
  exploitability: [],
  keyword: '',
  sortBy: 'severity',
  sortOrder: 'asc',
})

export const useVulnStore = defineStore('vuln', () => {
  const items = ref<Vuln[]>([])
  const total = ref(0)
  const page = ref(1)
  const pageSize = ref(20)
  const loading = ref(false)
  const error = ref<string | null>(null)
  const filters = ref<Filters>(defaultFilters())
  const selectedIds = ref<Set<string>>(new Set())

  const queueCount = computed(() => {
    return items.value.filter(
      (v) => v.status === 'pending_audit' || v.status === 'pending_retest',
    ).length
  })

  const criticalCount = computed(() =>
    items.value.filter((v) => v.severity === 'critical').length,
  )

  function buildQueryString(overrides: Partial<VulnListQuery> = {}): string {
    const merged: VulnListQuery = {
      page: page.value,
      pageSize: pageSize.value,
      sortBy: filters.value.sortBy,
      sortOrder: filters.value.sortOrder,
      projectId: filters.value.projectId ?? undefined,
      severity: filters.value.severity.length > 0 ? filters.value.severity : undefined,
      status: filters.value.status.length > 0 ? filters.value.status : undefined,
      exploitability:
        filters.value.exploitability.length > 0 ? filters.value.exploitability : undefined,
      keyword: filters.value.keyword.trim() || undefined,
      ...overrides,
    }
    const sp = new URLSearchParams()
    sp.set('page', String(merged.page))
    sp.set('size', String(merged.pageSize))
    if (merged.projectId) sp.set('projectId', String(merged.projectId))
    if (merged.severity && merged.severity.length > 0) sp.set('severity', merged.severity.join(','))
    if (merged.status && merged.status.length > 0) sp.set('status', merged.status.join(','))
    if (merged.exploitability && merged.exploitability.length > 0) {
      sp.set('exploitability', merged.exploitability.join(','))
    }
    if (merged.keyword) sp.set('keyword', merged.keyword)
    return sp.toString()
  }

  async function fetchList(): Promise<void> {
    loading.value = true
    error.value = null
    try {
      const resp = await http.get<PaginatedResult<Vuln>>(`/vulns?${buildQueryString()}`)
      items.value = resp.data.items
      total.value = resp.data.total
      page.value = resp.data.page
      pageSize.value = resp.data.pageSize
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Failed to load vulnerabilities'
    } finally {
      loading.value = false
    }
  }

  async function fetchOne(vulnId: string): Promise<Vuln> {
    const resp = await http.get<Vuln>(`/vulns/${vulnId}`)
    // Patch into the cache so the table updates without a refetch
    const idx = items.value.findIndex((v) => v.id === resp.data.id)
    if (idx >= 0) {
      items.value.splice(idx, 1, resp.data)
    }
    return resp.data
  }

  function setFilters(patch: Partial<Filters>): void {
    filters.value = { ...filters.value, ...patch }
    page.value = 1
  }

  function resetFilters(): void {
    filters.value = defaultFilters()
    page.value = 1
  }

  function setPage(p: number): void {
    page.value = p
  }

  function setPageSize(s: number): void {
    pageSize.value = s
    page.value = 1
  }

  function toggleSelect(id: string): void {
    const next = new Set(selectedIds.value)
    if (next.has(id)) next.delete(id)
    else next.add(id)
    selectedIds.value = next
  }

  function clearSelection(): void {
    selectedIds.value = new Set()
  }

  function patchVuln(updated: Vuln): void {
    const idx = items.value.findIndex((v) => v.id === updated.id)
    if (idx >= 0) {
      items.value.splice(idx, 1, updated)
    }
  }

  function getById(id: string): Vuln | undefined {
    return items.value.find((v) => v.id === id)
  }

  return {
    items,
    total,
    page,
    pageSize,
    loading,
    error,
    filters,
    selectedIds,
    queueCount,
    criticalCount,
    fetchList,
    fetchOne,
    setFilters,
    resetFilters,
    setPage,
    setPageSize,
    toggleSelect,
    clearSelection,
    patchVuln,
    getById,
  }
})

// Suppress unused-import warning for SEVERITY_ORDER (kept for callers that
// need the same order constant as the store uses internally).
void SEVERITY_ORDER
