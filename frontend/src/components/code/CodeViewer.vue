<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, ref, watch } from 'vue'
import { EditorView, basicSetup } from 'codemirror'
import { EditorState, StateEffect, StateField } from '@codemirror/state'
import { Decoration, type DecorationSet } from '@codemirror/view'
import { syntaxHighlighting, defaultHighlightStyle } from '@codemirror/language'
import { oneDark } from '@codemirror/theme-one-dark'
import { getLanguageExt } from '@/cm-setup'
import type { Vuln, Language } from '@/types/vuln'

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

const containerRef = ref<HTMLDivElement | null>(null)
let view: EditorView | null = null

// --- Decoration infrastructure ---

const addDeco = StateEffect.define<DecorationSet>()

const decoField = StateField.define<DecorationSet>({
  create() {
    return Decoration.none
  },
  update(decos, tr) {
    for (const e of tr.effects) {
      if (e.is(addDeco)) return e.value
    }
    return decos
  },
  provide: (f) => EditorView.decorations.from(f),
})

function buildDecoSet(vuln: Vuln): DecorationSet {
  const startLine = vuln.lineStart
  const endLine = vuln.lineEnd
  const decos: import('@codemirror/state').Range<Decoration>[] = []

  for (let line = startLine; line <= endLine; line++) {
    decos.push(
      Decoration.line({ class: 'cs-cm-vuln-line' }).range(line - 1),
    )
  }

  return Decoration.set(decos)
}

function applyDecos(): void {
  if (view === null) return
  if (props.vuln === null) {
    view.dispatch({ effects: addDeco.of(Decoration.none) })
    return
  }
  const decos = buildDecoSet(props.vuln)
  view.dispatch({ effects: addDeco.of(decos) })

  // Scroll to first affected line
  view.dispatch({
    effects: EditorView.scrollIntoView(props.vuln.lineStart - 1, { y: 'center' }),
  })
}

// --- Language extension (re-active on change) ---

const langExt = computed(() => getLanguageExt(props.language))

function buildExtensions(): import('@codemirror/state').Extension[] {
  return [
    basicSetup,
    oneDark,
    syntaxHighlighting(defaultHighlightStyle, { fallback: true }),
    langExt.value,
    EditorView.editable.of(false),
    EditorView.theme({
      '&': {
        fontSize: '13px',
        fontFamily: 'JetBrains Mono, SF Mono, Menlo, Consolas, monospace',
        backgroundColor: '#1e1e1e',
        height: '100%',
        outline: 'none',
      },
      '.cm-scroller': {
        fontFamily: 'JetBrains Mono, SF Mono, Menlo, Consolas, monospace',
      },
      '.cm-gutters': {
        backgroundColor: '#1e1e1e',
        borderRight: '1px solid #2d2d2d',
        color: '#858585',
        fontSize: '12px',
      },
      '.cm-activeLineGutter': {
        backgroundColor: 'transparent',
      },
      '.cm-foldGutter': {
        display: 'none',
      },
      '.cm-activeLine': {
        backgroundColor: 'transparent',
      },
      '.cm-foldPlaceholder': {
        display: 'none',
      },
      '.cm-content': {
        caretColor: 'transparent',
        padding: '12px 0',
      },
      '.cm-line': {
        padding: '0 12px',
      },
      '.cm-selectionBackground, .cm-focused .cm-selectionBackground': {
        backgroundColor: 'transparent !important',
      },
      '.cm-cursor': {
        display: 'none',
      },
      '&.cm-focused': {
        outline: 'none',
      },
    }),
    decoField,
  ]
}

// --- Lifecycle ---

onMounted(() => {
  if (containerRef.value === null) return

  const state = EditorState.create({
    doc: props.code,
    extensions: buildExtensions(),
  })

  view = new EditorView({
    state,
    parent: containerRef.value,
  })

  // Apply decorations after mount
  requestAnimationFrame(() => applyDecos())

  // Inject CSS for decoration classes
  if (typeof document !== 'undefined') {
    const id = 'cs-cm-deco-styles'
    if (!document.getElementById(id)) {
      const style = document.createElement('style')
      style.id = id
      style.textContent = `
        .cs-cm-vuln-line {
          background: rgba(207, 19, 34, 0.08) !important;
          border-left: 3px solid #CF1322 !important;
          border-radius: 0 !important;
        }
        .cs-cm-gutter-marker {
          display: inline-block;
          width: 3px;
          height: 100%;
          background: #CF1322;
          margin-left: 2px;
          vertical-align: top;
        }
      `
      document.head.appendChild(style)
    }
  }
})

onBeforeUnmount(() => {
  if (view !== null) {
    view.destroy()
    view = null
  }
})

// --- Reactivity ---

watch(
  () => props.code,
  (newCode) => {
    if (view === null) return
    const cur = view.state.doc.toString()
    if (cur !== newCode) {
      view.dispatch({
        changes: { from: 0, to: cur.length, insert: newCode },
      })
    }
    setTimeout(() => applyDecos(), 0)
  },
)

watch(
  () => [props.vuln?.id, props.vuln?.lineStart, props.vuln?.lineEnd],
  () => setTimeout(() => applyDecos(), 0),
)
</script>

<template>
  <div ref="containerRef" class="cs-cm-viewer" :style="{ height }" />
</template>

<style scoped>
.cs-cm-viewer {
  width: 100%;
  border-radius: var(--cs-radius-md);
  overflow: hidden;
  border: 1px solid var(--cs-border);
  background: #1e1e1e;
}
.cs-cm-viewer :deep(.cm-editor) {
  border-radius: var(--cs-radius-md);
  height: 100%;
}
</style>
