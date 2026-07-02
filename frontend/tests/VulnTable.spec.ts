import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import ElementPlus from 'element-plus'
import VulnTable from '@/components/vuln/VulnTable.vue'
import { useVulnStore } from '@/stores/vuln'
import type { Vuln } from '@/types/vuln'

function makeVuln(overrides: Partial<Vuln> = {}): Vuln {
  return {
    id: 'v1',
    projectId: 'proj-user-service',
    ruleId: 'java/sql-injection-001',
    title: 'SQL injection',
    severity: 'critical',
    status: 'pending_audit',
    exploitability: 'exploitable',
    exploitReason: 'r',
    filePath: 'src/main/java/Foo.java',
    lineStart: 42,
    lineEnd: 49,
    codeSnippet: '',
    description: '',
    fixSuggestion: '',
    fixCodeSnippet: '',
    fixLanguage: 'java',
    cwe: 'CWE-89',
    cve: null,
    engine: 'self_sast',
    engines: ['self_sast'],
    discoveredAt: '2026-06-27T08:14:00Z',
    discoveredBy: 'self_sast@1.4.2',
    assignee: null,
    deadline: null,
    closedAt: null,
    ...overrides,
  }
}

async function mountTable() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const store = useVulnStore()
  store.items = [makeVuln(), makeVuln({ id: 'v2', severity: 'low', title: 'XSS' })]
  store.total = 2
  store.page = 1
  store.pageSize = 20

  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/audit', name: 'audit', component: { template: '<div/>' } },
      { path: '/audit/:vulnId', name: 'workbench', component: { template: '<div/>' } },
    ],
  })

  const wrapper = mount(VulnTable, {
    global: { plugins: [pinia, router, ElementPlus] },
  })
  await flushPromises()
  return { wrapper, store, router }
}

describe('VulnTable', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders one row per vuln in the store', async () => {
    const { wrapper } = await mountTable()
    const rows = wrapper.findAll('.el-table__row')
    expect(rows).toHaveLength(2)
  })

  it('exposes the total count for pagination', async () => {
    const { store } = await mountTable()
    expect(store.total).toBe(2)
  })

  it('updates the store page when pagination changes', async () => {
    const { wrapper, store } = await mountTable()
    const pagination = wrapper.findComponent({ name: 'ElPagination' })
    await pagination.vm.$emit('current-change', 2)
    expect(store.page).toBe(2)
  })

  it('toggles selection state when a row is selected', async () => {
    const { wrapper, store } = await mountTable()
    store.toggleSelect('v1')
    expect(store.selectedIds.has('v1')).toBe(true)
    store.toggleSelect('v1')
    expect(store.selectedIds.has('v1')).toBe(false)
  })

  it('renders exploitability column with badge for each row', async () => {
    const { wrapper, store } = await mountTable()
    store.items = [
      makeVuln({ id: 'a', exploitability: 'exploitable', exploitReason: 'r1' }),
      makeVuln({ id: 'b', exploitability: 'potentially_exploitable', exploitReason: 'r2' }),
      makeVuln({ id: 'c', exploitability: 'not_exploitable', exploitReason: 'r3' }),
    ]
    await flushPromises()
    const text = wrapper.text()
    expect(text).toContain('可利用')
    expect(text).toContain('需审计')
    expect(text).toContain('不可利用')
  })

  it('filters by exploitability state when store filter is set', async () => {
    const { store } = await mountTable()
    store.items = [
      makeVuln({ id: 'a', exploitability: 'exploitable' }),
      makeVuln({ id: 'b', exploitability: 'potentially_exploitable' }),
      makeVuln({ id: 'c', exploitability: 'not_exploitable' }),
    ]
    store.setFilters({ exploitability: ['exploitable', 'not_exploitable'] })
    expect(store.filters.exploitability).toEqual(['exploitable', 'not_exploitable'])
    expect(store.filters.exploitability).not.toContain('potentially_exploitable')
  })
})
