import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { searchVulns, searchSnippets, type SearchQuery, type VulnSearchResult, type SnippetSearchResult, type SearchResponse } from '@/api/search'

export type SearchTab = 'vulns' | 'snippets'

export const useSearchStore = defineStore('search', () => {
  const query = ref('')
  const activeTab = ref<SearchTab>('vulns')

  // Vuln results
  const vulnResults = ref<VulnSearchResult[]>([])
  const vulnTotal = ref(0)
  const vulnHighlights = ref<Record<string, string[]>>({})
  const vulnTookMs = ref(0)

  // Snippet results
  const snippetResults = ref<SnippetSearchResult[]>([])
  const snippetTotal = ref(0)

  // Shared
  const loading = ref(false)
  const error = ref<string | null>(null)
  const page = ref(1)
  const pageSize = ref(20)

  // Filters
  const severityFilter = ref<string[]>([])
  const exploitabilityFilter = ref<string[]>([])
  const projectFilter = ref<string[]>([])
  const engineFilter = ref<string[]>([])
  const sortBy = ref<'_score' | 'discovered_at'>('_score')
  const sortOrder = ref<'asc' | 'desc'>('desc')

  const hasActiveFilters = computed(() =>
    severityFilter.value.length > 0 ||
    exploitabilityFilter.value.length > 0 ||
    projectFilter.value.length > 0 ||
    engineFilter.value.length > 0
  )

  function buildQuery(overrides: Partial<SearchQuery> = {}): SearchQuery {
    return {
      q: query.value || undefined,
      severity: severityFilter.value.length > 0 ? severityFilter.value : undefined,
      exploitability: exploitabilityFilter.value.length > 0 ? exploitabilityFilter.value : undefined,
      projectId: projectFilter.value.length > 0 ? projectFilter.value : undefined,
      engine: engineFilter.value.length > 0 ? engineFilter.value : undefined,
      page: page.value,
      pageSize: pageSize.value,
      sortBy: sortBy.value,
      sortOrder: sortOrder.value,
      ...overrides,
    }
  }

  async function search(tab?: SearchTab): Promise<void> {
    const target = tab ?? activeTab.value
    if (!query.value && !hasActiveFilters.value) {
      if (target === 'vulns') {
        vulnResults.value = []
        vulnTotal.value = 0
        vulnHighlights.value = {}
        vulnTookMs.value = 0
      } else {
        snippetResults.value = []
        snippetTotal.value = 0
      }
      return
    }

    loading.value = true
    error.value = null

    try {
      if (target === 'vulns') {
        const resp = await searchVulns(buildQuery())
        vulnResults.value = resp.items
        vulnTotal.value = resp.total
        vulnHighlights.value = resp.highlights ?? {}
        vulnTookMs.value = resp.tookMs
      } else {
        const resp = await searchSnippets(buildQuery())
        snippetResults.value = resp.items
        snippetTotal.value = resp.total
      }
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Search failed'
    } finally {
      loading.value = false
    }
  }

  function setQuery(q: string): void {
    query.value = q
    page.value = 1
  }

  function setTab(tab: SearchTab): void {
    activeTab.value = tab
    page.value = 1
  }

  function setPage(p: number): void {
    page.value = p
  }

  function setPageSize(s: number): void {
    pageSize.value = s
    page.value = 1
  }

  function clearFilters(): void {
    severityFilter.value = []
    exploitabilityFilter.value = []
    projectFilter.value = []
    engineFilter.value = []
    page.value = 1
  }

  return {
    query,
    activeTab,
    vulnResults,
    vulnTotal,
    vulnHighlights,
    vulnTookMs,
    snippetResults,
    snippetTotal,
    loading,
    error,
    page,
    pageSize,
    severityFilter,
    exploitabilityFilter,
    projectFilter,
    engineFilter,
    sortBy,
    sortOrder,
    hasActiveFilters,
    search,
    setQuery,
    setTab,
    setPage,
    setPageSize,
    clearFilters,
  }
})
