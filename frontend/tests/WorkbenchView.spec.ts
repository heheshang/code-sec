import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import Antd from 'ant-design-vue'
import WorkbenchView from '@/views/WorkbenchView.vue'
import type { Vuln } from '@/types/vuln'
import { http } from '@/api/client'

// Stub the @guolao/vue-monaco-editor wrapper to avoid loading Monaco in jsdom.
vi.mock('@guolao/vue-monaco-editor', () => ({
  Editor: {
    name: 'AMonacoEditor',
    props: ['value', 'modelValue', 'language', 'theme', 'height', 'options'],
    template: '<div class="monaco-stub" />',
  },
  loader: { config: vi.fn() },
}))

vi.mock('monaco-editor', () => ({
  editor: { setModelLanguage: (): void => undefined },
  Range: class {
    public startLineNumber: number
    public startColumn: number
    public endLineNumber: number
    public endColumn: number
    constructor(sl: number, sc: number, el: number, ec: number) {
      this.startLineNumber = sl
      this.startColumn = sc
      this.endLineNumber = el
      this.endColumn = ec
    }
  },
}))

function makeVuln(overrides: Partial<Vuln> = {}): Vuln {
  return {
    id: 'v-exploit',
    projectId: 'proj-user-service',
    ruleId: 'java/sql-injection-001',
    title: 'SQL injection',
    severity: 'critical',
    status: 'pending_audit',
    exploitability: 'EXPLOITABLE',
    exploitReason: '参数 userId 来自 @RequestParam',
    filePath: 'src/main/java/Foo.java',
    lineStart: 42,
    lineEnd: 49,
    codeSnippet: 'public class Foo {}',
    description: 'desc',
    fixSuggestion: 'fix',
    fixCodeSnippet: '// fix',
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

function mockVulnResponse(vuln: Vuln): void {
  vi.spyOn(http, 'get').mockImplementation(async (url: string) => {
    if (typeof url === 'string' && url.includes('/audits')) {
      return {
        data: { items: [], total: 0 },
        status: 200,
        statusText: 'OK',
        headers: {},
        config: {} as never,
      }
    }
    if (typeof url === 'string' && /^\/vulns\/[^/]+$/.test(url)) {
      return { data: vuln, status: 200, statusText: 'OK', headers: {}, config: {} as never }
    }
    throw new Error(`Unmocked GET ${String(url)}`)
  })
}

async function mountWorkbench(vuln: Vuln) {
  const pinia = createPinia()
  setActivePinia(pinia)
  mockVulnResponse(vuln)

  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/audit', name: 'audit', component: { template: '<div/>' } },
      { path: '/audit/:vulnId', name: 'workbench', component: WorkbenchView },
    ],
  })
  await router.push(`/audit/${vuln.id}`)
  await router.isReady()

  const wrapper = mount(WorkbenchView, {
    global: { plugins: [pinia, router, Antd] },
  })
  await flushPromises()
  await flushPromises()
  return { wrapper }
}

describe('WorkbenchView — exploitability display', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('displays the prominent exploitability badge and reason text', async () => {
    const { wrapper } = await mountWorkbench(makeVuln())
    expect(wrapper.text()).toContain('可利用')
    expect(wrapper.text()).toContain('参数 userId 来自 @RequestParam')
  })

  it('shows a muted empty-state hint when exploitReason is missing', async () => {
    const { wrapper } = await mountWorkbench(makeVuln({ exploitReason: '' }))
    expect(wrapper.text()).toContain('可利用')
    expect(wrapper.text()).toContain('未提供判定理由')
    // And the absent reason should not be rendered as a paragraph
    expect(wrapper.find('.cs-workbench__exploitReason').exists()).toBe(false)
  })

  it('renders the gold "需审计" label for POTENTIALLY_EXPLOITABLE', async () => {
    const { wrapper } = await mountWorkbench(
      makeVuln({ exploitability: 'POTENTIALLY_EXPLOITABLE', exploitReason: '需人工审计' }),
    )
    expect(wrapper.text()).toContain('需审计')
    expect(wrapper.text()).toContain('需人工审计')
  })

  it('renders the gray "不可利用" label for NOT_EXPLOITABLE', async () => {
    const { wrapper } = await mountWorkbench(
      makeVuln({ exploitability: 'NOT_EXPLOITABLE', exploitReason: '未被任何 HTTP 入口调用' }),
    )
    expect(wrapper.text()).toContain('不可利用')
    expect(wrapper.text()).toContain('未被任何 HTTP 入口调用')
  })
})
