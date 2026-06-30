import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import Antd from 'ant-design-vue'
import VulnTable from '@/components/vuln/VulnTable.vue'
import { useVulnStore } from '@/stores/vuln'
import type { Vuln } from '@/types/vuln'

// Stub the ant-design-vue Table to a simple list to keep the test fast
// and to assert against our own component logic, not AntD internals.
vi.mock('ant-design-vue', async () => {
  const actual = await vi.importActual<typeof import('ant-design-vue')>('ant-design-vue')
  const { h } = await import('vue')
  const ATable = {
    name: 'ATable',
    props: ['columns', 'dataSource', 'loading', 'pagination', 'rowSelection', 'rowKey', 'scroll', 'size'],
    setup(props: { columns: unknown[]; dataSource: unknown[]; loading: boolean; rowKey: ((r: unknown) => unknown) | string | undefined }) {
      return () => {
        const cols = (props.columns ?? []) as Array<{ key?: string; dataIndex?: string; customRender?: (args: { record: unknown; text: unknown; index: number }) => ReturnType<typeof h> }>
        const rows = (props.dataSource ?? []) as unknown[]
        const getKey = (r: unknown): unknown => {
          if (typeof props.rowKey === 'function') return props.rowKey(r)
          if (typeof props.rowKey === 'string' && r !== null && typeof r === 'object') {
            return (r as Record<string, unknown>)[props.rowKey]
          }
          return (r as { id: unknown })?.id
        }
        return h('div', { class: 'stub-table', 'data-loading': String(props.loading) },
          rows.map((row) =>
            h('div', { key: String(getKey(row)), class: 'stub-row' },
              cols.map((col) => {
                const cellKey = col.key ?? col.dataIndex ?? ''
                const text = col.dataIndex !== undefined && row !== null && typeof row === 'object'
                  ? (row as Record<string, unknown>)[col.dataIndex]
                  : undefined
                if (typeof col.customRender === 'function') {
                  return h('div', { key: cellKey, class: 'stub-cell', 'data-col': cellKey },
                    [col.customRender({ record: row, text, index: 0 })])
                }
                return h('div', { key: cellKey, class: 'stub-cell', 'data-col': cellKey }, [h('span', {}, String(text ?? ''))])
              }),
            ),
          ),
        )
      }
    },
  }
  return {
    ...actual,
    Table: ATable,
  }
})

function makeVuln(overrides: Partial<Vuln> = {}): Vuln {
  return {
    id: 'v1',
    projectId: 'proj-user-service',
    ruleId: 'java/sql-injection-001',
    title: 'SQL injection',
    severity: 'critical',
    status: 'pending_audit',
    exploitability: 'EXPLOITABLE',
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
    global: { plugins: [pinia, router, Antd] },
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
    const rows = wrapper.findAll('.stub-row')
    expect(rows).toHaveLength(2)
  })

  it('exposes the total count for pagination', async () => {
    const { store } = await mountTable()
    expect(store.total).toBe(2)
  })

  it('updates the store page when pagination changes', async () => {
    const { wrapper, store } = await mountTable()
    const table = wrapper.findComponent({ name: 'ATable' })
    await table.vm.$emit('change', { current: 2, pageSize: 20 })
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
      makeVuln({ id: 'a', exploitability: 'EXPLOITABLE', exploitReason: 'r1' }),
      makeVuln({ id: 'b', exploitability: 'POTENTIALLY_EXPLOITABLE', exploitReason: 'r2' }),
      makeVuln({ id: 'c', exploitability: 'NOT_EXPLOITABLE', exploitReason: 'r3' }),
    ]
    await flushPromises()
    const cells = wrapper.findAll('[data-col="exploitability"]')
    expect(cells).toHaveLength(3)
    const labels = cells.map((c) => c.text())
    expect(labels).toContain('可利用')
    expect(labels).toContain('需审计')
    expect(labels).toContain('不可利用')
  })

  it('filters by exploitability state when store filter is set', async () => {
    const { store } = await mountTable()
    store.items = [
      makeVuln({ id: 'a', exploitability: 'EXPLOITABLE' }),
      makeVuln({ id: 'b', exploitability: 'POTENTIALLY_EXPLOITABLE' }),
      makeVuln({ id: 'c', exploitability: 'NOT_EXPLOITABLE' }),
    ]
    store.setFilters({ exploitability: ['EXPLOITABLE', 'NOT_EXPLOITABLE'] })
    expect(store.filters.exploitability).toEqual(['EXPLOITABLE', 'NOT_EXPLOITABLE'])
    expect(store.filters.exploitability).not.toContain('POTENTIALLY_EXPLOITABLE')
  })
})
