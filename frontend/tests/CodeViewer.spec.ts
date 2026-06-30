import { describe, it, expect, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Antd from 'ant-design-vue'
import CodeViewer from '@/components/code/CodeViewer.vue'
import type { Vuln } from '@/types/vuln'

// Monaco pulls in a huge amount of browser-only code; stub it for the unit
// test so we can verify the wrapper mounts and forwards props.
vi.mock('@guolao/vue-monaco-editor', () => ({
  Editor: {
    name: 'AMonacoEditor',
    props: ['value', 'modelValue', 'language', 'theme', 'height', 'options'],
    template: '<div class="stub-monaco" :data-lang="language" :data-value="value" :data-readonly="options && options.readOnly" />',
  },
  loader: { config: vi.fn() },
}))

vi.mock('monaco-editor', () => {
  const noop = () => undefined
  class FakeRange {
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
  }
  return {
    Range: FakeRange,
    editor: {
      setModelLanguage: noop,
    },
  }
})

const baseVuln: Vuln = {
  id: 'v1',
  projectId: 'p1',
  ruleId: 'r1',
  title: 't',
  severity: 'high',
  status: 'pending_audit',
  exploitability: 'EXPLOITABLE',
  exploitReason: 'r',
  filePath: 'f',
  lineStart: 1,
  lineEnd: 2,
  codeSnippet: 'public class A {}',
  description: '',
  fixSuggestion: '',
  fixCodeSnippet: '',
  fixLanguage: 'java',
  cwe: 'CWE-89',
  cve: null,
  engine: 'self_sast',
  engines: ['self_sast'],
  discoveredAt: '',
  discoveredBy: '',
  assignee: null,
  deadline: null,
  closedAt: null,
}

describe('CodeViewer', () => {
  it('forwards code, language, and readOnly to the Monaco wrapper', async () => {
    const w = mount(CodeViewer, {
      props: { code: 'println("hi")', language: 'java', vuln: baseVuln, readOnly: true },
      global: { plugins: [Antd] },
    })
    await flushPromises()
    const stub = w.find('.stub-monaco')
    expect(stub.exists()).toBe(true)
    expect(stub.attributes('data-lang')).toBe('java')
    expect(stub.attributes('data-value')).toBe('println("hi")')
    expect(stub.attributes('data-readonly')).toBe('true')
  })

  it('maps the Language type to the correct Monaco identifier', async () => {
    const w = mount(CodeViewer, {
      props: { code: '', language: 'go', vuln: null, readOnly: true },
      global: { plugins: [Antd] },
    })
    await flushPromises()
    expect(w.find('.stub-monaco').attributes('data-lang')).toBe('go')
  })

  it('renders without a vuln in standalone read mode', async () => {
    const w = mount(CodeViewer, {
      props: { code: 'x', language: 'python', vuln: null, readOnly: true },
      global: { plugins: [Antd] },
    })
    await flushPromises()
    expect(w.find('.cs-code-viewer').exists()).toBe(true)
  })
})
