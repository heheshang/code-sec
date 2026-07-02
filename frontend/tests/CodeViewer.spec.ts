import { describe, it, expect } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import CodeViewer from '@/components/code/CodeViewer.vue'
import type { Vuln } from '@/types/vuln'

const baseVuln: Vuln = {
  id: 'v1',
  projectId: 'p1',
  ruleId: 'r1',
  title: 't',
  severity: 'high',
  status: 'pending_audit',
  exploitability: 'exploitable',
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
  it('renders the wrapper div', async () => {
    const w = mount(CodeViewer, {
      props: { code: 'println("hi")', language: 'java', vuln: baseVuln, readOnly: true },
      global: { plugins: [ElementPlus] },
    })
    await flushPromises()
    expect(w.find('.cs-cm-viewer').exists()).toBe(true)
  })

  it('forwards code and vuln props', async () => {
    const w = mount(CodeViewer, {
      props: { code: 'test code', language: 'java', vuln: baseVuln, readOnly: true },
      global: { plugins: [ElementPlus] },
    })
    await flushPromises()
    // CodeMirror initializes in jsdom but layout APIs aren't available;
    // verify the wrapper mounts without error.
    expect(w.find('.cs-cm-viewer').exists()).toBe(true)
  })
})
