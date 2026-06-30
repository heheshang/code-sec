<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, ref, watch } from 'vue'
import * as monaco from 'monaco-editor'
import { Editor as MonacoEditor } from '@guolao/vue-monaco-editor'
import type { Vuln, Language } from '@/types/vuln'
import { LANGUAGE_TO_MONACO } from '@/types/vuln'

interface Props {
  code: string
  language: Language
  vuln: Vuln | null
  readOnly?: boolean
  height?: string
  showLineNumbers?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  readOnly: true,
  height: '100%',
  showLineNumbers: true,
})

const editorRef = ref<monaco.editor.IStandaloneCodeEditor | null>(null)
let markerDecorations: string[] = []
let currentLang: string = 'java'

const monacoLang = computed<string>(() => LANGUAGE_TO_MONACO[props.language])

const editorOptions = computed<monaco.editor.IStandaloneEditorConstructionOptions>(() => ({
  readOnly: props.readOnly,
  fontSize: 13,
  fontFamily: 'JetBrains Mono, SF Mono, Menlo, Consolas, monospace',
  fontLigatures: true,
  lineNumbers: props.showLineNumbers ? 'on' : 'off',
  minimap: { enabled: false },
  scrollBeyondLastLine: false,
  renderLineHighlight: 'all',
  smoothScrolling: true,
  cursorBlinking: 'smooth',
  cursorSmoothCaretAnimation: 'on',
  scrollbar: {
    verticalScrollbarSize: 8,
    horizontalScrollbarSize: 8,
  },
  folding: true,
  guides: { indentation: true, highlightActiveIndentation: true },
  padding: { top: 12, bottom: 12 },
  automaticLayout: true,
  renderWhitespace: 'selection',
  wordWrap: 'off',
  contextmenu: false,
  quickSuggestions: false,
  suggestOnTriggerCharacters: false,
  parameterHints: { enabled: false },
  snippetSuggestions: 'none',
  tabFocusMode: false,
  occurrencesHighlight: 'off',
  selectionHighlight: false,
  hover: { enabled: false },
  bracketPairColorization: { enabled: true },
}))

function buildDecorations(): monaco.editor.IModelDeltaDecoration[] {
  if (props.vuln === null) return []
  const startLine = props.vuln.lineStart
  const endLine = props.vuln.lineEnd
  const range = new monaco.Range(startLine, 1, endLine, 1)
  return [
    {
      range,
      options: {
        isWholeLine: true,
        className: 'cs-vuln-line-decoration',
        marginClassName: 'cs-vuln-line-margin',
        linesDecorationsClassName: 'cs-vuln-gutter-decoration',
        hoverMessage: { value: `**Vulnerable lines** (${startLine}–${endLine})\n\n${props.vuln.title}` },
        zIndex: 1,
      },
    },
  ]
}

function applyDecorations(): void {
  if (editorRef.value === null) return
  const model = editorRef.value.getModel()
  if (model === null) return
  if (currentLang !== monacoLang.value) {
    monaco.editor.setModelLanguage(model, monacoLang.value)
    currentLang = monacoLang.value
  }
  markerDecorations = editorRef.value.deltaDecorations(
    markerDecorations,
    buildDecorations(),
  )
  // Scroll to the first affected line and reveal it
  if (props.vuln !== null) {
    editorRef.value.revealLineInCenterIfOutsideViewport(props.vuln.lineStart)
  }
}

function handleMount(editor: monaco.editor.IStandaloneCodeEditor): void {
  editorRef.value = editor
  applyDecorations()
}

onMounted(() => {
  // Inject minimal CSS so the decoration class is visible.
  // Monaco injects its own styles, but our custom classes need a sheet.
  if (typeof document !== 'undefined') {
    const id = 'cs-monaco-deco-styles'
    if (!document.getElementById(id)) {
      const style = document.createElement('style')
      style.id = id
      style.textContent = `
        .cs-vuln-line-decoration {
          background: rgba(207, 19, 34, 0.08) !important;
          border-left: 3px solid #CF1322 !important;
        }
        .cs-vuln-line-margin {
          background: rgba(207, 19, 34, 0.18) !important;
          width: 4px !important;
          margin-left: 3px;
        }
        .cs-vuln-gutter-decoration {
          background: #CF1322 !important;
          width: 3px !important;
          margin-left: 3px;
        }
        .monaco-editor .line-numbers {
          color: var(--cs-text-tertiary, #8C8C8C);
        }
      `
      document.head.appendChild(style)
    }
  }
})

onBeforeUnmount(() => {
  editorRef.value = null
})

watch(
  () => [props.code, props.vuln?.id, props.vuln?.lineStart, props.vuln?.lineEnd, monacoLang.value],
  () => {
    // Monaco updates the model on value change; reapply decorations after a tick.
    setTimeout(() => applyDecorations(), 0)
  },
)
</script>

<template>
  <div class="cs-code-viewer">
    <MonacoEditor
      :value="code"
      :language="monacoLang"
      :theme="'vs-dark'"
      :height="height"
      :options="editorOptions"
      @mount="handleMount"
    />
  </div>
</template>

<style scoped>
.cs-code-viewer {
  width: 100%;
  height: 100%;
  border-radius: var(--cs-radius-md);
  overflow: hidden;
  border: 1px solid var(--cs-border);
}
.cs-code-viewer :deep(.monaco-editor) {
  border-radius: var(--cs-radius-md);
}
</style>
